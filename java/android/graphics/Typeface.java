package android.graphics;

public class Typeface
{
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int BOLD_ITALIC = 3;

    private static final int WEIGHT_NORMAL = 400;
    private static final int WEIGHT_BOLD = 700;

    private static final android.util.LruCache<String, Typeface> sDynamicTypefaceCache = new android.util.LruCache<>(16);
    private static final Object sDynamicCacheLock = new Object();

    private static final android.util.LongSparseArray<android.util.SparseArray<Typeface>> sStyledTypefaceCache =
        new android.util.LongSparseArray<>(3);
    private static final Object sStyledCacheLock = new Object();

    private static final android.util.LongSparseArray<android.util.SparseArray<Typeface>> sWeightTypefaceCache =
        new android.util.LongSparseArray<>(3);
    private static final Object sWeightCacheLock = new Object();

    public static final Typeface DEFAULT = create("sans-serif", NORMAL);
    public static final Typeface DEFAULT_BOLD = create("sans-serif", BOLD);
    public static final Typeface SANS_SERIF = create("sans-serif", NORMAL);
    public static final Typeface SERIF = create("serif", NORMAL);
    public static final Typeface MONOSPACE = create("monospace", NORMAL);

    private final String mFamily;
    private final int mStyle;
    private final int mWeight;
    private final boolean mItalic;
    private final int mNativeFont;
    private final byte[] mFontData;

    private Typeface(String family, int style, int weight, boolean italic, int nativeFont)
    {
        this(family, style, weight, italic, nativeFont, null);
    }

    private Typeface(String family, int style, int weight, boolean italic, int nativeFont, byte[] fontData)
    {
        mFamily = family;
        mStyle = style;
        mWeight = weight;
        mItalic = italic;
        mNativeFont = nativeFont;
        mFontData = fontData;
    }

    private Typeface derive(int style, int weight, boolean italic)
    {
        if (mFontData == null) return build(mFamily, style, weight, italic);

        int font = loadFontData(mFontData, weight, italic);
        return new Typeface(mFamily, style, weight, italic, font == 0 ? mNativeFont : font, mFontData);
    }

    private static int loadFontData(byte[] data, int weight, boolean italic)
    {
        if (data == null || data.length == 0) return 0;

        try
        {
            return nativeLoadFromMemory(data, weight, italic);
        }
        catch (UnsatisfiedLinkError missing)
        {
            return 0;
        }
    }

    public int getNativeFont()
    {
        return mNativeFont;
    }

    public String getFamily()
    {
        return mFamily;
    }

    public int getStyle()
    {
        return mStyle;
    }

    public final boolean isBold()
    {
        return (mStyle & BOLD) != 0;
    }

    public final boolean isItalic()
    {
        return mItalic;
    }

    public int getWeight()
    {
        return mWeight;
    }

    public static Typeface defaultFromStyle(int style)
    {
        return create("sans-serif", style);
    }

    public static Typeface create(Typeface family, int style)
    {
        if ((style & ~BOLD_ITALIC) != 0) style = NORMAL;
        if (family == null) family = DEFAULT;
        if (family == null) return create("sans-serif", style);
        if (family.mStyle == style) return family;

        synchronized (sStyledCacheLock)
        {
            android.util.SparseArray<Typeface> styles = sStyledTypefaceCache.get(family.mNativeFont);

            if (styles == null)
            {
                styles = new android.util.SparseArray<>(4);
                sStyledTypefaceCache.put(family.mNativeFont, styles);
            }

            Typeface cached = styles.get(style);
            if (cached != null && cached.mFamily.equals(family.mFamily)) return cached;

            int weight = (style & BOLD) != 0 ? WEIGHT_BOLD : WEIGHT_NORMAL;
            Typeface created = family.derive(style, weight, (style & ITALIC) != 0);
            styles.put(style, created);

            return created;
        }
    }

    public static Typeface create(Typeface family, int weight, boolean italic)
    {
        if (weight < 1 || weight > 1000) throw new IllegalArgumentException("weight is out of range of [1, 1000] (too big)");
        if (family == null) family = DEFAULT;

        String name = family == null ? "sans-serif" : family.mFamily;
        int style = (weight >= WEIGHT_BOLD ? BOLD : NORMAL) | (italic ? ITALIC : NORMAL);
        if (family == null) return build(name, style, weight, italic);

        int key = (weight << 1) | (italic ? 1 : 0);

        synchronized (sWeightCacheLock)
        {
            android.util.SparseArray<Typeface> variants = sWeightTypefaceCache.get(family.mNativeFont);

            if (variants == null)
            {
                variants = new android.util.SparseArray<>(4);
                sWeightTypefaceCache.put(family.mNativeFont, variants);
            }

            Typeface cached = variants.get(key);
            if (cached != null && cached.mFamily.equals(name)) return cached;

            Typeface created = family.derive(style, weight, italic);
            variants.put(key, created);

            return created;
        }
    }

    public static Typeface create(String familyName, int style)
    {
        int weight = (style & BOLD) != 0 ? WEIGHT_BOLD : WEIGHT_NORMAL;
        boolean italic = (style & ITALIC) != 0;
        return build(familyName, style, weight, italic);
    }

    private static Typeface build(String familyName, int style, int weight, boolean italic)
    {
        String family = familyName == null || familyName.isEmpty() ? "sans-serif" : familyName;
        int font = runtime.TextMeasurers.get().resolveFamily(family, weight, italic);
        return new Typeface(family, style, weight, italic, font);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Typeface)) return false;

        Typeface typeface = (Typeface)other;
        return mStyle == typeface.mStyle && mWeight == typeface.mWeight && mItalic == typeface.mItalic
               && mFamily.equals(typeface.mFamily);
    }

    @Override
    public int hashCode()
    {
        return mFamily.hashCode() * 31 + mStyle * 7 + mWeight + (mItalic ? 1 : 0);
    }

    public static final class CustomFallbackBuilder
    {
        private static final int SLANT_MISMATCH_PENALTY = 1000;

        private int mWeight = 400;
        private boolean mItalic;
        private String mSystemFallback = "sans-serif";
        private final android.graphics.fonts.FontFamily mFamily;

        public CustomFallbackBuilder(android.graphics.fonts.FontFamily family)
        {
            java.util.Objects.requireNonNull(family, "family can not be null");
            mFamily = family;
        }

        public static int getMaxCustomFallbackCount()
        {
            return 64;
        }

        public CustomFallbackBuilder addCustomFallback(android.graphics.fonts.FontFamily family)
        {
            return this;
        }

        public CustomFallbackBuilder setSystemFallback(String familyName)
        {
            if (familyName != null) mSystemFallback = familyName;
            return this;
        }

        public CustomFallbackBuilder setStyle(android.graphics.fonts.FontStyle style)
        {
            if (style == null) return this;

            mWeight = style.getWeight();
            mItalic = style.getSlant() == android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC;

            return this;
        }

        public Typeface build()
        {
            android.graphics.fonts.Font chosen = closestFont();
            if (chosen == null) return Typeface.create(mSystemFallback, mItalic ? Typeface.ITALIC : Typeface.NORMAL);

            return new Builder(chosen).setWeight(mWeight).setItalic(mItalic).setFallback(mSystemFallback).build();
        }

        private android.graphics.fonts.Font closestFont()
        {
            android.graphics.fonts.Font chosen = null;
            int bestScore = Integer.MAX_VALUE;

            for (int index = 0; index < mFamily.getSize(); index++)
            {
                android.graphics.fonts.Font font = mFamily.getFont(index);
                if (font == null || font.getStyle() == null) continue;

                boolean italic = font.getStyle().getSlant() == android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC;
                int score = Math.abs(font.getStyle().getWeight() - mWeight) + (italic == mItalic ? 0 : SLANT_MISMATCH_PENALTY);

                if (score >= bestScore) continue;

                bestScore = score;
                chosen = font;
            }

            return chosen;
        }
    }

    public final String getSystemFontFamilyName()
    {
        return mFamily;
    }

    public static Typeface createFromAsset(android.content.res.AssetManager manager, String path)
    {
        if (path == null) throw new NullPointerException();

        String key = "asset:" + path;

        synchronized (sDynamicCacheLock)
        {
            Typeface cached = sDynamicTypefaceCache.get(key);
            if (cached != null) return cached;

            Typeface created = new Builder(manager, path).build();
            sDynamicTypefaceCache.put(key, created);

            return created;
        }
    }

    public static Typeface createFromFile(java.io.File file)
    {
        if (file == null) throw new NullPointerException();

        return new Builder(file).build();
    }

    public static Typeface createFromFile(String path)
    {
        if (path == null) throw new NullPointerException();

        return createFromFile(new java.io.File(path));
    }

    private static int weightFromName(String name)
    {
        if (name == null) return WEIGHT_NORMAL;

        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("thin") || lower.contains("hairline")) return 100;
        if (lower.contains("extralight") || lower.contains("ultralight")) return 200;
        if (lower.contains("semibold") || lower.contains("demibold")) return 600;
        if (lower.contains("extrabold") || lower.contains("ultrabold")) return 800;
        if (lower.contains("black") || lower.contains("heavy")) return 900;
        if (lower.contains("light")) return 300;
        if (lower.contains("medium")) return 500;
        if (lower.contains("bold")) return WEIGHT_BOLD;
        return WEIGHT_NORMAL;
    }

    private static boolean italicFromName(String name)
    {
        if (name == null) return false;

        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("italic") || lower.contains("oblique");
    }

    public static final class Builder
    {
        private String mFamily = "sans-serif";
        private int mWeight = WEIGHT_NORMAL;
        private boolean mItalic;
        private String mFontVariationSettings;
        private String mSource;
        private byte[] mData;

        public Builder(java.io.File path)
        {
            this(path == null ? null : path.getName());
            if (path == null) return;

            mSource = path.getPath();
            mData = readFile(path);
        }

        public Builder(java.io.FileDescriptor descriptor)
        {
            if (descriptor == null) return;

            mSource = "fd/" + System.identityHashCode(descriptor);

            try
            {
                mData = readStream(new java.io.FileInputStream(descriptor), false);
            }
            catch (java.io.IOException failure)
            {
                mData = null;
            }
        }

        public Builder(String path)
        {
            if (path == null) return;

            mWeight = weightFromName(path);
            mItalic = italicFromName(path);

            if (mSource != null) return;

            java.io.File file = new java.io.File(path);
            if (!file.isFile()) return;

            mSource = file.getPath();
            mData = readFile(file);
        }

        public Builder(android.content.res.AssetManager assets, String path)
        {
            this(assets, path, true, 0);
        }

        public Builder(android.content.res.AssetManager assets, String path, boolean isAsset, int cookie)
        {
            if (path == null) return;

            mWeight = weightFromName(path);
            mItalic = italicFromName(path);
            mSource = (isAsset ? "asset:" : "res:") + path;

            if (assets == null) return;

            try
            {
                mData = readStream(isAsset ? assets.open(path) : assets.openNonAsset(path), true);
            }
            catch (java.io.IOException failure)
            {
                mData = null;
            }
        }

        public Builder(android.graphics.fonts.Font font)
        {
            if (font == null || font.getStyle() == null) return;

            mWeight = font.getStyle().getWeight();
            mItalic = font.getStyle().getSlant() == android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC;

            java.io.File file = font.getFile();
            if (file != null)
            {
                mSource = file.getPath();
                mData = readFile(file);
                return;
            }

            java.nio.ByteBuffer buffer = font.getBuffer();
            if (buffer == null || buffer.remaining() == 0) return;

            byte[] data = new byte[buffer.remaining()];
            buffer.duplicate().get(data);

            mSource = "font/" + font.getSourceIdentifier();
            mData = data;
        }

        private static byte[] readFile(java.io.File file)
        {
            try
            {
                return readStream(new java.io.FileInputStream(file), true);
            }
            catch (java.io.IOException failure)
            {
                return null;
            }
        }

        private static byte[] readStream(java.io.InputStream stream, boolean close) throws java.io.IOException
        {
            try
            {
                java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[16384];

                int read;
                while ((read = stream.read(chunk)) > 0) collected.write(chunk, 0, read);

                return collected.toByteArray();
            }
            finally
            {
                if (close) stream.close();
            }
        }

        public Builder setWeight(int weight)
        {
            mWeight = weight;
            return this;
        }

        public Builder setFontVariationSettings(android.graphics.fonts.FontVariationAxis[] axes)
        {
            mFontVariationSettings = android.graphics.fonts.FontVariationAxis.toFontVariationSettings(axes);
            return this;
        }

        public Builder setItalic(boolean italic)
        {
            mItalic = italic;
            return this;
        }

        public Builder setFallback(String familyName)
        {
            if (familyName != null) mFamily = familyName;
            return this;
        }

        public Builder setTtcIndex(int index)
        {
            return this;
        }

        public Builder setFontVariationSettings(String settings)
        {
            android.graphics.fonts.FontVariationAxis.fromFontVariationSettings(settings);
            mFontVariationSettings = settings;
            return this;
        }

        public Typeface build()
        {
            int style = (mWeight >= WEIGHT_BOLD ? BOLD : NORMAL) | (mItalic ? ITALIC : NORMAL);
            int font = loadFontData(mData, mWeight, mItalic);

            if (font != 0) return new Typeface(mSource, style, mWeight, mItalic, font, mData);

            return Typeface.build(mFamily, style, mWeight, mItalic);
        }
    }

    public static native int nativeResolveFamily(String familyName, int weight, boolean italic);

    public static native int nativeWeight(int font);

    public static native boolean nativeIsItalic(int font);

    private static native int nativeLoadFromMemory(byte[] data, int weight, boolean italic);
}
