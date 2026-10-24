package amoralsolution.test.utils.render.font;

public enum FontType {

    COMMISSIONER_THIN("Commissioner-Thin.ttf", 100),
    COMMISSIONER_EXTRALIGHT("Commissioner-ExtraLight.ttf", 200),
    COMMISSIONER_LIGHT("Commissioner-Light.ttf", 300),
    COMMISSIONER_REGULAR("Commissioner-Regular.ttf", 400),
    COMMISSIONER_MEDIUM("Commissioner-Medium.ttf", 500),
    COMMISSIONER_SEMIBOLD("Commissioner-SemiBold.ttf", 600),
    COMMISSIONER_BOLD("Commissioner-Bold.ttf", 700),
    COMMISSIONER_EXTRABOLD("Commissioner-ExtraBold.ttf", 800),
    COMMISSIONER_BLACK("Commissioner-Black.ttf", 900),

    LORA_REGULAR("Lora-Regular.ttf", 400),
    LORA_BOLD("Lora-Bold.ttf", 700),

    MINECRAFT_REGULAR("MinecraftRegular.otf", 400),
    MINECRAFT_BOLD("MinecraftBold.otf", 700);

    private final String ttfPath;
    private final int weight;

    FontType(String ttfPath, int weight) {
        this.ttfPath = ttfPath;
        this.weight = weight;
    }

    public String ttfFile() {
        return this.ttfPath;
    }

    public int weight() {
        return this.weight;
    }

    public static FontType commissioner(int weight) {
        if (weight <= 150) return COMMISSIONER_THIN;
        if (weight <= 250) return COMMISSIONER_EXTRALIGHT;
        if (weight <= 350) return COMMISSIONER_LIGHT;
        if (weight <= 450) return COMMISSIONER_REGULAR;
        if (weight <= 550) return COMMISSIONER_MEDIUM;
        if (weight <= 650) return COMMISSIONER_SEMIBOLD;
        if (weight <= 750) return COMMISSIONER_BOLD;
        if (weight <= 850) return COMMISSIONER_EXTRABOLD;
        return COMMISSIONER_BLACK;
    }
}
