package android.graphics;

public class Color
{
    public static final int BLACK = 0xFF000000;
    public static final int DKGRAY = 0xFF444444;
    public static final int GRAY = 0xFF888888;
    public static final int LTGRAY = 0xFFCCCCCC;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;

    public static int alpha(int color)
    {
        return color >>> 24;
    }

    public static int red(int color)
    {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color)
    {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color)
    {
        return color & 0xFF;
    }

    public static int rgb(int red, int green, int blue)
    {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static int argb(int alpha, int red, int green, int blue)
    {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int rgb(float red, float green, float blue)
    {
        return argb(1.0f, red, green, blue);
    }

    public static int argb(float alpha, float red, float green, float blue)
    {
        return ((int)(alpha * 255.0f + 0.5f) << 24) | ((int)(red * 255.0f + 0.5f) << 16) | ((int)(green * 255.0f + 0.5f) << 8)
               | (int)(blue * 255.0f + 0.5f);
    }

    public static float luminance(int color)
    {
        return (float)(0.2126 * linear(red(color) / 255.0) + 0.7152 * linear(green(color) / 255.0)
                       + 0.0722 * linear(blue(color) / 255.0));
    }

    private static double linear(double component)
    {
        return component <= 0.04045 ? component / 12.92 : Math.pow((component + 0.055) / 1.055, 2.4);
    }

    public static int parseColor(String colorString)
    {
        if (colorString == null || colorString.isEmpty()) throw new IllegalArgumentException("Unknown color");

        if (colorString.charAt(0) == '#')
        {
            long color = Long.parseLong(colorString.substring(1), 16);

            if (colorString.length() == 7) color |= 0x00000000ff000000L;
            else if (colorString.length() != 9) throw new IllegalArgumentException("Unknown color");

            return (int)color;
        }

        Integer named = NAMED_COLORS.get(colorString.toLowerCase(java.util.Locale.ROOT));
        if (named != null) return named;

        throw new IllegalArgumentException("Unknown color");
    }

    private static final java.util.HashMap<String, Integer> NAMED_COLORS = new java.util.HashMap<>();

    static
    {
        NAMED_COLORS.put("black", BLACK);
        NAMED_COLORS.put("darkgray", DKGRAY);
        NAMED_COLORS.put("gray", GRAY);
        NAMED_COLORS.put("lightgray", LTGRAY);
        NAMED_COLORS.put("white", WHITE);
        NAMED_COLORS.put("red", RED);
        NAMED_COLORS.put("green", GREEN);
        NAMED_COLORS.put("blue", BLUE);
        NAMED_COLORS.put("yellow", YELLOW);
        NAMED_COLORS.put("cyan", CYAN);
        NAMED_COLORS.put("magenta", MAGENTA);
        NAMED_COLORS.put("aqua", 0xFF00FFFF);
        NAMED_COLORS.put("fuchsia", 0xFFFF00FF);
        NAMED_COLORS.put("darkgrey", DKGRAY);
        NAMED_COLORS.put("grey", GRAY);
        NAMED_COLORS.put("lightgrey", LTGRAY);
        NAMED_COLORS.put("lime", 0xFF00FF00);
        NAMED_COLORS.put("maroon", 0xFF800000);
        NAMED_COLORS.put("navy", 0xFF000080);
        NAMED_COLORS.put("olive", 0xFF808000);
        NAMED_COLORS.put("purple", 0xFF800080);
        NAMED_COLORS.put("silver", 0xFFC0C0C0);
        NAMED_COLORS.put("teal", 0xFF008080);
    }

    public static void RGBToHSV(int red, int green, int blue, float[] hsv)
    {
        if (hsv.length < 3) throw new RuntimeException("3 components required for hsv");

        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue = 0.0f;
        if (delta != 0.0f)
        {
            if (max == r) hue = (g - b) / delta;
            else if (max == g) hue = 2.0f + (b - r) / delta;
            else hue = 4.0f + (r - g) / delta;

            hue *= 60.0f;
            if (hue < 0.0f) hue += 360.0f;
        }

        hsv[0] = hue;
        hsv[1] = max == 0.0f ? 0.0f : delta / max;
        hsv[2] = max;
    }

    public static void colorToHSV(int color, float[] hsv)
    {
        RGBToHSV(red(color), green(color), blue(color), hsv);
    }

    public static int HSVToColor(float[] hsv)
    {
        return HSVToColor(0xFF, hsv);
    }

    public static int HSVToColor(int alpha, float[] hsv)
    {
        if (hsv.length < 3) throw new RuntimeException("3 components required for hsv");

        float hue = ((hsv[0] % 360.0f) + 360.0f) % 360.0f;
        float saturation = Math.max(0.0f, Math.min(1.0f, hsv[1]));
        float value = Math.max(0.0f, Math.min(1.0f, hsv[2]));

        float chroma = value * saturation;
        float sector = hue / 60.0f;
        float second = chroma * (1.0f - Math.abs(sector % 2.0f - 1.0f));
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;

        if (sector < 1.0f)
        {
            r = chroma;
            g = second;
        }
        else if (sector < 2.0f)
        {
            r = second;
            g = chroma;
        }
        else if (sector < 3.0f)
        {
            g = chroma;
            b = second;
        }
        else if (sector < 4.0f)
        {
            g = second;
            b = chroma;
        }
        else if (sector < 5.0f)
        {
            r = second;
            b = chroma;
        }
        else
        {
            r = chroma;
            b = second;
        }

        float offset = value - chroma;
        return argb(alpha, Math.round((r + offset) * 255.0f), Math.round((g + offset) * 255.0f), Math.round((b + offset) * 255.0f));
    }

    private static final int SRGB_IDENTIFIER = 0;

    private static int identifierOf(long color)
    {
        return (int)(color & 0x3fL);
    }

    private static float halfToFloat(int half)
    {
        int sign = (half >> 15) & 0x1;
        int exponent = (half >> 10) & 0x1f;
        int mantissa = half & 0x3ff;

        float magnitude;
        if (exponent == 0) magnitude = mantissa / 1024.0f * (float)Math.pow(2, -14);
        else if (exponent == 31) magnitude = mantissa == 0 ? Float.POSITIVE_INFINITY : Float.NaN;
        else magnitude = (1.0f + mantissa / 1024.0f) * (float)Math.pow(2, exponent - 15);

        return sign == 0 ? magnitude : -magnitude;
    }

    private static int floatToHalf(float value)
    {
        int bits = Float.floatToIntBits(value);
        int sign = (bits >>> 16) & 0x8000;
        int exponent = ((bits >>> 23) & 0xff) - 127 + 15;
        int mantissa = bits & 0x7fffff;

        if (exponent <= 0) return sign;
        if (exponent >= 31) return sign | 0x7c00;

        return sign | (exponent << 10) | (mantissa >> 13);
    }

    public static ColorSpace colorSpace(long color)
    {
        int identifier = identifierOf(color);
        ColorSpace.Named[] named = ColorSpace.Named.values();

        return ColorSpace.get(identifier < named.length ? named[identifier] : ColorSpace.Named.SRGB);
    }

    public static float red(long color)
    {
        if (identifierOf(color) == SRGB_IDENTIFIER) return ((color >> 48) & 0xff) / 255.0f;
        return halfToFloat((int)((color >> 48) & 0xffff));
    }

    public static float green(long color)
    {
        if (identifierOf(color) == SRGB_IDENTIFIER) return ((color >> 40) & 0xff) / 255.0f;
        return halfToFloat((int)((color >> 32) & 0xffff));
    }

    public static float blue(long color)
    {
        if (identifierOf(color) == SRGB_IDENTIFIER) return ((color >> 32) & 0xff) / 255.0f;
        return halfToFloat((int)((color >> 16) & 0xffff));
    }

    public static float alpha(long color)
    {
        if (identifierOf(color) == SRGB_IDENTIFIER) return ((color >> 56) & 0xff) / 255.0f;
        return ((color >> 6) & 0x3ff) / 1023.0f;
    }

    public static boolean isSrgb(long color)
    {
        return identifierOf(color) == SRGB_IDENTIFIER;
    }

    public static boolean isWideGamut(long color)
    {
        return colorSpace(color).isWideGamut();
    }

    public static int toArgb(long color)
    {
        if (identifierOf(color) == SRGB_IDENTIFIER) return (int)(color >> 32);
        return argb(alpha(color), clamp(red(color)), clamp(green(color)), clamp(blue(color)));
    }

    private static float clamp(float component)
    {
        return Math.max(0.0f, Math.min(1.0f, component));
    }

    public static float luminance(long color)
    {
        return luminance(toArgb(color));
    }

    public static Color valueOf(long color)
    {
        return new Color(red(color), green(color), blue(color), alpha(color), colorSpace(color));
    }

    public static Color valueOf(float[] components, ColorSpace colorSpace)
    {
        if (components.length < colorSpace.getComponentCount() + 1)
        {
            throw new IllegalArgumentException("Received a component array of length " + components.length
                                               + " but the color model requires " + (colorSpace.getComponentCount() + 1)
                                               + " (including alpha)");
        }

        return new Color(java.util.Arrays.copyOf(components, colorSpace.getComponentCount() + 1), colorSpace);
    }

    public static long pack(int color)
    {
        return ((long)color & 0xffffffffL) << 32;
    }

    public static long pack(float red, float green, float blue)
    {
        return pack(red, green, blue, 1.0f, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    public static long pack(float red, float green, float blue, float alpha)
    {
        return pack(red, green, blue, alpha, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    public static long pack(float red, float green, float blue, float alpha, ColorSpace colorSpace)
    {
        if (colorSpace == null || colorSpace.isSrgb()) return pack(argb(alpha, clamp(red), clamp(green), clamp(blue)));

        int identifier = colorSpace.getId();
        if (identifier < 0) throw new IllegalArgumentException("unknown color space " + colorSpace);

        long packedAlpha = (long)(Math.max(0.0f, Math.min(1.0f, alpha)) * 1023.0f + 0.5f) & 0x3ffL;

        return ((long)floatToHalf(red) & 0xffffL) << 48 | ((long)floatToHalf(green) & 0xffffL) << 32
               | ((long)floatToHalf(blue) & 0xffffL) << 16 | packedAlpha << 6 | ((long)identifier & 0x3fL);
    }

    public static long convert(int color, ColorSpace colorSpace)
    {
        return pack(red(color) / 255.0f, green(color) / 255.0f, blue(color) / 255.0f, alpha(color) / 255.0f, colorSpace);
    }

    public static long convert(long color, ColorSpace colorSpace)
    {
        return convert(red(color), green(color), blue(color), alpha(color), colorSpace(color), colorSpace);
    }

    public static long convert(float red, float green, float blue, float alpha, ColorSpace source, ColorSpace destination)
    {
        float[] converted = ColorSpace.connect(source, destination).transform(new float[] {red, green, blue});
        return pack(converted[0], converted[1], converted[2], alpha, destination);
    }

    public static long convert(long color, ColorSpace.Connector connector)
    {
        return convert(red(color), green(color), blue(color), alpha(color), connector);
    }

    public static long convert(float red, float green, float blue, float alpha, ColorSpace.Connector connector)
    {
        float[] converted = connector.transform(new float[] {red, green, blue});
        return pack(converted[0], converted[1], converted[2], alpha, connector.getDestination());
    }

    public static boolean isInColorSpace(long color, ColorSpace colorSpace)
    {
        return identifierOf(color) == colorSpace.getId();
    }

    private final float[] mComponents;
    private final ColorSpace mColorSpace;

    public Color()
    {
        mComponents = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
        mColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    }

    private Color(float red, float green, float blue, float alpha)
    {
        this(red, green, blue, alpha, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    private Color(float red, float green, float blue, float alpha, ColorSpace colorSpace)
    {
        mComponents = new float[] {red, green, blue, alpha};
        mColorSpace = colorSpace;
    }

    private Color(float[] components, ColorSpace colorSpace)
    {
        mComponents = components;
        mColorSpace = colorSpace;
    }

    public static Color valueOf(int color)
    {
        return new Color(red(color) / 255.0f, green(color) / 255.0f, blue(color) / 255.0f, alpha(color) / 255.0f);
    }

    public static Color valueOf(float red, float green, float blue)
    {
        return new Color(red, green, blue, 1.0f);
    }

    public static Color valueOf(float red, float green, float blue, float alpha)
    {
        return new Color(clamp(red), clamp(green), clamp(blue), clamp(alpha));
    }

    public static Color valueOf(float red, float green, float blue, float alpha, ColorSpace colorSpace)
    {
        if (colorSpace.getComponentCount() > 3)
        {
            throw new IllegalArgumentException("The specified color space must use a color model with at most 3 color components");
        }

        return new Color(red, green, blue, alpha, colorSpace);
    }

    public boolean isSrgb()
    {
        return mColorSpace.isSrgb();
    }

    public boolean isWideGamut()
    {
        return mColorSpace.isWideGamut();
    }

    public int toArgb()
    {
        if (mColorSpace.isSrgb())
        {
            return argb(Math.round(clamp(mComponents[3]) * 255.0f), Math.round(clamp(mComponents[0]) * 255.0f),
                        Math.round(clamp(mComponents[1]) * 255.0f), Math.round(clamp(mComponents[2]) * 255.0f));
        }

        float[] converted = ColorSpace.connect(mColorSpace).transform(new float[] {mComponents[0], mComponents[1], mComponents[2]});
        return argb(Math.round(clamp(mComponents[mComponents.length - 1]) * 255.0f), Math.round(clamp(converted[0]) * 255.0f),
                    Math.round(clamp(converted[1]) * 255.0f), Math.round(clamp(converted[2]) * 255.0f));
    }

    public float red()
    {
        return mComponents[0];
    }

    public float green()
    {
        return mComponents[1];
    }

    public float blue()
    {
        return mComponents[2];
    }

    public float alpha()
    {
        return mComponents[mComponents.length - 1];
    }

    public ColorSpace getColorSpace()
    {
        return mColorSpace;
    }

    public ColorSpace.Model getModel()
    {
        return mColorSpace.getModel();
    }

    public int getComponentCount()
    {
        return mColorSpace.getComponentCount() + 1;
    }

    public Color convert(ColorSpace colorSpace)
    {
        ColorSpace.Connector connector = ColorSpace.connect(mColorSpace, colorSpace);
        float[] converted = connector.transform(new float[] {mComponents[0], mComponents[1], mComponents[2]});
        return new Color(converted[0], converted[1], converted[2], mComponents[mComponents.length - 1], colorSpace);
    }

    public float[] getComponents()
    {
        return mComponents.clone();
    }

    public float[] getComponents(float[] components)
    {
        if (components == null) return getComponents();

        if (components.length < mComponents.length)
        {
            throw new IllegalArgumentException("The specified array's length must be at least " + mComponents.length);
        }

        System.arraycopy(mComponents, 0, components, 0, mComponents.length);
        return components;
    }

    public float getComponent(int component)
    {
        return mComponents[component];
    }

    public long pack()
    {
        return pack(mComponents[0], mComponents[1], mComponents[2], mComponents[mComponents.length - 1], mColorSpace);
    }

    public float luminance()
    {
        return luminance(toArgb());
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Color)) return false;

        Color color = (Color)other;
        return java.util.Arrays.equals(mComponents, color.mComponents) && mColorSpace.equals(color.mColorSpace);
    }

    @Override
    public int hashCode()
    {
        return 31 * java.util.Arrays.hashCode(mComponents) + mColorSpace.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("Color(");
        for (float component : mComponents) builder.append(component).append(", ");
        return builder.append(mColorSpace.getName()).append(')').toString();
    }
}
