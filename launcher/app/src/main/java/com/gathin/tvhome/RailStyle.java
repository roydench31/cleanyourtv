package com.gathin.tvhome;

/**
 * The Hard Light rail layout and its look, from a theme's config.json (docs/hardlight/GUIDE.md, Appendix B).
 * With "layout": "rail" Home is one horizontal row of tiles on a horizon line, under a big app name. Every value
 * defaults to today's launcher, and {@link #CLASSIC} is what a theme without these keys gets.
 */
final class RailStyle {
    static final String LAYOUT_CLASSIC = "classic", LAYOUT_RAIL = "rail";
    static final String FONT_OUTFIT = "outfit", FONT_GREAT_VIBES = "great_vibes";
    static final String HINTS_NONE = "none", HINTS_TEXT = "text";
    static final String FOCUS_GLASS = "glass", FOCUS_HALO = "halo";
    static final String HORIZON_LINE = "line", HORIZON_WAVE = "wave";

    static final RailStyle CLASSIC = new RailStyle(false, FONT_OUTFIT, 0, 0, 0, 0, false, HINTS_NONE, FOCUS_GLASS,
            100, false, HORIZON_LINE, false, false);

    /** layout: rail. Off keeps the dock and grid. */
    final boolean on;
    /** display_font: the hero name's (and, for a script face, the greeting's) typeface. */
    final String displayFont;
    /** ink_color, muted_color, highlight_color, hairline_color: opaque ARGB, or 0 when the theme leaves them out. */
    final int ink, muted, highlight, hairline;
    /** hero_eyebrow: "Last opened · …" above the name. */
    final boolean heroEyebrow;
    /** control_hints: none or text. */
    final String hints;
    /** focus_style: glass (the springing highlight) or halo (a rim, shadow and glow in the app's colour). */
    final String focusStyle;
    /** tile_rest_alpha: opacity of the tiles without focus, 30 to 100. */
    final int restAlpha;
    /** horizon_line and horizon_style. */
    final boolean horizon;
    final String horizonStyle;
    /** tile_reflection, under_glow. */
    final boolean reflection, underGlow;

    RailStyle(boolean on, String displayFont, int ink, int muted, int highlight, int hairline, boolean heroEyebrow,
              String hints, String focusStyle, int restAlpha, boolean horizon, String horizonStyle,
              boolean reflection, boolean underGlow) {
        this.on = on;
        this.displayFont = displayFont(displayFont);
        this.ink = ink;
        this.muted = muted;
        this.highlight = highlight;
        this.hairline = hairline;
        this.heroEyebrow = heroEyebrow;
        this.hints = hints(hints);
        this.focusStyle = focusStyle(focusStyle);
        this.restAlpha = Math.max(30, Math.min(100, restAlpha));
        this.horizon = horizon;
        this.horizonStyle = horizonStyle(horizonStyle);
        this.reflection = reflection;
        this.underGlow = underGlow;
    }

    boolean script() {
        return FONT_GREAT_VIBES.equals(displayFont);
    }

    boolean halo() {
        return FOCUS_HALO.equals(focusStyle);
    }

    boolean wave() {
        return HORIZON_WAVE.equals(horizonStyle);
    }

    /** The theme's colour, or the given fallback when it set none. */
    static int or(int color, int fallback) {
        return color != 0 ? color : fallback;
    }

    static boolean rail(String layout) {
        return layout != null && LAYOUT_RAIL.equalsIgnoreCase(layout.trim());
    }

    static String displayFont(String value) {
        return value != null && FONT_GREAT_VIBES.equalsIgnoreCase(value.trim()) ? FONT_GREAT_VIBES : FONT_OUTFIT;
    }

    static String hints(String value) {
        return value != null && HINTS_TEXT.equalsIgnoreCase(value.trim()) ? HINTS_TEXT : HINTS_NONE;
    }

    static String focusStyle(String value) {
        return value != null && FOCUS_HALO.equalsIgnoreCase(value.trim()) ? FOCUS_HALO : FOCUS_GLASS;
    }

    static String horizonStyle(String value) {
        return value != null && HORIZON_WAVE.equalsIgnoreCase(value.trim()) ? HORIZON_WAVE : HORIZON_LINE;
    }

    /** The rail's geometry, from the Ocean and Hard Light mockups at 1920x1080 and scaled to the screen. */
    static final float FOCUS_SCALE = 1.10f;

    /** Tile width: 288 px at 1920. */
    static int tileWidth(int screenWidth) {
        return Math.round(screenWidth * 0.15f);
    }

    /** Gap between tiles: 32 px at 1920. */
    static int gap(int screenWidth) {
        return Math.round(screenWidth * 32f / 1920f);
    }

    /** Left edge of the first tile and of the hero: 140 px at 1920. */
    static int margin(int screenWidth) {
        return Math.round(screenWidth * 140f / 1920f);
    }

    /** The horizon, where the tiles stand: y 868 of 1080. */
    static int horizonY(int screenHeight) {
        return Math.round(screenHeight * 868f / 1080f);
    }

    /** How far the focused tile rises: 30 px at 1080. */
    static float rise(int screenHeight) {
        return screenHeight * 30f / 1080f;
    }

    /** The focused tile parks at this slot (0-based) once the row scrolls, as in the mockups. */
    static final int FOCUS_SLOT = 3;

    /** y of the shoreline wave at x, around 0: two summed sines, as the Ocean mockup draws it (shorePath). */
    static float waveY(float x, float width, float scale) {
        if (width <= 0) return 0f;
        double t = x / width * Math.PI;
        return (float) ((4 * Math.sin(t * 9) + 1.5 * Math.sin(t * 23)) * scale);
    }
}
