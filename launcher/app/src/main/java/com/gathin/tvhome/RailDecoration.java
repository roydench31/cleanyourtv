package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What the rail layout draws under its tiles (RailStyle): the horizon (a hairline or a shoreline wave), each tile
 * mirrored under it, a glow in the focused app's colour at its foot, and with focus_style halo a rim, shadow and
 * glow around the focused tile in that colour. All of it is drawn under the tiles, so a tile always covers its own
 * patch. Nothing is blurred: the glows are thin fading rings, as GlassView draws them, and the reflections are
 * half-size flipped copies of the banners, made once per banner.
 */
final class RailDecoration extends RecyclerView.ItemDecoration {
    /** The focused tile's colour; 0 while focus is off the row. */
    interface Accent {
        int colorAt(int position);
    }

    private final RailStyle style;
    private final Accent accent;
    /** Screen pixels per mockup pixel (the mockups are 1920x1080): the sizes here follow the mockups, not dp. */
    private final float unit;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mirror = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Path wave = new Path();
    private final RectF tile = new RectF();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    /** Banner to its flipped, faded, half-size copy; weak, so a banner the adapter drops takes its copy with it. */
    private final Map<Drawable, Bitmap> reflections = new WeakHashMap<>();
    private int lineWidth = -1;
    private int glowColor;

    RailDecoration(RailStyle style, Accent accent, float unit) {
        this.style = style;
        this.accent = accent;
        this.unit = unit;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, 1.5f * unit));
        ring.setStyle(Paint.Style.STROKE);
    }

    /** Lets go of every reflection (Home is in the background and memory is short). */
    void clear() {
        for (Bitmap b : reflections.values()) b.recycle();
        reflections.clear();
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int n = parent.getChildCount();
        if (n == 0) return;
        // The horizon is where every tile stands at rest: the foot of a card that is not raised.
        View first = parent.getChildAt(0).findViewById(R.id.card);
        if (first == null) return;
        float horizon = parent.getChildAt(0).getTop() + first.getBottom();

        View focusedCard = null;
        int focusedPos = RecyclerView.NO_POSITION;
        for (int i = 0; i < n; i++) {
            View item = parent.getChildAt(i);
            View card = item.findViewById(R.id.card);
            if (card == null || card.getVisibility() != View.VISIBLE) continue;
            if (card.isFocused()) {
                focusedCard = card;
                focusedPos = parent.getChildAdapterPosition(item);
            }
        }

        int color = focusedCard != null && focusedPos != RecyclerView.NO_POSITION ? accent.colorAt(focusedPos) : 0;
        if (style.underGlow && focusedCard != null && color != 0) drawUnderGlow(c, focusedCard, horizon, color);
        if (style.reflection) {
            for (int i = 0; i < n; i++) {
                View card = parent.getChildAt(i).findViewById(R.id.card);
                if (card != null && card.getVisibility() == View.VISIBLE) drawReflection(c, card, horizon);
            }
        }
        if (style.horizon) drawHorizon(c, parent.getWidth(), horizon);
        if (style.halo() && focusedCard != null && color != 0) drawHalo(c, focusedCard, color);
    }

    /** Where the card is drawn now, in the row's coordinates: its layout box moved and zoomed as it animates. */
    private void visualRect(View card, RectF out) {
        View item = (View) card.getParent();
        float sx = card.getScaleX(), sy = card.getScaleY();
        float left = item.getLeft() + item.getTranslationX() + card.getLeft() + card.getTranslationX()
                + card.getPivotX() * (1 - sx);
        float top = item.getTop() + item.getTranslationY() + card.getTop() + card.getTranslationY()
                + card.getPivotY() * (1 - sy);
        out.set(left, top, left + card.getWidth() * sx, top + card.getHeight() * sy);
    }

    private void drawHorizon(Canvas c, int width, float y) {
        if (lineWidth != width) {
            lineWidth = width;
            int base = RailStyle.or(style.hairline, 0xFFE9EDF7);
            int on = (0x73 << 24) | (base & 0xFFFFFF), off = base & 0xFFFFFF;
            line.setShader(new LinearGradient(0, 0, width, 0, new int[]{off, on, on, off},
                    new float[]{0f, 0.1f, 0.9f, 1f}, Shader.TileMode.CLAMP));
            wave.reset();
            for (int x = 0; x <= width; x += Math.max(4, Math.round(16 * unit))) {
                float dy = RailStyle.waveY(x, width, unit);
                if (x == 0) wave.moveTo(x, dy);
                else wave.lineTo(x, dy);
            }
        }
        if (style.wave()) {
            c.save();
            c.translate(0, y + 6 * unit);
            c.drawPath(wave, line);
            c.restore();
        } else {
            c.drawLine(0, y + 2 * unit, width, y + 2 * unit, line);
        }
    }

    private void drawReflection(Canvas c, View card, float horizon) {
        ImageView banner = card.findViewById(R.id.banner);
        Drawable d = banner != null && banner.getVisibility() == View.VISIBLE ? banner.getDrawable() : null;
        if (d == null || card.getWidth() <= 0) return;
        Bitmap b = reflections.get(d);
        if (b == null) {
            b = reflection(d, card.getWidth() / 2, card.getHeight() / 2);
            reflections.put(d, b);
        }
        visualRect(card, tile);
        // Mirrored in the horizon: a raised tile's reflection drops by as much as the tile rises.
        float gap = horizon - tile.bottom;
        dst.set(tile.left, horizon + gap + 2 * unit, tile.right, horizon + gap + 2 * unit + tile.height());
        src.set(0, 0, b.getWidth(), b.getHeight());
        mirror.setAlpha(Math.round(255 * (card.isFocused() ? 0.22f : 0.12f) * Math.min(1f, card.getAlpha() + 0.2f)));
        c.drawBitmap(b, src, dst, mirror);
    }

    /** The banner flipped upside down, strongest at its top edge (the tile's foot) and gone by half its height. */
    private static Bitmap reflection(Drawable d, int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        // Draw a private copy: changing the bounds of the drawable a tile shows would blank the tile.
        Drawable copy = d.getConstantState() != null ? d.getConstantState().newDrawable().mutate() : d;
        Rect saved = new Rect(copy.getBounds());
        c.save();
        c.scale(1f, -1f, w / 2f, h / 2f);
        copy.setBounds(0, 0, w, h);
        copy.draw(c);
        copy.setBounds(saved);
        c.restore();
        Paint fade = new Paint();
        fade.setShader(new LinearGradient(0, 0, 0, h * 0.5f, 0xB3000000, 0x00000000, Shader.TileMode.CLAMP));
        fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        c.drawRect(0, 0, w, h, fade);
        return out;
    }

    private void drawUnderGlow(Canvas c, View card, float horizon, int color) {
        visualRect(card, tile);
        float rx = tile.width() * 0.9f, ry = tile.height() * 0.22f;
        if (color != glowColor || glow.getShader() == null) {
            glowColor = color;
            int rgb = color & 0xFFFFFF;
            glow.setShader(new RadialGradient(0, 0, 100f, new int[]{(0x66 << 24) | rgb, (0x26 << 24) | rgb, rgb},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        }
        c.save();
        c.translate(tile.centerX(), horizon);
        c.scale(rx / 100f, ry / 100f);
        c.drawCircle(0, 0, 100f, glow);
        c.restore();
    }

    /** A 3px rim in the app's colour, a shadow under the raised tile and a wide faint glow around it. */
    private void drawHalo(Canvas c, View card, int color) {
        visualRect(card, tile);
        int rgb = color & 0xFFFFFF;
        // The tile's own corners (Holder's outline), zoomed with it.
        float radius = AppAdapter.cornerRadius(card.getWidth(), card.getResources().getDisplayMetrics().density) * card.getScaleX();

        // The wide glow: thin rings fading out over 45px, as GlassView's glow.
        int rings = 14;
        float spread = 45 * unit, step = spread / rings;
        ring.setStrokeWidth(step + 0.5f);
        for (int i = 0; i < rings; i++) {
            float o = step * (i + 0.5f);
            float t = 1f - (float) i / rings;
            ring.setColor(Math.round(0x40 * t * t) << 24 | rgb);
            c.drawRoundRect(tile.left - o, tile.top - o, tile.right + o, tile.bottom + o, radius + o, radius + o, ring);
        }
        // The shadow: the same rings, stronger and pushed 18px down, under the tile's foot.
        float drop = 18 * unit;
        rings = 10;
        step = 24 * unit / rings;
        ring.setStrokeWidth(step + 0.5f);
        for (int i = 0; i < rings; i++) {
            float o = step * (i + 0.5f);
            float t = 1f - (float) i / rings;
            ring.setColor(Math.round(0x70 * t * t) << 24 | rgb);
            c.drawRoundRect(tile.left + 8 * unit - o, tile.top + drop + 8 * unit - o,
                    tile.right - 8 * unit + o, tile.bottom + drop + o, radius + o, radius + o, ring);
        }
        // The rim, just outside the tile's edge.
        float w = 3 * unit;
        ring.setStrokeWidth(w);
        ring.setColor(0xFF000000 | rgb);
        c.drawRoundRect(tile.left - w / 2, tile.top - w / 2, tile.right + w / 2, tile.bottom + w / 2,
                radius + w / 2, radius + w / 2, ring);
    }
}
