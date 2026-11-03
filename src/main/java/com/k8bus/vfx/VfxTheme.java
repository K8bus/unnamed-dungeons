package com.k8bus.vfx;

public enum VfxTheme {
    FIRE(0xFF5500, 0xFFAA00, 0xFF2200),
    FROST(0x55FFFF, 0x00AAFF, 0xFFFFFF),
    LIGHTNING(0xFFFF55, 0x55FFFF, 0xFFFFAA),
    POISON(0x55FF55, 0x00AA00, 0x228822),
    VOID(0xAA00FF, 0x5500AA, 0x220044),
    RADIANCE(0xFFFF55, 0xFFAA00, 0xFFFFFF),
    SOUL(0x00FFFF, 0x0088AA, 0x55FFFF),
    DEFLECT(0xFFAA00, 0xFFFFFF, 0xFFDD55),
    VINES(0x33AA33, 0x116611, 0x55FF55),
    WEBBING(0xEEEEEE, 0xCCCCCC, 0xFFFFFF);

    private final int primaryColor;
    private final int secondaryColor;
    private final int glowColor;

    VfxTheme(int primaryColor, int secondaryColor, int glowColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.glowColor = glowColor;
    }

    public int getPrimaryColor() { return primaryColor; }
    public int getSecondaryColor() { return secondaryColor; }
    public int getGlowColor() { return glowColor; }

    public float getRed() { return ((primaryColor >> 16) & 0xFF) / 255.0f; }
    public float getGreen() { return ((primaryColor >> 8) & 0xFF) / 255.0f; }
    public float getBlue() { return (primaryColor & 0xFF) / 255.0f; }

    public float getSecondaryRed() { return ((secondaryColor >> 16) & 0xFF) / 255.0f; }
    public float getSecondaryGreen() { return ((secondaryColor >> 8) & 0xFF) / 255.0f; }
    public float getSecondaryBlue() { return (secondaryColor & 0xFF) / 255.0f; }
}
