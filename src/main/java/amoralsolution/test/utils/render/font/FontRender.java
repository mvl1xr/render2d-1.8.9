package amoralsolution.test.utils.render.font;

public final class FontRender {

    private FontRender() {}

    public static float measureWidth(FontType type, String text, float size) {
        return TtfFont.measureWidth(type, text, size);
    }

    public static float measureWidth(FontType type, String text, float size, float letterSpacingEm) {
        return TtfFont.measureWidth(type, text, size, letterSpacingEm);
    }

    public static float textHeight(FontType type, float size) {
        return TtfFont.textHeight(type, size);
    }

    public static float lineHeight(FontType type, float size) {
        return TtfFont.lineHeight(type, size);
    }

    public static void draw(FontType type, String text, float x, float y, float size, int color) {
        TtfFont.draw(type, text, x, y, size, color, false);
    }

    public static void draw(FontType type, String text, float x, float y, float size, int color, boolean shadow) {
        TtfFont.draw(type, text, x, y, size, color, shadow);
    }

    public static void drawCentered(FontType type, String text, float cx, float y, float size, int color, boolean shadow) {
        TtfFont.drawCentered(type, text, cx, y, size, color, shadow);
    }

    public static void drawCentered(FontType type, String text, float cx, float y, float size, int color) {
        TtfFont.drawCentered(type, text, cx, y, size, color, false);
    }

    public static void drawRight(FontType type, String text, float rx, float y, float size, int color, boolean shadow) {
        TtfFont.drawRight(type, text, rx, y, size, color, shadow);
    }

    public static void drawRight(FontType type, String text, float rx, float y, float size, int color) {
        TtfFont.drawRight(type, text, rx, y, size, color, false);
    }

    public static void drawExactCentered(FontType type, String text, float cx, float cy, float size, int color) {
        TtfFont.drawExactCentered(type, text, cx, cy, size, color);
    }
}
