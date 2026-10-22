package android.content.res;

import android.util.DisplayMetrics;
import android.util.TypedValue;

public class TypedArray implements AutoCloseable
{
    static final int STYLE_NUM_ENTRIES = 7;
    static final int STYLE_TYPE = 0;
    static final int STYLE_DATA = 1;
    static final int STYLE_ASSET_COOKIE = 2;
    static final int STYLE_RESOURCE_ID = 3;
    static final int STYLE_CHANGING_CONFIGURATIONS = 4;
    static final int STYLE_DENSITY = 5;
    static final int STYLE_SOURCE_RESOURCE_ID = 6;

    private final Resources mResources;
    private int[] mData;
    private int[] mIndices;
    private int[] mAttributes;
    private XmlBlock.Parser mParser;
    private int mLength;
    private boolean mRecycled;
    private Resources.Theme mTheme;
    private String[] mStrings;

    TypedArray(Resources resources)
    {
        mResources = resources;
    }

    TypedArray(Resources resources, Resources.Theme theme)
    {
        mResources = resources;
        mTheme = theme;
    }

    void bind(int[] attributes, int[] data, int[] indices, XmlBlock.Parser parser)
    {
        mAttributes = attributes;
        mData = data;
        mIndices = indices;
        mParser = parser;
        mLength = attributes.length;
        mRecycled = false;
        mStrings = null;
    }

    void bindStrings(String[] strings)
    {
        mStrings = strings;
    }

    public Resources getResources()
    {
        return mResources;
    }

    public int length()
    {
        return mLength;
    }

    public int getIndexCount()
    {
        return mIndices == null ? 0 : mIndices[0];
    }

    public int getIndex(int at)
    {
        return mIndices[at + 1];
    }

    public boolean hasValue(int index)
    {
        return typeAt(index) != TypedValue.TYPE_NULL;
    }

    public boolean hasValueOrEmpty(int index)
    {
        return hasValue(index);
    }

    public int getType(int index)
    {
        return typeAt(index);
    }

    public int getResourceId(int index, int defaultValue)
    {
        if (!inRange(index)) return defaultValue;

        int resourceId = mData[index * STYLE_NUM_ENTRIES + STYLE_RESOURCE_ID];
        return resourceId != 0 ? resourceId : defaultValue;
    }

    public boolean getBoolean(int index, boolean defaultValue)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_NULL) return defaultValue;
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) return dataAt(index) != 0;
        return defaultValue;
    }

    public int getInt(int index, int defaultValue)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_NULL) return defaultValue;
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) return dataAt(index);
        return defaultValue;
    }

    public int getInteger(int index, int defaultValue)
    {
        return getInt(index, defaultValue);
    }

    public float getFloat(int index, float defaultValue)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_FLOAT) return Float.intBitsToFloat(dataAt(index));
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) return dataAt(index);
        return defaultValue;
    }

    public int getColor(int index, int defaultValue)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_NULL) return defaultValue;
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) return dataAt(index);

        int resourceId = mData[index * STYLE_NUM_ENTRIES + STYLE_RESOURCE_ID];
        if (type == TypedValue.TYPE_STRING && resourceId != 0)
        {
            ColorStateList colors = mResources.getColorStateList(resourceId, mTheme);
            return colors != null ? colors.getDefaultColor() : defaultValue;
        }

        return defaultValue;
    }

    public ColorStateList getColorStateList(int index)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_NULL) return null;
        if (type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT)
        {
            return ColorStateList.valueOf(dataAt(index));
        }

        int resourceId = mData[index * STYLE_NUM_ENTRIES + STYLE_RESOURCE_ID];
        if (resourceId != 0) return mResources.getColorStateList(resourceId, mTheme);

        runtime.Strict.unimplemented("TypedArray.getColorStateList type 0x" + Integer.toHexString(type));
        return null;
    }

    public float getDimension(int index, float defaultValue)
    {
        if (typeAt(index) != TypedValue.TYPE_DIMENSION) return defaultValue;
        return TypedValue.complexToDimension(dataAt(index), metrics());
    }

    public int getDimensionPixelOffset(int index, int defaultValue)
    {
        if (typeAt(index) != TypedValue.TYPE_DIMENSION) return defaultValue;
        return TypedValue.complexToDimensionPixelOffset(dataAt(index), metrics());
    }

    public int getDimensionPixelSize(int index, int defaultValue)
    {
        if (typeAt(index) != TypedValue.TYPE_DIMENSION) return defaultValue;
        return TypedValue.complexToDimensionPixelSize(dataAt(index), metrics());
    }

    public int getLayoutDimension(int index, String name)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_DIMENSION) return TypedValue.complexToDimensionPixelSize(dataAt(index), metrics());
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) return dataAt(index);

        throw new RuntimeException("You must supply a " + name + " attribute.");
    }

    public int getLayoutDimension(int index, int defaultValue)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_DIMENSION) return TypedValue.complexToDimensionPixelSize(dataAt(index), metrics());
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) return dataAt(index);
        return defaultValue;
    }

    public float getFraction(int index, int base, int pbase, float defaultValue)
    {
        if (typeAt(index) != TypedValue.TYPE_FRACTION) return defaultValue;
        return TypedValue.complexToFraction(dataAt(index), base, pbase);
    }

    public String getString(int index)
    {
        if (!inRange(index)) return null;
        if (mStrings != null && index < mStrings.length && mStrings[index] != null) return mStrings[index];

        int type = typeAt(index);
        if (type == TypedValue.TYPE_NULL) return null;

        if (type == TypedValue.TYPE_STRING)
        {
            int cookie = cookieAt(index);
            if (cookie < 0) return mParser == null ? null : mParser.getPooledString(dataAt(index));
            return mResources.getStringForCookie(dataAt(index), cookie);
        }

        int resourceId = mData[index * STYLE_NUM_ENTRIES + STYLE_RESOURCE_ID];
        if (resourceId != 0) return mResources.getStringOrNull(resourceId);

        return TypedValue.coerceToString(type, dataAt(index));
    }

    public String getNonResourceString(int index)
    {
        return getString(index);
    }

    public CharSequence getText(int index)
    {
        return getString(index);
    }

    public CharSequence[] getTextArray(int index)
    {
        String value = getString(index);
        return value == null ? new CharSequence[0] : new CharSequence[] {value};
    }

    public android.graphics.drawable.Drawable getDrawable(int index)
    {
        int type = typeAt(index);
        if (type == TypedValue.TYPE_NULL) return null;
        if (type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT)
        {
            return new android.graphics.drawable.ColorDrawable(dataAt(index));
        }

        int resourceId = mData[index * STYLE_NUM_ENTRIES + STYLE_RESOURCE_ID];
        return resourceId == 0 ? null : mResources.getDrawable(resourceId);
    }

    public android.graphics.Typeface getFont(int index)
    {
        int resourceId = inRange(index) ? mData[index * STYLE_NUM_ENTRIES + STYLE_RESOURCE_ID] : 0;
        if (resourceId == 0) return android.graphics.Typeface.DEFAULT;

        try
        {
            return mResources.getFont(resourceId);
        }
        catch (Resources.NotFoundException failure)
        {
            return android.graphics.Typeface.DEFAULT;
        }
    }

    public int getSourceResourceId(int index, int defaultValue)
    {
        if (!inRange(index)) return defaultValue;

        int resourceId = mData[index * STYLE_NUM_ENTRIES + STYLE_SOURCE_RESOURCE_ID];
        return resourceId != 0 ? resourceId : defaultValue;
    }

    public boolean getValue(int index, TypedValue outValue)
    {
        if (!inRange(index)) return false;

        int base = index * STYLE_NUM_ENTRIES;
        outValue.type = mData[base + STYLE_TYPE];
        outValue.data = mData[base + STYLE_DATA];
        outValue.assetCookie = mData[base + STYLE_ASSET_COOKIE];
        outValue.resourceId = mData[base + STYLE_RESOURCE_ID];
        outValue.changingConfigurations = mData[base + STYLE_CHANGING_CONFIGURATIONS];
        outValue.density = mData[base + STYLE_DENSITY];
        outValue.sourceResourceId = mData[base + STYLE_SOURCE_RESOURCE_ID];

        if (outValue.type == TypedValue.TYPE_STRING) outValue.string = getString(index);
        return outValue.type != TypedValue.TYPE_NULL;
    }

    public TypedValue peekValue(int index)
    {
        TypedValue value = new TypedValue();
        return getValue(index, value) ? value : null;
    }

    public int getChangingConfigurations()
    {
        return 0;
    }

    public String getPositionDescription()
    {
        return "<internal>";
    }

    public void recycle()
    {
        mRecycled = true;
    }

    @Override
    public void close()
    {
        recycle();
    }

    @Override
    public String toString()
    {
        return mData == null ? "TypedArray{}" : java.util.Arrays.toString(mData);
    }

    boolean isRecycled()
    {
        return mRecycled;
    }

    private DisplayMetrics metrics()
    {
        return mResources.getDisplayMetrics();
    }

    private int defaultTextColor()
    {
        return 0xFF000000;
    }

    private boolean inRange(int index)
    {
        return mData != null && index >= 0 && index < mLength;
    }

    private int typeAt(int index)
    {
        if (!inRange(index)) return TypedValue.TYPE_NULL;
        return mData[index * STYLE_NUM_ENTRIES + STYLE_TYPE];
    }

    private int dataAt(int index)
    {
        return mData[index * STYLE_NUM_ENTRIES + STYLE_DATA];
    }

    private int cookieAt(int index)
    {
        return mData[index * STYLE_NUM_ENTRIES + STYLE_ASSET_COOKIE];
    }
}
