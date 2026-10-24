package amoralsolution.test.utils.render.font;

import amoralsolution.test.utils.render.core.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TtfFont {

    private static class WidthKey {
        final FontType type;
        final float size;
        final float letterSpacingEm;
        final String text;

        WidthKey(FontType type, float size, float letterSpacingEm, String text) {
            this.type = type;
            this.size = size;
            this.letterSpacingEm = letterSpacingEm;
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WidthKey widthKey = (WidthKey) o;
            if (Float.compare(widthKey.size, size) != 0) return false;
            if (Float.compare(widthKey.letterSpacingEm, letterSpacingEm) != 0) return false;
            if (type != widthKey.type) return false;
            return text.equals(widthKey.text);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + (size != +0.0f ? Float.floatToIntBits(size) : 0);
            result = 31 * result + (letterSpacingEm != +0.0f ? Float.floatToIntBits(letterSpacingEm) : 0);
            result = 31 * result + text.hashCode();
            return result;
        }
    }

    private static class TextCacheKey {
        final FontType type;
        final float scale;
        final float size;
        final float letterSpacingEm;
        final String text;

        TextCacheKey(FontType type, float scale, float size, float letterSpacingEm, String text) {
            this.type = type;
            this.scale = scale;
            this.size = size;
            this.letterSpacingEm = letterSpacingEm;
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextCacheKey that = (TextCacheKey) o;
            if (Float.compare(that.scale, scale) != 0) return false;
            if (Float.compare(that.size, size) != 0) return false;
            if (Float.compare(that.letterSpacingEm, letterSpacingEm) != 0) return false;
            if (type != that.type) return false;
            return text.equals(that.text);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + (scale != +0.0f ? Float.floatToIntBits(scale) : 0);
            result = 31 * result + (size != +0.0f ? Float.floatToIntBits(size) : 0);
            result = 31 * result + (letterSpacingEm != +0.0f ? Float.floatToIntBits(letterSpacingEm) : 0);
            result = 31 * result + text.hashCode();
            return result;
        }
    }

    private static class MetricsKey {
        final FontType type;
        final float size;

        MetricsKey(FontType type, float size) {
            this.type = type;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MetricsKey that = (MetricsKey) o;
            if (Float.compare(that.size, size) != 0) return false;
            return type == that.type;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + (size != +0.0f ? Float.floatToIntBits(size) : 0);
            return result;
        }
    }

    private static class FontMetricsInfo {
        final float ascender;
        final float descender;
        final float lineHeight;
        final float textHeight;

        FontMetricsInfo(float ascender, float descender, float lineHeight, float textHeight) {
            this.ascender = ascender;
            this.descender = descender;
            this.lineHeight = lineHeight;
            this.textHeight = textHeight;
        }
    }

    private static final Map<String, Font> BASE = new ConcurrentHashMap<String, Font>();
    private static final Map<WidthKey, Float> WIDTH_CACHE = new ConcurrentHashMap<WidthKey, Float>();
    private static final Map<MetricsKey, FontMetricsInfo> METRICS_CACHE = new ConcurrentHashMap<MetricsKey, FontMetricsInfo>();

    private static final int MAX_CACHE_ENTRIES = 2048;

    private static final float GLYPH_GRID_PX = 64.0F;
    private static final float MAX_RASTER_SCALE = 12.0F;
    private static final long MAX_RASTER_PIXELS = 2_000_000L;

    private static final Map<TextCacheKey, TtfEntry> CACHE = new LinkedHashMap<TextCacheKey, TtfEntry>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TextCacheKey, TtfEntry> eldest) {
            if (size() > MAX_CACHE_ENTRIES) {
                TtfEntry e = eldest.getValue();
                if (e != null) {
                    e.close();
                }
                return true;
            }
            return false;
        }
    };

    private TtfFont() {}

    private static float guiScale() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return 2.0F;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        return Math.max(1.0F, (float) sr.getScaleFactor());
    }

    private static Font base(FontType type) {
        String key = type.ttfFile();
        Font cached = BASE.get(key);
        if (cached != null) return cached;

        InputStream in = null;
        String[] paths = new String[]{
                "/assets/amoral/fonts/" + key,
                "/assets/amoral/fonts/" + key.toLowerCase(Locale.ROOT),
                "assets/amoral/fonts/" + key,
                "assets/amoral/fonts/" + key.toLowerCase(Locale.ROOT),
                "/assets/amoral/fonts/ttf/" + key,
                "assets/amoral/fonts/ttf/" + key
        };

        for (String p : paths) {
            in = TtfFont.class.getResourceAsStream(p);
            if (in == null) {
                in = TtfFont.class.getClassLoader().getResourceAsStream(p.startsWith("/") ? p.substring(1) : p);
            }
            if (in != null) break;
        }

        if (in == null) {
            java.io.File[] fileFallbacks = new java.io.File[] {
                new java.io.File("run/assets/amoral/fonts/" + key),
                new java.io.File("assets/amoral/fonts/" + key),
                new java.io.File("../run/assets/amoral/fonts/" + key),
                new java.io.File("src/main/resources/assets/amoral/fonts/" + key),
                new java.io.File("run/assets/amoral/fonts/ttf/" + key),
                new java.io.File("src/main/resources/assets/amoral/fonts/ttf/" + key)
            };
            for (java.io.File f : fileFallbacks) {
                if (f.exists()) {
                    try {
                        in = new java.io.FileInputStream(f);
                        break;
                    } catch (Exception ignored) {}
                }
            }
        }

        if (in != null) {
            try {
                Font f = Font.createFont(Font.TRUETYPE_FONT, in);
                in.close();
                BASE.put(key, f);
                return f;
            } catch (Exception e) {
                System.err.println("[TtfFont] Failed to create font for " + key + ": " + e.getMessage());
            }
        }

        System.err.println("[TtfFont] Font resource not found for: " + key + ", falling back to SansSerif");
        Font fallback = new Font("SansSerif", Font.PLAIN, 18);
        BASE.put(key, fallback);
        return fallback;
    }

    private static Font derive(FontType type, float size, float letterSpacingEm) {
        Font font = base(type).deriveFont(Font.PLAIN, size);
        if (Math.abs(letterSpacingEm) > 0.0001F) {
            Map<TextAttribute, Object> attrs = new HashMap<TextAttribute, Object>(font.getAttributes());
            attrs.put(TextAttribute.TRACKING, letterSpacingEm);
            return font.deriveFont(attrs);
        }
        return font;
    }

    private static Font deriveForText(FontType type, float size, float letterSpacingEm, String text) {
        Font primary = derive(type, size, letterSpacingEm);
        if (text == null || text.isEmpty()) {
            return primary;
        }

        boolean canDisplayAll = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!primary.canDisplay(c)) {
                canDisplayAll = false;
                break;
            }
        }

        if (canDisplayAll) {
            return primary;
        }

        FontType fallbackType = FontType.commissioner(type.weight());
        return derive(fallbackType, size, letterSpacingEm);
    }

    private static FontMetricsInfo getMetrics(FontType type, float size) {
        MetricsKey key = new MetricsKey(type, size);
        FontMetricsInfo info = METRICS_CACHE.get(key);
        if (info != null) return info;

        Font font = base(type).deriveFont(Font.PLAIN, size);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        LineMetrics lm = font.getLineMetrics("Ag", frc);
        float ascender = lm.getAscent();
        float descender = lm.getDescent();
        float lineHeight = lm.getHeight();
        float textHeight = ascender + descender;

        info = new FontMetricsInfo(ascender, descender, lineHeight, textHeight);
        METRICS_CACHE.put(key, info);
        return info;
    }

    public static float measureWidth(FontType type, String text, float size) {
        return measureWidth(type, text, size, 0.0F);
    }

    public static float measureWidth(FontType type, String text, float size, float letterSpacingEm) {
        if (text == null || text.isEmpty() || size <= 0.0F) {
            return 0.0F;
        }
        WidthKey key = new WidthKey(type, size, letterSpacingEm, text);
        Float cached = WIDTH_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Font font = deriveForText(type, size, letterSpacingEm, text);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Rectangle2D bounds = font.getStringBounds(text, frc);
        float width = (float) bounds.getWidth();
        if (WIDTH_CACHE.size() > 8192) {
            WIDTH_CACHE.clear();
        }
        WIDTH_CACHE.put(key, width);
        return width;
    }

    public static float textHeight(FontType type, float size) {
        return getMetrics(type, size).textHeight;
    }

    public static float lineHeight(FontType type, float size) {
        return getMetrics(type, size).lineHeight;
    }

    private static synchronized TtfEntry ensure(FontType type, float size, String text, float letterSpacingEm) {
        float scale = guiScale();
        TextCacheKey key = new TextCacheKey(type, scale, size, letterSpacingEm, text);
        TtfEntry e = CACHE.get(key);
        if (e != null) {
            return e;
        }
        e = rasterize(type, size, text, scale, letterSpacingEm);
        CACHE.put(key, e);
        return e;
    }

    public static void draw(FontType type, String text, float x, float y, float size, int color) {
        draw(type, text, x, y, size, color, false);
    }

    public static void draw(FontType type, String text, float x, float y, float size, int color, boolean shadow) {
        if (text == null || text.isEmpty() || size <= 0.0F || ColorUtils.getAlpha(color) == 0) {
            return;
        }
        if (shadow) {
            int shadowColor = ColorUtils.rgba(0, 0, 0, (int) (ColorUtils.getAlpha(color) * 0.6f));
            drawInternal(type, text, x + 1.0F, y + 1.0F, size, shadowColor);
        }
        drawInternal(type, text, x, y, size, color);
    }

    public static void drawCentered(FontType type, String text, float cx, float y, float size, int color) {
        drawCentered(type, text, cx, y, size, color, false);
    }

    public static void drawCentered(FontType type, String text, float cx, float y, float size, int color, boolean shadow) {
        float w = measureWidth(type, text, size);
        draw(type, text, cx - w / 2.0F, y, size, color, shadow);
    }

    public static void drawRight(FontType type, String text, float rx, float y, float size, int color) {
        drawRight(type, text, rx, y, size, color, false);
    }

    public static void drawRight(FontType type, String text, float rx, float y, float size, int color, boolean shadow) {
        float w = measureWidth(type, text, size);
        draw(type, text, rx - w, y, size, color, shadow);
    }

    public static void drawExactCentered(FontType type, String text, float cx, float cy, float size, int color) {
        if (text == null || text.isEmpty() || size <= 0.0F || ColorUtils.getAlpha(color) == 0) {
            return;
        }
        TtfEntry e = ensure(type, size, text, 0.0F);
        float drawX = cx - e.visualCenterGuiX;
        float drawY = cy - e.visualCenterGuiY;
        renderTexture(e.textureId, drawX, drawY, e.dw, e.dh, color);
    }

    private static void drawInternal(FontType type, String text, float x, float y, float size, int color) {
        TtfEntry e = ensure(type, size, text, 0.0F);
        float verticalShift = 0.5F;
        renderTexture(e.textureId, x - e.padGui, y + verticalShift - e.padGui, e.dw, e.dh, color);
    }

    private static void renderTexture(int textureId, float x, float y, float w, float h, int color) {
        boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        if (alphaTest) {
            GlStateManager.disableAlpha();
        }
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.bindTexture(textureId);

        float r = ColorUtils.getRedF(color);
        float g = ColorUtils.getGreenF(color);
        float b = ColorUtils.getBlueF(color);
        float a = ColorUtils.getAlphaF(color);
        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + h, 0.0).tex(0.0, 1.0).endVertex();
        worldrenderer.pos(x + w, y + h, 0.0).tex(1.0, 1.0).endVertex();
        worldrenderer.pos(x + w, y, 0.0).tex(1.0, 0.0).endVertex();
        worldrenderer.pos(x, y, 0.0).tex(0.0, 0.0).endVertex();
        tessellator.draw();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (alphaTest) {
            GlStateManager.enableAlpha();
        }
    }

    private static TtfEntry rasterize(FontType type, float size, String text, float scale, float letterSpacingEm) {
        float supersample = 1.5F;
        float fontScale = Math.max(scale * supersample, 2.0F);
        fontScale = Math.min(fontScale, MAX_RASTER_SCALE);

        float estWidth = measureWidth(type, text, size, letterSpacingEm);
        if (estWidth > 0.0F && size > 0.0F) {
            float estPixels = estWidth * fontScale * Math.max(fontScale * size * 1.3F, GLYPH_GRID_PX);
            if (estPixels > (float) MAX_RASTER_PIXELS) {
                float shrink = (float) Math.sqrt((float) MAX_RASTER_PIXELS / estPixels);
                fontScale = Math.max(1.0F, fontScale * shrink);
            }
        }

        float renderSize = Math.max(1.0F, (float) Math.round(size * fontScale));
        float actualFontScale = renderSize / size;

        Font base = deriveForText(type, renderSize, letterSpacingEm, text);

        int pad = 4;
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = base.createGlyphVector(frc, text);
        Rectangle2D bounds = gv.getPixelBounds(frc, 0, 0);

        int minX = (int) Math.floor(bounds.getX());
        int minY = (int) Math.floor(bounds.getY());
        int maxX = (int) Math.ceil(bounds.getX() + bounds.getWidth());
        int maxY = (int) Math.ceil(bounds.getY() + bounds.getHeight());

        GlyphVector refGv = base.createGlyphVector(frc, "H0");
        Rectangle2D refBounds = refGv.getPixelBounds(frc, 0, 0);
        int standardTop = (int) Math.floor(refBounds.getY());
        minY = Math.min(minY, standardTop);

        int textWidth = Math.max(1, maxX - minX);
        int textHeight = Math.max(1, maxY - minY);

        int rw = textWidth + pad * 2;
        int rh = textHeight + pad * 2;

        BufferedImage img = new BufferedImage(rw, rh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.drawGlyphVector(gv, (float) (pad - minX), (float) (pad - minY));
        g.dispose();

        int texId = uploadTexture(img, rw, rh);
        float dw = (float) rw / actualFontScale;
        float dh = (float) rh / actualFontScale;
        float padGui = (float) pad / actualFontScale;
        float visualCenterGuiX = (float) (pad + textWidth / 2.0f) / actualFontScale;
        float visualCenterGuiY = (float) (pad + textHeight / 2.0f) / actualFontScale;
        return new TtfEntry(texId, dw, dh, padGui, visualCenterGuiX, visualCenterGuiY);
    }

    private static int uploadTexture(BufferedImage img, int rw, int rh) {
        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        ByteBuffer buffer = BufferUtils.createByteBuffer(rw * rh * 4);
        int[] pixels = new int[rw * rh];
        img.getRGB(0, 0, rw, rh, pixels, 0, rw);

        for (int y = 0; y < rh; y++) {
            for (int x = 0; x < rw; x++) {
                int pixel = pixels[y * rw + x];
                int a = (pixel >> 24) & 0xFF;
                buffer.put((byte) 255);
                buffer.put((byte) 255);
                buffer.put((byte) 255);
                buffer.put((byte) a);
            }
        }
        buffer.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, rw, rh, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        return texId;
    }

    private static final class TtfEntry {
        final int textureId;
        final float dw;
        final float dh;
        final float padGui;
        final float visualCenterGuiX;
        final float visualCenterGuiY;

        TtfEntry(int textureId, float dw, float dh, float padGui, float visualCenterGuiX, float visualCenterGuiY) {
            this.textureId = textureId;
            this.dw = dw;
            this.dh = dh;
            this.padGui = padGui;
            this.visualCenterGuiX = visualCenterGuiX;
            this.visualCenterGuiY = visualCenterGuiY;
        }

        public void close() {
            try {
                if (textureId != 0) {
                    GL11.glDeleteTextures(textureId);
                }
            } catch (Throwable ignored) {}
        }
    }
}
