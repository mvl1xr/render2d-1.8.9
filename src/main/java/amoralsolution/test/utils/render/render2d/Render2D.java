package amoralsolution.test.utils.render.render2d;

import amoralsolution.test.utils.render.core.ColorUtils;
import amoralsolution.test.utils.render.shader.Shader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public final class Render2D {

    private static Shader squircleShader;
    private static Shader blurSquircleShader;
    private static int backdropTexture = -1;
    private static int backdropWidth = -1;
    private static int backdropHeight = -1;

    private static final String VERTEX_SHADER =
            "#version 120\n" +
            "void main() {\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 120\n" +
            "#extension GL_OES_standard_derivatives : enable\n" +
            "\n" +
            "uniform vec2 u_size;\n" +
            "uniform vec4 u_radii;\n" +
            "uniform vec4 u_color1;\n" +
            "uniform vec4 u_color2;\n" +
            "uniform vec4 u_color3;\n" +
            "uniform vec4 u_color4;\n" +
            "uniform int  u_mode;\n" +
            "uniform float u_thickness;\n" +
            "uniform float u_softness;\n" +
            "uniform int   u_isSquircle;\n" +
            "\n" +
            "float selectRadius(vec2 p, vec4 r) {\n" +
            "    if (p.x >= 0.0) {\n" +
            "        return p.y < 0.0 ? r.y : r.z;\n" +
            "    }\n" +
            "    return p.y < 0.0 ? r.x : r.w;\n" +
            "}\n" +
            "\n" +
            "float squircleSdf(vec2 point, vec2 halfSize, vec4 radii) {\n" +
            "    float radius = selectRadius(point, radii);\n" +
            "    if (radius <= 0.0001) {\n" +
            "        vec2 q = abs(point) - halfSize;\n" +
            "        return max(q.x, q.y);\n" +
            "    }\n" +
            "    float minHalf = min(halfSize.x, halfSize.y);\n" +
            "    if (radius >= minHalf * 0.95) {\n" +
            "        float r = min(radius, minHalf);\n" +
            "        vec2 q = abs(point) - halfSize + vec2(r);\n" +
            "        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;\n" +
            "    }\n" +
            "    vec2 q = abs(point) - halfSize;\n" +
            "    float L = min(radius * 3.0, minHalf);\n" +
            "    vec2 w = max(q + vec2(L), 0.0) / max(L, 0.0001);\n" +
            "    if (w.x == 0.0 && w.y == 0.0) {\n" +
            "        return max(q.x, q.y);\n" +
            "    }\n" +
            "    vec2 w2 = w * w;\n" +
            "    vec2 w4 = w2 * w2;\n" +
            "    float norm = sqrt(sqrt(w4.x + w4.y));\n" +
            "    return (norm - 1.0) * L;\n" +
            "}\n" +
            "\n" +
            "float circularSdf(vec2 point, vec2 halfSize, vec4 radii) {\n" +
            "    float radius = selectRadius(point, radii);\n" +
            "    vec2 q = abs(point) - halfSize + vec2(radius);\n" +
            "    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 halfSize = u_size * 0.5;\n" +
            "    vec2 pos = gl_TexCoord[0].st * u_size;\n" +
            "    vec2 centerPos = pos - halfSize;\n" +
            "\n" +
            "    float dist = (u_isSquircle == 1) ? squircleSdf(centerPos, halfSize, u_radii) : circularSdf(centerPos, halfSize, u_radii);\n" +
            "\n" +
            "    #if defined(GL_OES_standard_derivatives) || __VERSION__ >= 130\n" +
            "        float edge = max(fwidth(dist), 0.75);\n" +
            "    #else\n" +
            "        float edge = 0.85;\n" +
            "    #endif\n" +
            "\n" +
            "    float alpha = clamp(0.5 - dist / edge, 0.0, 1.0);\n" +
            "    alpha = alpha * alpha * (3.0 - 2.0 * alpha);\n" +
            "\n" +
            "    vec2 uv = gl_TexCoord[0].st;\n" +
            "    vec4 topCol = mix(u_color1, u_color2, uv.x);\n" +
            "    vec4 botCol = mix(u_color4, u_color3, uv.x);\n" +
            "    vec4 col = mix(topCol, botCol, uv.y);\n" +
            "\n" +
            "    if (u_mode == 0 || u_mode == 1) {\n" +
            "        if (alpha <= 0.001) discard;\n" +
            "        gl_FragColor = vec4(col.rgb, col.a * alpha);\n" +
            "    } else if (u_mode == 2) {\n" +
            "        float innerDist = dist + u_thickness;\n" +
            "        float innerAlpha = clamp(0.5 - innerDist / edge, 0.0, 1.0);\n" +
            "        innerAlpha = innerAlpha * innerAlpha * (3.0 - 2.0 * innerAlpha);\n" +
            "        float outlineAlpha = max(alpha - innerAlpha, 0.0);\n" +
            "        if (outlineAlpha <= 0.001) discard;\n" +
            "        gl_FragColor = vec4(col.rgb, col.a * outlineAlpha);\n" +
            "    } else if (u_mode == 3) {\n" +
            "        float x = max(dist, 0.0) / max(u_softness, 0.0001);\n" +
            "        float shadowAlpha = u_color1.a * exp(-4.5 * x * x);\n" +
            "        if (shadowAlpha <= 0.005) discard;\n" +
            "        gl_FragColor = vec4(u_color1.rgb, shadowAlpha);\n" +
            "    }\n" +
            "}\n";

    private static final String BLUR_FRAGMENT_SHADER =
            "#version 120\n" +
            "#extension GL_OES_standard_derivatives : enable\n" +
            "\n" +
            "uniform sampler2D u_screenTexture;\n" +
            "uniform vec2 u_screenSize;\n" +
            "uniform vec2 u_size;\n" +
            "uniform vec4 u_radii;\n" +
            "uniform vec4 u_tintColor;\n" +
            "uniform float u_blurRadius;\n" +
            "\n" +
            "float selectRadius(vec2 p, vec4 r) {\n" +
            "    if (p.x >= 0.0) return p.y < 0.0 ? r.y : r.z;\n" +
            "    return p.y < 0.0 ? r.x : r.w;\n" +
            "}\n" +
            "\n" +
            "float squircleSdf(vec2 point, vec2 halfSize, vec4 radii) {\n" +
            "    float radius = selectRadius(point, radii);\n" +
            "    if (radius <= 0.0001) {\n" +
            "        vec2 q = abs(point) - halfSize;\n" +
            "        return max(q.x, q.y);\n" +
            "    }\n" +
            "    float minHalf = min(halfSize.x, halfSize.y);\n" +
            "    if (radius >= minHalf * 0.95) {\n" +
            "        float r = min(radius, minHalf);\n" +
            "        vec2 q = abs(point) - halfSize + vec2(r);\n" +
            "        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;\n" +
            "    }\n" +
            "    vec2 q = abs(point) - halfSize;\n" +
            "    float L = min(radius * 3.0, minHalf);\n" +
            "    vec2 w = max(q + vec2(L), 0.0) / max(L, 0.0001);\n" +
            "    if (w.x == 0.0 && w.y == 0.0) return max(q.x, q.y);\n" +
            "    vec2 w2 = w * w;\n" +
            "    vec2 w4 = w2 * w2;\n" +
            "    return (sqrt(sqrt(w4.x + w4.y)) - 1.0) * L;\n" +
            "}\n" +
            "\n" +
            "const int BLUR_SAMPLES = 20;\n" +
            "const float GOLDEN_ANGLE = 2.39996323;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 halfSize = u_size * 0.5;\n" +
            "    vec2 pos = gl_TexCoord[0].st * u_size;\n" +
            "    vec2 centerPos = pos - halfSize;\n" +
            "\n" +
            "    float dist = squircleSdf(centerPos, halfSize, u_radii);\n" +
            "    float edge = max(fwidth(dist), 0.85);\n" +
            "    float mask = clamp(0.5 - dist / edge, 0.0, 1.0);\n" +
            "    mask = mask * mask * (3.0 - 2.0 * mask);\n" +
            "\n" +
            "    if (mask <= 0.001) discard;\n" +
            "\n" +
            "    vec2 screenUv = gl_FragCoord.xy / u_screenSize;\n" +
            "    vec2 invTexSize = 1.0 / u_screenSize;\n" +
            "\n" +
            "    float effBlur = max(u_blurRadius, 1.0);\n" +
            "    vec2 blurStep = effBlur * invTexSize * 1.8;\n" +
            "\n" +
            "    vec3 acc = vec3(0.0);\n" +
            "    float weightSum = 0.0;\n" +
            "\n" +
            "    for (int i = 0; i < BLUR_SAMPLES; i++) {\n" +
            "        float t = (float(i) + 0.5) / float(BLUR_SAMPLES);\n" +
            "        float r = sqrt(t);\n" +
            "        float theta = float(i) * GOLDEN_ANGLE;\n" +
            "        vec2 offset = vec2(cos(theta), sin(theta)) * (r * blurStep);\n" +
            "\n" +
            "        vec3 s = texture2D(u_screenTexture, clamp(screenUv + offset, vec2(0.001), vec2(0.999))).rgb;\n" +
            "        float w = exp(-2.0 * t);\n" +
            "        acc += s * w;\n" +
            "        weightSum += w;\n" +
            "    }\n" +
            "\n" +
            "    vec3 blurred = acc / max(weightSum, 0.0001);\n" +
            "    vec3 panel = mix(blurred, u_tintColor.rgb, clamp(u_tintColor.a, 0.0, 1.0));\n" +
            "\n" +
            "    float innerDist = dist + 1.0;\n" +
            "    float innerMask = clamp(0.5 - innerDist / edge, 0.0, 1.0);\n" +
            "    innerMask = innerMask * innerMask * (3.0 - 2.0 * innerMask);\n" +
            "    float borderAlpha = max(mask - innerMask, 0.0) * 0.25;\n" +
            "\n" +
            "    vec3 finalRgb = mix(panel, vec3(1.0), borderAlpha);\n" +
            "    gl_FragColor = vec4(finalRgb, mask * 0.95);\n" +
            "}\n";

    private Render2D() {}

    private static void ensureShader() {
        if (squircleShader == null) {
            squircleShader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);
        }
    }

    private static void drawQuad(float x, float y, float width, float height) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0).tex(0.0, 1.0).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0).tex(1.0, 1.0).endVertex();
        worldrenderer.pos(x + width, y, 0.0).tex(1.0, 0.0).endVertex();
        worldrenderer.pos(x, y, 0.0).tex(0.0, 0.0).endVertex();
        tessellator.draw();
    }

    private static void setupRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void restoreRenderState() {
        Shader.unbind();
        GlStateManager.enableTexture2D();
    }

    public static void drawSquircle(float x, float y, float width, float height, float radius, int color) {
        drawSquircle(x, y, width, height, radius, radius, radius, radius, color);
    }

    public static void drawSquircle(float x, float y, float width, float height, float r1, float r2, float r3, float r4, int color) {
        if (width <= 0.0F || height <= 0.0F || ColorUtils.getAlpha(color) == 0) return;
        ensureShader();
        setupRenderState();

        float[] c = ColorUtils.toFloats(color);
        squircleShader.bind();
        squircleShader.setUniform2f("u_size", width, height);
        squircleShader.setUniform4f("u_radii", r1, r2, r3, r4);
        squircleShader.setUniform4f("u_color1", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color2", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color3", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color4", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform1i("u_mode", 0);
        squircleShader.setUniform1i("u_isSquircle", 1);

        drawQuad(x, y, width, height);
        restoreRenderState();
    }

    public static void drawBlurredSquircle(float x, float y, float width, float height, float radius, float blurRadius, int tintColor) {
        if (width <= 0.0F || height <= 0.0F) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return;

        if (blurSquircleShader == null) {
            blurSquircleShader = new Shader(VERTEX_SHADER, BLUR_FRAGMENT_SHADER);
        }

        if (backdropTexture == -1 || backdropWidth != mc.displayWidth || backdropHeight != mc.displayHeight) {
            if (backdropTexture != -1) {
                GL11.glDeleteTextures(backdropTexture);
            }
            backdropTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, backdropTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 0, 0, mc.displayWidth, mc.displayHeight, 0);
            backdropWidth = mc.displayWidth;
            backdropHeight = mc.displayHeight;
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, backdropTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, mc.displayWidth, mc.displayHeight);
        }

        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.bindTexture(backdropTexture);

        float[] c = ColorUtils.toFloats(tintColor);
        blurSquircleShader.bind();
        blurSquircleShader.setUniform1i("u_screenTexture", 0);
        blurSquircleShader.setUniform2f("u_screenSize", mc.displayWidth, mc.displayHeight);
        blurSquircleShader.setUniform2f("u_size", width, height);
        blurSquircleShader.setUniform4f("u_radii", radius, radius, radius, radius);
        blurSquircleShader.setUniform4f("u_tintColor", c[0], c[1], c[2], c[3]);
        blurSquircleShader.setUniform1f("u_blurRadius", blurRadius);

        drawQuad(x, y, width, height);

        Shader.unbind();
        GlStateManager.enableTexture2D();
    }

    public static void drawGradientSquircle(float x, float y, float width, float height, float radius, int c1, int c2, int c3, int c4) {
        if (width <= 0.0F || height <= 0.0F) return;
        ensureShader();
        setupRenderState();

        float[] f1 = ColorUtils.toFloats(c1);
        float[] f2 = ColorUtils.toFloats(c2);
        float[] f3 = ColorUtils.toFloats(c3);
        float[] f4 = ColorUtils.toFloats(c4);

        squircleShader.bind();
        squircleShader.setUniform2f("u_size", width, height);
        squircleShader.setUniform4f("u_radii", radius, radius, radius, radius);
        squircleShader.setUniform4f("u_color1", f1[0], f1[1], f1[2], f1[3]);
        squircleShader.setUniform4f("u_color2", f2[0], f2[1], f2[2], f2[3]);
        squircleShader.setUniform4f("u_color3", f3[0], f3[1], f3[2], f3[3]);
        squircleShader.setUniform4f("u_color4", f4[0], f4[1], f4[2], f4[3]);
        squircleShader.setUniform1i("u_mode", 1);
        squircleShader.setUniform1i("u_isSquircle", 1);

        drawQuad(x, y, width, height);
        restoreRenderState();
    }

    public static void drawOutlineSquircle(float x, float y, float width, float height, float radius, float thickness, int color) {
        if (width <= 0.0F || height <= 0.0F || ColorUtils.getAlpha(color) == 0) return;
        ensureShader();
        setupRenderState();

        float[] c = ColorUtils.toFloats(color);
        squircleShader.bind();
        squircleShader.setUniform2f("u_size", width, height);
        squircleShader.setUniform4f("u_radii", radius, radius, radius, radius);
        squircleShader.setUniform4f("u_color1", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color2", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color3", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color4", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform1i("u_mode", 2);
        squircleShader.setUniform1f("u_thickness", thickness);
        squircleShader.setUniform1i("u_isSquircle", 1);

        drawQuad(x, y, width, height);
        restoreRenderState();
    }

    public static void drawGlow(float x, float y, float width, float height, float radius, float glowSize, int color) {
        if (width <= 0.0F || height <= 0.0F || glowSize <= 0.0F || ColorUtils.getAlpha(color) == 0) return;
        ensureShader();
        setupRenderState();

        float pad = glowSize * 2.0F;
        float drawX = x - pad;
        float drawY = y - pad;
        float drawW = width + pad * 2.0F;
        float drawH = height + pad * 2.0F;

        float[] c = ColorUtils.toFloats(color);
        squircleShader.bind();
        squircleShader.setUniform2f("u_size", drawW, drawH);
        squircleShader.setUniform4f("u_radii", radius, radius, radius, radius);
        squircleShader.setUniform4f("u_color1", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform1i("u_mode", 3);
        squircleShader.setUniform1f("u_softness", glowSize);
        squircleShader.setUniform1i("u_isSquircle", 1);

        drawQuad(drawX, drawY, drawW, drawH);
        restoreRenderState();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0F || height <= 0.0F || ColorUtils.getAlpha(color) == 0) return;
        ensureShader();
        setupRenderState();

        float[] c = ColorUtils.toFloats(color);
        squircleShader.bind();
        squircleShader.setUniform2f("u_size", width, height);
        squircleShader.setUniform4f("u_radii", radius, radius, radius, radius);
        squircleShader.setUniform4f("u_color1", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color2", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color3", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform4f("u_color4", c[0], c[1], c[2], c[3]);
        squircleShader.setUniform1i("u_mode", 0);
        squircleShader.setUniform1i("u_isSquircle", 0);

        drawQuad(x, y, width, height);
        restoreRenderState();
    }

    public static void drawCircle(float centerX, float centerY, float radius, int color) {
        drawRoundedRect(centerX - radius, centerY - radius, radius * 2.0F, radius * 2.0F, radius, color);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        drawRoundedRect(x, y, width, height, 0.0F, color);
    }

    public static void pushScissor(float x, float y, float width, float height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        int scissorX = (int) Math.round(x * scale);
        int scissorY = (int) Math.round((sr.getScaledHeight() - (y + height)) * scale);
        int scissorW = (int) Math.round(width * scale);
        int scissorH = (int) Math.round(height * scale);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(Math.max(0, scissorX), Math.max(0, scissorY), Math.max(0, scissorW), Math.max(0, scissorH));
    }

    public static void popScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
