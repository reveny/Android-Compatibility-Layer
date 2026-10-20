package android.content.res;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Resources
{
    private final long mAssetsHandle;
    private final Configuration mConfiguration = new Configuration();
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    private final AssetManager mAssets;
    private final java.util.ArrayList<Theme> mLiveThemes = new java.util.ArrayList<>();

    private static Resources sSystem;

    public Resources(long assetsHandle)
    {
        mAssetsHandle = assetsHandle;
        mAssets = new AssetManager(this, assetsHandle);

        if (sSystem == null && assetsHandle != 0) sSystem = this;
    }

    public static Resources getSystem()
    {
        if (sSystem == null) sSystem = new Resources(0);

        return sSystem;
    }

    public AssetManager getAssets()
    {
        return mAssets;
    }

    public Configuration getConfiguration()
    {
        return mConfiguration;
    }

    public DisplayMetrics getDisplayMetrics()
    {
        return mMetrics;
    }

    public void updateMetrics(int widthPixels, int heightPixels, int densityDpi)
    {
        mMetrics.widthPixels = widthPixels;
        mMetrics.heightPixels = heightPixels;
        mMetrics.densityDpi = densityDpi;
        mMetrics.density = densityDpi / (float)DisplayMetrics.DENSITY_DEFAULT;
        mMetrics.scaledDensity = mMetrics.density;
        mMetrics.xdpi = densityDpi;
        mMetrics.ydpi = densityDpi;

        mConfiguration.densityDpi = densityDpi;
        mConfiguration.screenWidthDp = (int)(widthPixels / mMetrics.density);
        mConfiguration.screenHeightDp = (int)(heightPixels / mMetrics.density);
        mConfiguration.orientation =
            widthPixels > heightPixels ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
    }

    public void updateConfiguration(Configuration configuration, DisplayMetrics metrics)
    {
        if (configuration != null) mConfiguration.setTo(configuration);
        if (metrics != null) mMetrics.setTo(metrics);
    }

    public Resources withConfiguration(Configuration overrideConfiguration)
    {
        Resources copy = new Resources(mAssetsHandle);
        copy.mMetrics.setTo(mMetrics);
        copy.mConfiguration.setTo(mConfiguration);
        if (overrideConfiguration != null) copy.mConfiguration.setTo(overrideConfiguration);
        return copy;
    }

    public Theme newTheme()
    {
        Theme theme = new Theme(this);
        mLiveThemes.add(theme);
        return theme;
    }

    public void releaseThemes()
    {
        for (Theme theme : mLiveThemes)
        {
            theme.destroy();
        }
        mLiveThemes.clear();
    }

    public String getString(int resourceId) throws NotFoundException
    {
        String value = nativeGetString(mAssetsHandle, resourceId);
        if (value == null) throw new NotFoundException("String resource ID #0x" + Integer.toHexString(resourceId));
        return value;
    }

    public String getString(int resourceId, Object... formatArgs) throws NotFoundException
    {
        return String.format(getString(resourceId), formatArgs);
    }

    public CharSequence getText(int resourceId) throws NotFoundException
    {
        return getString(resourceId);
    }

    String getStringOrNull(int resourceId)
    {
        return nativeGetString(mAssetsHandle, resourceId);
    }

    String getStringForCookie(int stringIndex, int cookie)
    {
        return nativeGetPooledString(mAssetsHandle, cookie, stringIndex);
    }

    public int getIdentifier(String name, String defType, String defPackage)
    {
        return nativeGetIdentifier(mAssetsHandle, name, defType, defPackage);
    }

    public String getResourceName(int resourceId) throws NotFoundException
    {
        String name = nativeGetResourceName(mAssetsHandle, resourceId);
        if (name == null) throw new NotFoundException("Resource ID #0x" + Integer.toHexString(resourceId));
        return name;
    }

    public String getResourcePackageName(int resourceId) throws NotFoundException
    {
        String name = getResourceName(resourceId);
        int colon = name.indexOf(':');

        return colon < 0 ? name : name.substring(0, colon);
    }

    public String getResourceTypeName(int resourceId) throws NotFoundException
    {
        String name = getResourceName(resourceId);
        int colon = name.indexOf(':');
        int slash = name.indexOf('/');

        if (slash < 0) return "";

        return name.substring(colon + 1, slash);
    }

    public String getResourceEntryName(int resourceId) throws NotFoundException
    {
        String name = getResourceName(resourceId);
        int slash = name.indexOf('/');

        return slash < 0 ? name : name.substring(slash + 1);
    }

    public int getColor(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Color 0x" + Integer.toHexString(resourceId));
        return value.data;
    }

    public int getColor(int resourceId, Theme theme) throws NotFoundException
    {
        return getColor(resourceId);
    }

    public ColorStateList getColorStateList(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Color state list 0x" + Integer.toHexString(resourceId));

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT)
        {
            return ColorStateList.valueOf(value.data);
        }

        runtime.Strict.unimplemented("Resources.getColorStateList selector 0x" + Integer.toHexString(resourceId));
        return null;
    }

    public ColorStateList getColorStateList(int resourceId, Theme theme) throws NotFoundException
    {
        return getColorStateList(resourceId);
    }

    public int getInteger(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Integer 0x" + Integer.toHexString(resourceId));
        return value.data;
    }

    public boolean getBoolean(int resourceId) throws NotFoundException
    {
        return getInteger(resourceId) != 0;
    }

    public float getFloat(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Float 0x" + Integer.toHexString(resourceId));

        if (value.type == TypedValue.TYPE_FLOAT) return Float.intBitsToFloat(value.data);

        return value.data;
    }

    public float getDimension(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Dimension 0x" + Integer.toHexString(resourceId));
        return TypedValue.complexToDimension(value.data, mMetrics);
    }

    public int getDimensionPixelSize(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Dimension 0x" + Integer.toHexString(resourceId));
        return TypedValue.complexToDimensionPixelSize(value.data, mMetrics);
    }

    public int getDimensionPixelOffset(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Dimension 0x" + Integer.toHexString(resourceId));
        return TypedValue.complexToDimensionPixelOffset(value.data, mMetrics);
    }

    public Drawable getDrawable(int resourceId)
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) return null;

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT)
        {
            return new ColorDrawable(value.data);
        }

        String path = value.string == null ? null : value.string.toString();

        if (path != null && path.endsWith(".xml"))
        {
            XmlResourceParser parser = null;

            try
            {
                parser = getXml(resourceId);
                return Drawable.createFromXml(this, parser);
            }
            catch (Exception failure)
            {
                android.util.Log.w("Resources", "cannot inflate drawable 0x" + Integer.toHexString(resourceId) + ": "
                                                + failure);
                return null;
            }
            finally
            {
                if (parser != null) parser.close();
            }
        }

        if (path != null)
        {
            android.graphics.Bitmap bitmap = getBitmap(resourceId);
            if (bitmap != null) return new android.graphics.drawable.BitmapDrawable(this, bitmap);
        }

        runtime.Strict.unimplemented("Resources.getDrawable type 0x" + Integer.toHexString(value.type)
                                     + " for 0x" + Integer.toHexString(resourceId));
        return null;
    }

    public java.io.InputStream openRawResource(int resourceId) throws NotFoundException
    {
        return openRawResource(resourceId, new TypedValue());
    }

    public java.io.InputStream openRawResource(int resourceId, TypedValue value) throws NotFoundException
    {
        if (!fetchValue(resourceId, value) || value.string == null)
        {
            throw new NotFoundException("no raw resource 0x" + Integer.toHexString(resourceId));
        }

        String path = value.string.toString();

        try
        {
            return getAssets().openNonAsset(path);
        }
        catch (java.io.IOException failure)
        {
            throw new NotFoundException("cannot read " + path + ": " + failure);
        }
    }

    public AssetFileDescriptor openRawResourceFd(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();

        if (!fetchValue(resourceId, value) || value.string == null)
        {
            throw new NotFoundException("no raw resource 0x" + Integer.toHexString(resourceId));
        }

        try
        {
            return getAssets().openNonAssetFd(value.string.toString());
        }
        catch (java.io.IOException failure)
        {
            throw new NotFoundException("cannot open " + value.string + ": " + failure);
        }
    }

    public android.graphics.Bitmap getBitmap(int resourceId)
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value) || value.string == null) return null;

        String path = value.string.toString();

        try (java.io.InputStream stream = getAssets().openNonAsset(path))
        {
            return android.graphics.BitmapFactory.decodeStream(stream);
        }
        catch (Exception failure)
        {
            android.util.Log.w("Resources", "cannot read image " + path + ": " + failure);
            return null;
        }
    }

    public Drawable getDrawable(int resourceId, Theme theme)
    {
        return getDrawable(resourceId);
    }

    public void getValue(int resourceId, TypedValue outValue, boolean resolveRefs) throws NotFoundException
    {
        if (fetchValue(resourceId, outValue)) return;
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(resourceId));
    }

    public void getValue(String name, TypedValue outValue, boolean resolveRefs) throws NotFoundException
    {
        getValue(getIdentifier(name, null, null), outValue, resolveRefs);
    }

    public void getValueForDensity(int resourceId, int density, TypedValue outValue, boolean resolveRefs) throws NotFoundException
    {
        getValue(resourceId, outValue, resolveRefs);
    }

    private boolean fetchValue(int resourceId, TypedValue outValue)
    {
        int[] packed = nativeGetResourceValue(mAssetsHandle, resourceId);
        if (packed == null) return false;

        outValue.type = packed[0];
        outValue.data = packed[1];
        outValue.resourceId = packed[2];
        if (outValue.type == TypedValue.TYPE_STRING) outValue.string = getStringOrNull(resourceId);
        return true;
    }

    public String[] getStringArray(int resourceId) throws NotFoundException
    {
        CharSequence[] text = getTextArray(resourceId);
        String[] values = new String[text.length];

        for (int index = 0; index < text.length; index++) values[index] = text[index] == null ? null : text[index].toString();

        return values;
    }

    public CharSequence[] getTextArray(int resourceId) throws NotFoundException
    {
        int count = nativeGetArraySize(mAssetsHandle, resourceId);
        if (count < 0) throw new NotFoundException("Array 0x" + Integer.toHexString(resourceId));

        CharSequence[] values = new CharSequence[count];

        for (int index = 0; index < count; index++)
        {
            int[] packed = nativeGetArrayEntry(mAssetsHandle, resourceId, index);
            if (packed == null) continue;

            if (packed[0] == TypedValue.TYPE_STRING && packed[2] != 0) values[index] = getStringOrNull(packed[2]);
            else if (packed[0] == TypedValue.TYPE_STRING) values[index] = nativeGetPooledString(mAssetsHandle, 0, packed[1]);
            else values[index] = Integer.toString(packed[1]);
        }

        return values;
    }

    public int[] getIntArray(int resourceId) throws NotFoundException
    {
        int count = nativeGetArraySize(mAssetsHandle, resourceId);
        if (count < 0) throw new NotFoundException("Array 0x" + Integer.toHexString(resourceId));

        int[] values = new int[count];

        for (int index = 0; index < count; index++)
        {
            int[] packed = nativeGetArrayEntry(mAssetsHandle, resourceId, index);
            if (packed != null) values[index] = packed[1];
        }

        return values;
    }

    private static native int nativeGetArraySize(long assetsHandle, int resourceId);

    private static native int[] nativeGetArrayEntry(long assetsHandle, int resourceId, int index);

    public XmlResourceParser getLayout(int resourceId) throws NotFoundException
    {
        long handle = nativeOpenXmlResource(mAssetsHandle, resourceId);
        if (handle == 0) throw new NotFoundException("Layout 0x" + Integer.toHexString(resourceId));
        return new XmlBlock.Parser(this, handle);
    }

    public XmlResourceParser getXml(int resourceId) throws NotFoundException
    {
        return getLayout(resourceId);
    }

    public TypedArray obtainAttributes(AttributeSet set, int[] attributes)
    {
        TypedArray array = new TypedArray(this);
        XmlBlock.Parser parser = set instanceof XmlBlock.Parser ? (XmlBlock.Parser)set : null;
        long xmlHandle = parser == null ? 0 : parser.getHandle();

        int[] data = new int[attributes.length * TypedArray.STYLE_NUM_ENTRIES];
        int[] indices = new int[attributes.length + 1];

        nativeObtainNoTheme(mAssetsHandle, xmlHandle, attributes, data, indices);
        array.bind(attributes, data, indices, parser);
        return array;
    }

    private static native String nativeGetString(long assetsHandle, int resourceId);

    private static native String nativeGetPooledString(long assetsHandle, int cookie, int stringIndex);

    private static native String nativeGetResourceName(long assetsHandle, int resourceId);

    private static native int nativeGetIdentifier(long assetsHandle, String name, String defType, String defPackage);

    private static native int[] nativeGetResourceValue(long assetsHandle, int resourceId);

    private static native long nativeOpenXmlResource(long assetsHandle, int resourceId);

    private static native void nativeObtainNoTheme(long assetsHandle, long xmlHandle, int[] attributes, int[] outValues, int[] outIndices);

    public static class NotFoundException extends RuntimeException
    {
        public NotFoundException()
        {
        }

        public NotFoundException(String message)
        {
            super(message);
        }
    }

    public final class Theme
    {
        private final Resources mOwner;
        private final java.util.ArrayList<int[]> mAppliedStyles = new java.util.ArrayList<>();
        private long mHandle;

        Theme(Resources owner)
        {
            mOwner = owner;
            mHandle = nativeCreateTheme(owner.mAssetsHandle);
        }

        public Resources getResources()
        {
            return mOwner;
        }

        long getHandle()
        {
            return mHandle;
        }

        public void applyStyle(int resourceId, boolean force)
        {
            if (mHandle == 0) return;
            nativeApplyStyle(mHandle, resourceId, force);
            mAppliedStyles.add(new int[] {resourceId, force ? 1 : 0});
        }

        public void setTo(Theme other)
        {
            if (other == null || mHandle == 0 || other.mHandle == 0) return;
            nativeSetTo(mHandle, other.mHandle);

            mAppliedStyles.clear();
            mAppliedStyles.addAll(other.mAppliedStyles);
        }

        public void rebase()
        {
            if (mHandle == 0) return;

            nativeClearTheme(mHandle);
            for (int[] applied : mAppliedStyles)
            {
                nativeApplyStyle(mHandle, applied[0], applied[1] != 0);
            }
        }

        public void release()
        {
            mOwner.mLiveThemes.remove(this);
            destroy();
        }

        void destroy()
        {
            if (mHandle == 0) return;

            nativeDestroyTheme(mHandle);
            mHandle = 0;
            mAppliedStyles.clear();
        }

        public boolean resolveAttribute(int attributeId, TypedValue outValue, boolean resolveRefs)
        {
            if (mHandle == 0) return false;

            int[] packed = nativeResolveAttribute(mHandle, attributeId);
            if (packed == null) return false;

            outValue.type = packed[0];
            outValue.data = packed[1];
            outValue.resourceId = packed[2];
            if (outValue.type == TypedValue.TYPE_STRING) outValue.string = mOwner.getStringOrNull(outValue.resourceId);
            return true;
        }

        public TypedArray obtainStyledAttributes(int[] attributes)
        {
            return obtainStyledAttributes(null, attributes, 0, 0);
        }

        public TypedArray obtainStyledAttributes(int resourceId, int[] attributes)
        {
            return obtainStyledAttributes(null, attributes, 0, resourceId);
        }

        public TypedArray obtainStyledAttributes(AttributeSet set, int[] attributes, int defStyleAttr, int defStyleRes)
        {
            TypedArray array = new TypedArray(mOwner);

            int[] data = new int[attributes.length * TypedArray.STYLE_NUM_ENTRIES];
            int[] indices = new int[attributes.length + 1];

            XmlBlock.Parser parser = set instanceof XmlBlock.Parser ? (XmlBlock.Parser)set : null;
            long xmlHandle = parser == null ? 0 : parser.getHandle();
            nativeObtainStyled(mHandle, xmlHandle, defStyleAttr, defStyleRes, attributes, data, indices);

            array.bind(attributes, data, indices, parser);
            return array;
        }
    }

    private static native long nativeCreateTheme(long assetsHandle);

    private static native void nativeApplyStyle(long themeHandle, int resourceId, boolean force);

    private static native void nativeSetTo(long targetHandle, long sourceHandle);

    static native long nativeAssetOpen(long assetsHandle, String path);

    static native long nativeAssetOpenNonAsset(long assetsHandle, String path);

    static native java.io.FileDescriptor nativeAssetOpenFd(long assetHandle, long[] outDescriptorOffsetLength);

    static native long nativeAssetLength(long assetHandle);

    static native int nativeAssetRead(long assetHandle, byte[] buffer, int offset, int count);

    static native void nativeAssetClose(long assetHandle);

    static native String[] nativeAssetList(long assetsHandle, String directory);

    private static native void nativeClearTheme(long themeHandle);

    private static native void nativeDestroyTheme(long themeHandle);

    private static native int[] nativeResolveAttribute(long themeHandle, int attributeId);

    private static native void nativeObtainStyled(long themeHandle, long xmlHandle, int defStyleAttr, int defStyleRes, int[] attributes,
                                                  int[] outValues, int[] outIndices);
}
