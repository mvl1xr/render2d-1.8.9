package amoralsolution.test.utils.render;

import amoralsolution.test.utils.animation.AnimatedValue;
import amoralsolution.test.utils.animation.Animation;
import amoralsolution.test.utils.render.font.FontRender;
import amoralsolution.test.utils.render.font.FontType;
import amoralsolution.test.utils.render.render2d.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public final class Watermark {

    private static float x = 7.0F;
    private static float y = 7.0F;
    private static float width = 100.0F;
    private static float height = 18.0F;

    private static boolean dragging = false;
    private static float dragOffsetX = 0.0F;
    private static float dragOffsetY = 0.0F;

    private static final AnimatedValue animX = new AnimatedValue(260, Animation.Easing.EASE_OUT_CUBIC);
    private static final AnimatedValue animY = new AnimatedValue(260, Animation.Easing.EASE_OUT_CUBIC);
    private static final AnimatedValue outlineAlphaAnim = new AnimatedValue(180, Animation.Easing.EASE_OUT_CUBIC);

    private Watermark() {}

    public static void render() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();

        if (dragging) {
            if (!Mouse.isButtonDown(0)) {
                dragging = false;
            } else {
                int mouseX = Mouse.getX() * screenW / mc.displayWidth;
                int mouseY = screenH - Mouse.getY() * screenH / mc.displayHeight - 1;
                x = Math.max(2.0F, Math.min(mouseX - dragOffsetX, screenW - width - 2.0F));
                y = Math.max(2.0F, Math.min(mouseY - dragOffsetY, screenH - height - 2.0F));
            }
        }

        float curX = animX.toward(x);
        float curY = animY.toward(y);

        String title = "amoralsolution";
        String separator = "|";
        String fps = Minecraft.getDebugFPS() + " fps";

        float fontSize = 11.2F;

        float titleWidth = FontRender.measureWidth(FontType.LORA_REGULAR, title, fontSize);
        float sepWidth = FontRender.measureWidth(FontType.COMMISSIONER_LIGHT, separator, fontSize);
        float fpsWidth = FontRender.measureWidth(FontType.COMMISSIONER_REGULAR, fps, fontSize);

        float gap = 5.5F;
        float padX = 6.0F;
        float padY = 3.5F;

        float contentWidth = titleWidth + gap + sepWidth + gap + fpsWidth;
        width = contentWidth + padX * 2.0F;
        height = fontSize + padY * 2.0F + 1.2F;
        float radius = 4.0F;

        Render2D.drawBlurredSquircle(curX, curY, width, height, radius, 8.0F, 0x85101014);

        boolean inChat = mc.currentScreen instanceof GuiChat;
        if (inChat) {
            int mouseX = Mouse.getX() * screenW / mc.displayWidth;
            int mouseY = screenH - Mouse.getY() * screenH / mc.displayHeight - 1;
            boolean hovered = mouseX >= curX - 2.0F && mouseX <= curX + width + 2.0F && mouseY >= curY - 2.0F && mouseY <= curY + height + 2.0F;
            float targetAlpha = (hovered || dragging) ? 1.0F : 0.3F;
            float outlineAlpha = outlineAlphaAnim.toward(targetAlpha);
            if (outlineAlpha > 0.01F) {
                int col = ((int) (outlineAlpha * 200.0F) << 24) | 0x00FFFFFF;
                Render2D.drawOutlineSquircle(curX - 1.0F, curY - 1.0F, width + 2.0F, height + 2.0F, radius + 1.0F, 1.2F, col);
            }
        }

        float textStartX = curX + padX;
        float textY = curY + padY + 1.0F;

        FontRender.draw(FontType.LORA_REGULAR, title, textStartX, textY, fontSize, 0xFFFFFFFF);
        textStartX += titleWidth + gap;

        FontRender.draw(FontType.COMMISSIONER_LIGHT, separator, textStartX, textY, fontSize, 0x66FFFFFF);
        textStartX += sepWidth + gap;

        FontRender.draw(FontType.COMMISSIONER_REGULAR, fps, textStartX, textY, fontSize, 0xDDFFFFFF);
    }

    public static boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            float curX = animX.value();
            float curY = animY.value();
            if (mouseX >= curX - 3.0F && mouseX <= curX + width + 3.0F && mouseY >= curY - 3.0F && mouseY <= curY + height + 3.0F) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return true;
            }
        }
        return false;
    }

    public static void mouseReleased() {
        dragging = false;
    }
}
