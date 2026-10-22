package android.content.res;

import android.content.res.loader.ResourcesLoader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Resources
{
    private final long mAssetsHandle;
    private final Configuration mConfiguration = new Configuration();
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    private final AssetManager mAssets;
    private final java.util.ArrayList<Theme> mLiveThemes = new java.util.ArrayList<>();
    private final java.util.ArrayList<ResourcesLoader> mLoaders = new java.util.ArrayList<>();
    private ResourcesImpl mResourcesImpl;
    private ClassLoader mClassLoader = ClassLoader.getSystemClassLoader();

    public static final int ID_NULL = 0;
    private static final int ANDROID_NAME = 0x01010003;
    private static final int ANDROID_VALUE = 0x01010024;
    private static final int ANDROID_FONT = 0x01010532;
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    private static Resources sSystem;

    public Resources(long assetsHandle)
    {
        mAssetsHandle = assetsHandle;
        mAssets = new AssetManager(this, assetsHandle);
        mResourcesImpl = new ResourcesImpl(mAssets, mMetrics, mConfiguration);

        if (sSystem == null && assetsHandle != 0) sSystem = this;
    }

    public Resources(AssetManager assets, DisplayMetrics metrics, Configuration config)
    {
        mAssets = assets == null ? new AssetManager(this, 0) : assets;
        mAssetsHandle = mAssets.getHandle();
        mResourcesImpl = new ResourcesImpl(mAssets, mMetrics, mConfiguration);

        if (config != null) mConfiguration.setTo(config);
        if (metrics != null) mMetrics.setTo(metrics);
    }

    ResourcesImpl getImpl()
    {
        return mResourcesImpl;
    }

    public ClassLoader getClassLoader()
    {
        return mClassLoader;
    }

    void setImpl(ResourcesImpl impl)
    {
        if (impl == null || impl == mResourcesImpl) return;

        mConfiguration.setTo(impl.getConfiguration());
        mMetrics.setTo(impl.getDisplayMetrics());
        mResourcesImpl = impl;
    }

    public static int getAttributeSetSourceResId(AttributeSet set)
    {
        return ID_NULL;
    }

    public final void flushLayoutCache()
    {
    }

    public final void finishPreloading()
    {
    }

    public void addLoaders(ResourcesLoader... loaders)
    {
        if (loaders == null) return;

        synchronized (mLoaders)
        {
            for (ResourcesLoader loader : loaders)
            {
                if (loader != null && !mLoaders.contains(loader)) mLoaders.add(loader);
            }
        }
    }

    public void removeLoaders(ResourcesLoader... loaders)
    {
        if (loaders == null) return;

        synchronized (mLoaders)
        {
            for (ResourcesLoader loader : loaders) mLoaders.remove(loader);
        }
    }

    java.util.List<ResourcesLoader> getLoaders()
    {
        synchronized (mLoaders)
        {
            return new java.util.ArrayList<>(mLoaders);
        }
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

    public final Theme newTheme()
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
        return getText(resourceId).toString();
    }

    public String getString(int resourceId, Object... formatArgs) throws NotFoundException
    {
        return String.format(getString(resourceId), formatArgs);
    }

    public CharSequence getText(int resourceId) throws NotFoundException
    {
        String value = nativeGetString(mAssetsHandle, resourceId);
        if (value == null) throw new NotFoundException("String resource ID #0x" + Integer.toHexString(resourceId));
        return value;
    }

    public CharSequence getText(int resourceId, CharSequence defaultValue)
    {
        String value = resourceId != 0 ? nativeGetString(mAssetsHandle, resourceId) : null;
        return value != null ? value : defaultValue;
    }

    public CharSequence getQuantityText(int resourceId, int quantity) throws NotFoundException
    {
        int count = nativeGetArraySize(mAssetsHandle, resourceId);
        if (count <= 0) throw new NotFoundException("Plural resource ID #0x" + Integer.toHexString(resourceId));

        java.util.Locale locale = mConfiguration.getLocales().isEmpty() ? mConfiguration.locale : mConfiguration.getLocales().get(0);
        int index = QuantityRules.entryIndex(locale, quantity, count);
        int[] packed = nativeGetArrayEntry(mAssetsHandle, resourceId, index);
        if (packed == null && index != 0) packed = nativeGetArrayEntry(mAssetsHandle, resourceId, 0);
        if (packed == null)
        {
            throw new NotFoundException("Plural resource ID #0x" + Integer.toHexString(resourceId) + " quantity=" + quantity);
        }

        String value;
        if (packed[0] == TypedValue.TYPE_STRING && packed[2] != 0) value = getStringOrNull(packed[2]);
        else if (packed[0] == TypedValue.TYPE_STRING) value = nativeGetPooledString(mAssetsHandle, 0, packed[1]);
        else value = TypedValue.coerceToString(packed[0], packed[1]);

        if (value == null)
        {
            throw new NotFoundException("Plural resource ID #0x" + Integer.toHexString(resourceId) + " quantity=" + quantity);
        }
        return value;
    }

    public String getQuantityString(int resourceId, int quantity, Object... formatArgs) throws NotFoundException
    {
        String raw = getQuantityText(resourceId, quantity).toString();
        return String.format(mConfiguration.locale == null ? java.util.Locale.getDefault() : mConfiguration.locale, raw, formatArgs);
    }

    public String getQuantityString(int resourceId, int quantity) throws NotFoundException
    {
        return getQuantityText(resourceId, quantity).toString();
    }

    public TypedArray obtainTypedArray(int resourceId) throws NotFoundException
    {
        int count = nativeGetArraySize(mAssetsHandle, resourceId);
        if (count < 0) throw new NotFoundException("Array resource ID #0x" + Integer.toHexString(resourceId));

        int[] attributes = new int[count];
        int[] data = new int[count * TypedArray.STYLE_NUM_ENTRIES];
        int[] indices = new int[count + 1];
        String[] strings = new String[count];

        for (int index = 0; index < count; index++)
        {
            int[] packed = nativeGetArrayEntry(mAssetsHandle, resourceId, index);
            if (packed == null) continue;

            int base = index * TypedArray.STYLE_NUM_ENTRIES;
            data[base + TypedArray.STYLE_TYPE] = packed[0];
            data[base + TypedArray.STYLE_DATA] = packed[1];
            data[base + TypedArray.STYLE_RESOURCE_ID] = packed[2];

            if (packed[0] == TypedValue.TYPE_STRING)
            {
                strings[index] = packed[2] != 0 ? getStringOrNull(packed[2]) : nativeGetPooledString(mAssetsHandle, 0, packed[1]);
            }

            if (packed[0] != TypedValue.TYPE_NULL) indices[++indices[0]] = index;
        }

        TypedArray array = new TypedArray(this);
        array.bind(attributes, data, indices, null);
        array.bindStrings(strings);
        return array;
    }

    String getStringOrNull(int resourceId)
    {
        return nativeGetString(mAssetsHandle, resourceId);
    }

    String getStringForCookie(int stringIndex, int cookie)
    {
        return nativeGetPooledString(mAssetsHandle, cookie - 1, stringIndex);
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
        if (isColorStateListFile(value)) return getColorStateList(resourceId, null).getDefaultColor();
        return value.data;
    }

    public int getColor(int resourceId, Theme theme) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Color 0x" + Integer.toHexString(resourceId));
        if (isColorStateListFile(value)) return getColorStateList(resourceId, theme).getDefaultColor();
        return value.data;
    }

    public ColorStateList getColorStateList(int resourceId) throws NotFoundException
    {
        return getColorStateList(resourceId, null);
    }

    public ColorStateList getColorStateList(int resourceId, Theme theme) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value)) throw new NotFoundException("Color state list 0x" + Integer.toHexString(resourceId));

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT)
        {
            return ColorStateList.valueOf(value.data);
        }

        String path = value.string == null ? null : value.string.toString();
        if (path == null || !path.endsWith(".xml"))
        {
            throw new NotFoundException("Resource 0x" + Integer.toHexString(resourceId) + " is not a color or color state list");
        }

        XmlResourceParser parser = null;
        try
        {
            parser = getXml(resourceId);
            return ColorStateList.createFromXml(this, parser, theme);
        }
        catch (org.xmlpull.v1.XmlPullParserException | java.io.IOException failure)
        {
            NotFoundException missing = new NotFoundException("File " + path + " from color state list resource ID #0x" +
                                                              Integer.toHexString(resourceId));
            missing.initCause(failure);
            throw missing;
        }
        finally
        {
            if (parser != null) parser.close();
        }
    }

    private boolean isColorStateListFile(TypedValue value)
    {
        return value.type == TypedValue.TYPE_STRING && value.string != null && value.string.toString().endsWith(".xml");
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

        if (path != null && path.endsWith(".9.png"))
        {
            try (java.io.InputStream stream = getAssets().openNonAsset(path))
            {
                Drawable patch = Drawable.createFromResourceStream(this, value, stream, path);
                if (patch != null) return patch;
            }
            catch (Exception failure)
            {
                android.util.Log.w("Resources", "cannot read nine-patch " + path + ": " + failure);
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

    public Drawable getDrawableForDensity(int resourceId, int density) throws NotFoundException
    {
        return getDrawableForDensity(resourceId, density, null);
    }

    public Drawable getDrawableForDensity(int resourceId, int density, Theme theme)
    {
        return getDrawable(resourceId, theme);
    }

    public float getFraction(int resourceId, int base, int pbase)
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value) || value.type != TypedValue.TYPE_FRACTION)
        {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(resourceId) + " type #0x"
                                        + Integer.toHexString(value.type) + " is not valid");
        }
        return TypedValue.complexToFraction(value.data, base, pbase);
    }

    public android.graphics.Movie getMovie(int resourceId) throws NotFoundException
    {
        java.io.InputStream stream = openRawResource(resourceId);

        try
        {
            return android.graphics.Movie.decodeStream(stream);
        }
        finally
        {
            try
            {
                stream.close();
            }
            catch (java.io.IOException failure)
            {
                android.util.Log.w("Resources", "cannot close movie stream: " + failure);
            }
        }
    }

    public android.graphics.Typeface getFont(int resourceId) throws NotFoundException
    {
        TypedValue value = new TypedValue();
        if (!fetchValue(resourceId, value) || value.string == null)
        {
            throw new NotFoundException("Font resource ID #0x" + Integer.toHexString(resourceId));
        }

        String path = value.string.toString();

        if (!path.endsWith(".xml"))
        {
            android.graphics.Typeface typeface = new android.graphics.Typeface.Builder(getAssets(), path, false, 0).build();
            if (typeface != null) return typeface;
            throw new NotFoundException("Font resource ID #0x" + Integer.toHexString(resourceId) + " could not be retrieved.");
        }

        int fontResource = 0;
        XmlResourceParser parser = null;

        try
        {
            parser = getXml(resourceId);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (type != XmlPullParser.START_TAG || !"font".equals(parser.getName())) continue;

                int candidate = 0;
                for (int index = 0; index < parser.getAttributeCount(); index++)
                {
                    if (parser.getAttributeNameResource(index) == ANDROID_FONT || "font".equals(parser.getAttributeName(index)))
                    {
                        candidate = parser.getAttributeResourceValue(index, 0);
                        if (candidate != 0) break;
                    }
                }

                if (candidate == 0) continue;

                String weight = parser.getAttributeValue(ANDROID_NAMESPACE, "fontWeight");
                String style = parser.getAttributeValue(ANDROID_NAMESPACE, "fontStyle");
                boolean regular = (weight == null || "400".equals(weight)) && (style == null || "0".equals(style) || "normal".equals(style));
                if (fontResource == 0 || regular) fontResource = candidate;
                if (regular) break;
            }
        }
        catch (XmlPullParserException | java.io.IOException failure)
        {
            NotFoundException missing = new NotFoundException("Font resource ID #0x" + Integer.toHexString(resourceId));
            missing.initCause(failure);
            throw missing;
        }
        finally
        {
            if (parser != null) parser.close();
        }

        if (fontResource == 0 || fontResource == resourceId)
        {
            throw new NotFoundException("Font resource ID #0x" + Integer.toHexString(resourceId) + " could not be retrieved.");
        }

        return getFont(fontResource);
    }

    public void parseBundleExtras(XmlResourceParser parser, Bundle outBundle) throws XmlPullParserException, java.io.IOException
    {
        int outerDepth = parser.getDepth();
        int type;

        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth))
        {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) continue;

            if ("extra".equals(parser.getName())) parseBundleExtra("extra", parser, outBundle);

            skipCurrentTag(parser);
        }
    }

    private static void skipCurrentTag(XmlPullParser parser) throws XmlPullParserException, java.io.IOException
    {
        int outerDepth = parser.getDepth();
        int type;

        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth))
        {
        }
    }

    public void parseBundleExtra(String tagName, AttributeSet attrs, Bundle outBundle) throws XmlPullParserException
    {
        TypedArray array = obtainAttributes(attrs, new int[] {ANDROID_NAME, ANDROID_VALUE});

        try
        {
            String name = array.getString(0);
            if (name == null) name = attrs.getAttributeValue(ANDROID_NAMESPACE, "name");
            if (name == null)
            {
                throw new XmlPullParserException("<" + tagName + "> requires an android:name attribute at " + attrs.getPositionDescription());
            }

            TypedValue value = array.peekValue(1);
            if (value == null)
            {
                throw new XmlPullParserException("<" + tagName + "> requires an android:value or android:resource attribute at "
                                                 + attrs.getPositionDescription());
            }

            if (value.type == TypedValue.TYPE_STRING)
            {
                outBundle.putCharSequence(name, value.string);
            }
            else if (value.type == TypedValue.TYPE_INT_BOOLEAN)
            {
                outBundle.putBoolean(name, value.data != 0);
            }
            else if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT)
            {
                outBundle.putInt(name, value.data);
            }
            else if (value.type == TypedValue.TYPE_FLOAT)
            {
                outBundle.putFloat(name, value.getFloat());
            }
            else
            {
                throw new XmlPullParserException("<" + tagName + "> only supports string, integer, float, color, and boolean at "
                                                 + attrs.getPositionDescription());
            }
        }
        finally
        {
            array.recycle();
        }
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

    XmlResourceParser openXmlBuffer(byte[] data)
    {
        long handle = nativeOpenXmlBuffer(mAssetsHandle, data);
        return handle == 0 ? null : new XmlBlock.Parser(this, handle);
    }

    public XmlResourceParser getXml(int resourceId) throws NotFoundException
    {
        return getLayout(resourceId);
    }

    public XmlResourceParser getAnimation(int resourceId) throws NotFoundException
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

    private static native long nativeOpenXmlBuffer(long assetsHandle, byte[] data);

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

        public NotFoundException(String message, Exception cause)
        {
            super(message, cause);
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

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof Theme)) return false;

            Theme that = (Theme)other;
            if (mOwner != that.mOwner || mAppliedStyles.size() != that.mAppliedStyles.size()) return false;

            for (int index = 0; index < mAppliedStyles.size(); index++)
            {
                if (!java.util.Arrays.equals(mAppliedStyles.get(index), that.mAppliedStyles.get(index))) return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = System.identityHashCode(mOwner);
            for (int[] style : mAppliedStyles) hash = 31 * hash + java.util.Arrays.hashCode(style);

            return hash;
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
            TypedArray array = new TypedArray(mOwner, this);

            int[] data = new int[attributes.length * TypedArray.STYLE_NUM_ENTRIES];
            int[] indices = new int[attributes.length + 1];

            XmlBlock.Parser parser = set instanceof XmlBlock.Parser ? (XmlBlock.Parser)set : null;
            long xmlHandle = parser == null ? 0 : parser.getHandle();
            nativeObtainStyled(mHandle, xmlHandle, defStyleAttr, defStyleRes, attributes, data, indices);

            array.bind(attributes, data, indices, parser);
            return array;
        }

        public Drawable getDrawable(int resourceId) throws NotFoundException
        {
            return mOwner.getDrawable(resourceId, this);
        }

        public int getChangingConfigurations()
        {
            return 0;
        }

        public void dump(int priority, String tag, String prefix)
        {
            android.util.Log.println(priority, tag, prefix + toString());
        }

        public int getExplicitStyle(AttributeSet set)
        {
            return set == null ? ID_NULL : set.getStyleAttribute();
        }

        public int[] getAttributeResolutionStack(int defStyleAttr, int defStyleRes, int explicitStyleRes)
        {
            return new int[0];
        }

        @Override
        public String toString()
        {
            StringBuilder styles = new StringBuilder();
            for (int[] applied : mAppliedStyles)
            {
                if (styles.length() > 0) styles.append(", ");
                styles.append("0x").append(Integer.toHexString(applied[0]));
            }
            return "Theme{" + styles + "}";
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
