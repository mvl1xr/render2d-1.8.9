package amoralsolution.test.utils.render.core;

import java.awt.Color;

public final class ColorUtils {

    private ColorUtils() {}

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0x00000000;

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(val, max));
    }

    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(val, max));
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    public static int rgba(float r, float g, float b, float a) {
        return rgba(
                (int) (clamp(r, 0.0f, 1.0f) * 255.0f),
                (int) (clamp(g, 0.0f, 1.0f) * 255.0f),
                (int) (clamp(b, 0.0f, 1.0f) * 255.0f),
                (int) (clamp(a, 0.0f, 1.0f) * 255.0f)
        );
    }

    public static int getAlpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    public static int getRed(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int getGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int getBlue(int argb) {
        return argb & 0xFF;
    }

    public static float getAlphaF(int argb) {
        return getAlpha(argb) / 255.0f;
    }

    public static float getRedF(int argb) {
        return getRed(argb) / 255.0f;
    }

    public static float getGreenF(int argb) {
        return getGreen(argb) / 255.0f;
    }

    public static float getBlueF(int argb) {
        return getBlue(argb) / 255.0f;
    }

    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    public static int withAlpha(int argb, float alpha) {
        return withAlpha(argb, (int) (clamp(alpha, 0.0f, 1.0f) * 255.0f));
    }

    public static int multiplyAlpha(int argb, float alpha) {
        int a = (int) (getAlpha(argb) * clamp(alpha, 0.0f, 1.0f));
        return withAlpha(argb, a);
    }

    public static int withSaturation(int argb, float factor) {
        if (factor >= 0.999f && factor <= 1.001f) {
            return argb;
        }
        float[] hsb = Color.RGBtoHSB(getRed(argb), getGreen(argb), getBlue(argb), null);
        float sat = clamp(hsb[1] * factor, 0.0f, 1.0f);
        int rgb = Color.HSBtoRGB(hsb[0], sat, hsb[2]) & 0x00FFFFFF;
        return ((getAlpha(argb) & 0xFF) << 24) | rgb;
    }

    public static int lerp(int c1, int c2, float ratio) {
        float f = clamp(ratio, 0.0f, 1.0f);
        float inv = 1.0f - f;

        int a = (int) (getAlpha(c1) * inv + getAlpha(c2) * f);
        int r = (int) (getRed(c1) * inv + getRed(c2) * f);
        int g = (int) (getGreen(c1) * inv + getGreen(c2) * f);
        int b = (int) (getBlue(c1) * inv + getBlue(c2) * f);

        return rgba(r, g, b, a);
    }

    public static float[] toFloats(int argb) {
        return new float[]{getRedF(argb), getGreenF(argb), getBlueF(argb), getAlphaF(argb)};
    }
}
