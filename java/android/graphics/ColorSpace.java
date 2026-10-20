package android.graphics;

public abstract class ColorSpace
{
    public static final int MIN_ID = -1;
    public static final int MAX_ID = 63;

    private final String mName;
    private final int mId;
    private final Model mModel;

    private ColorSpace(String name, Model model, int id)
    {
        mName = name;
        mModel = model;
        mId = id;
    }

    public String getName()
    {
        return mName;
    }

    public int getId()
    {
        return mId;
    }

    public Model getModel()
    {
        return mModel;
    }

    public int getComponentCount()
    {
        return mModel.getComponentCount();
    }

    public boolean isSrgb()
    {
        return mId == Named.SRGB.ordinal();
    }

    public abstract boolean isWideGamut();

    public static ColorSpace get(Named name)
    {
        int index = name == null ? Named.SRGB.ordinal() : name.ordinal();

        if (sSpaces[index] == null) sSpaces[index] = new Rgb(name == null ? Named.SRGB : name);

        return sSpaces[index];
    }

    @Override
    public String toString()
    {
        return mName;
    }

    private static final ColorSpace[] sSpaces = new ColorSpace[Named.values().length];

    public static class Rgb extends ColorSpace
    {
        private final Named mNamed;

        Rgb(Named named)
        {
            super(named.name(), Model.RGB, named.ordinal());
            mNamed = named;
        }

        @Override
        public boolean isWideGamut()
        {
            return mNamed != Named.SRGB && mNamed != Named.LINEAR_SRGB;
        }
    }

    public enum Model
    {
        RGB(3),
        XYZ(3),
        LAB(3),
        CMYK(4);

        private final int mComponents;

        Model(int components)
        {
            mComponents = components;
        }

        public int getComponentCount()
        {
            return mComponents;
        }
    }

    public enum Named
    {
        SRGB,
        LINEAR_SRGB,
        EXTENDED_SRGB,
        LINEAR_EXTENDED_SRGB,
        BT709,
        BT2020,
        DCI_P3,
        DISPLAY_P3,
        NTSC_1953,
        SMPTE_C,
        ADOBE_RGB,
        PRO_PHOTO_RGB,
        ACES,
        ACESCG,
        CIE_XYZ,
        CIE_LAB
    }
}
