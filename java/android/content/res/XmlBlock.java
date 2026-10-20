package android.content.res;

import android.util.TypedValue;
import org.xmlpull.v1.XmlPullParserException;

public final class XmlBlock
{
    private XmlBlock()
    {
    }

    public static final class Parser implements XmlResourceParser
    {
        private static final int ANDROID_ID = 0x010100d0;
        private static final int ANDROID_STYLE = 0x01010000;

        private final Resources mResources;
        private long mHandle;
        private int mEventType = START_DOCUMENT;

        Parser(Resources resources, long handle)
        {
            mResources = resources;
            mHandle = handle;
        }

        long getHandle()
        {
            return mHandle;
        }

        @Override
        public int next()
        {
            mEventType = nativeNext(mHandle);
            return mEventType;
        }

        @Override
        public int nextToken()
        {
            return next();
        }

        @Override
        public int nextTag()
        {
            int event = next();
            while (event == TEXT)
            {
                event = next();
            }
            return event;
        }

        @Override
        public String nextText()
        {
            return null;
        }

        @Override
        public void require(int type, String namespace, String name) throws XmlPullParserException
        {
            if (type != getEventType() || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName())))
            {
                throw new XmlPullParserException("expected " + TYPES[type] + " " + getPositionDescription());
            }
        }

        @Override
        public int getEventType()
        {
            return mEventType;
        }

        @Override
        public int getDepth()
        {
            return nativeDepth(mHandle);
        }

        @Override
        public String getName()
        {
            return nativeElementName(mHandle);
        }

        @Override
        public String getText()
        {
            return null;
        }

        @Override
        public char[] getTextCharacters(int[] holderForStartAndLength)
        {
            String text = getText();
            if (text == null) return null;

            char[] characters = text.toCharArray();
            if (holderForStartAndLength != null && holderForStartAndLength.length >= 2)
            {
                holderForStartAndLength[0] = 0;
                holderForStartAndLength[1] = characters.length;
            }
            return characters;
        }

        @Override
        public int getLineNumber()
        {
            return nativeLineNumber(mHandle);
        }

        @Override
        public int getColumnNumber()
        {
            return -1;
        }

        @Override
        public boolean isWhitespace()
        {
            return false;
        }

        @Override
        public boolean isEmptyElementTag()
        {
            return false;
        }

        @Override
        public String getNamespace()
        {
            return NO_NAMESPACE;
        }

        @Override
        public String getNamespace(String prefix)
        {
            return null;
        }

        @Override
        public int getNamespaceCount(int depth)
        {
            return 0;
        }

        @Override
        public String getNamespacePrefix(int pos)
        {
            return null;
        }

        @Override
        public String getNamespaceUri(int pos)
        {
            return null;
        }

        @Override
        public String getPrefix()
        {
            return null;
        }

        @Override
        public String getInputEncoding()
        {
            return null;
        }

        @Override
        public void setFeature(String name, boolean state) throws XmlPullParserException
        {
            throw new XmlPullParserException("setFeature is not supported on a compiled resource");
        }

        @Override
        public boolean getFeature(String name)
        {
            return false;
        }

        @Override
        public void setProperty(String name, Object value) throws XmlPullParserException
        {
            throw new XmlPullParserException("setProperty is not supported on a compiled resource");
        }

        @Override
        public Object getProperty(String name)
        {
            return null;
        }

        @Override
        public void setInput(java.io.Reader in) throws XmlPullParserException
        {
            throw new XmlPullParserException("setInput is not supported on a compiled resource");
        }

        @Override
        public void setInput(java.io.InputStream inputStream, String inputEncoding) throws XmlPullParserException
        {
            throw new XmlPullParserException("setInput is not supported on a compiled resource");
        }

        @Override
        public void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException
        {
            throw new XmlPullParserException("defineEntityReplacementText is not supported on a compiled resource");
        }

        @Override
        public void close()
        {
            if (mHandle == 0) return;

            nativeClose(mHandle);
            mHandle = 0;
        }

        @Override
        public int getAttributeCount()
        {
            return nativeAttributeCount(mHandle);
        }

        @Override
        public String getAttributeName(int index)
        {
            return nativeAttributeName(mHandle, index);
        }

        @Override
        public String getAttributeNamespace(int index)
        {
            return NO_NAMESPACE;
        }

        @Override
        public String getAttributePrefix(int index)
        {
            return null;
        }

        @Override
        public String getAttributeType(int index)
        {
            return "CDATA";
        }

        @Override
        public boolean isAttributeDefault(int index)
        {
            return false;
        }

        @Override
        public String getAttributeValue(int index)
        {
            return nativeAttributeStringValue(mHandle, index);
        }

        @Override
        public String getAttributeValue(String namespace, String name)
        {
            int index = nativeIndexOfName(mHandle, namespace, name);
            return index < 0 ? null : getAttributeValue(index);
        }

        @Override
        public String getPositionDescription()
        {
            return "Binary XML file line #" + getLineNumber();
        }

        @Override
        public int getAttributeNameResource(int index)
        {
            return nativeAttributeNameResource(mHandle, index);
        }

        public int[] getAttributeTypedValue(int index)
        {
            return nativeAttributeValue(mHandle, index);
        }

        public int[] getAttributeTypedValue(String namespace, String name)
        {
            int index = nativeIndexOfName(mHandle, namespace, name);
            return index < 0 ? null : nativeAttributeValue(mHandle, index);
        }

        @Override
        public int getAttributeListValue(int index, String[] options, int defaultValue)
        {
            return getAttributeIntValue(index, defaultValue);
        }

        @Override
        public boolean getAttributeBooleanValue(int index, boolean defaultValue)
        {
            int[] packed = nativeAttributeValue(mHandle, index);
            if (packed == null) return defaultValue;
            if (packed[0] < TypedValue.TYPE_FIRST_INT || packed[0] > TypedValue.TYPE_LAST_INT) return defaultValue;
            return packed[1] != 0;
        }

        @Override
        public int getAttributeResourceValue(int index, int defaultValue)
        {
            int[] packed = nativeAttributeValue(mHandle, index);
            if (packed == null) return defaultValue;
            if (packed[0] != TypedValue.TYPE_REFERENCE) return defaultValue;
            return packed[1];
        }

        @Override
        public int getAttributeIntValue(int index, int defaultValue)
        {
            int[] packed = nativeAttributeValue(mHandle, index);
            if (packed == null) return defaultValue;
            if (packed[0] < TypedValue.TYPE_FIRST_INT || packed[0] > TypedValue.TYPE_LAST_INT) return defaultValue;
            return packed[1];
        }

        @Override
        public int getAttributeUnsignedIntValue(int index, int defaultValue)
        {
            return getAttributeIntValue(index, defaultValue);
        }

        @Override
        public float getAttributeFloatValue(int index, float defaultValue)
        {
            int[] packed = nativeAttributeValue(mHandle, index);
            if (packed == null) return defaultValue;
            if (packed[0] != TypedValue.TYPE_FLOAT) return defaultValue;
            return Float.intBitsToFloat(packed[1]);
        }

        @Override
        public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue)
        {
            int index = nativeIndexOfName(mHandle, namespace, attribute);
            return index < 0 ? defaultValue : getAttributeListValue(index, options, defaultValue);
        }

        @Override
        public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue)
        {
            int index = nativeIndexOfName(mHandle, namespace, attribute);
            return index < 0 ? defaultValue : getAttributeBooleanValue(index, defaultValue);
        }

        @Override
        public int getAttributeResourceValue(String namespace, String attribute, int defaultValue)
        {
            int index = nativeIndexOfName(mHandle, namespace, attribute);
            return index < 0 ? defaultValue : getAttributeResourceValue(index, defaultValue);
        }

        @Override
        public int getAttributeIntValue(String namespace, String attribute, int defaultValue)
        {
            int index = nativeIndexOfName(mHandle, namespace, attribute);
            return index < 0 ? defaultValue : getAttributeIntValue(index, defaultValue);
        }

        @Override
        public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue)
        {
            return getAttributeIntValue(namespace, attribute, defaultValue);
        }

        @Override
        public float getAttributeFloatValue(String namespace, String attribute, float defaultValue)
        {
            int index = nativeIndexOfName(mHandle, namespace, attribute);
            return index < 0 ? defaultValue : getAttributeFloatValue(index, defaultValue);
        }

        @Override
        public String getIdAttribute()
        {
            int index = nativeIndexOfResource(mHandle, ANDROID_ID);
            return index < 0 ? null : getAttributeValue(index);
        }

        @Override
        public String getClassAttribute()
        {
            return getAttributeValue(null, "class");
        }

        @Override
        public int getIdAttributeResourceValue(int defaultValue)
        {
            int index = nativeIndexOfResource(mHandle, ANDROID_ID);
            return index < 0 ? defaultValue : getAttributeResourceValue(index, defaultValue);
        }

        @Override
        public int getStyleAttribute()
        {
            int index = nativeIndexOfResource(mHandle, ANDROID_STYLE);
            return index < 0 ? 0 : getAttributeResourceValue(index, 0);
        }

        public String getPooledString(int stringIndex)
        {
            return nativePooledString(mHandle, stringIndex);
        }

        private static native int nativeNext(long handle);

        private static native int nativeDepth(long handle);

        private static native void nativeClose(long handle);

        private static native String nativeElementName(long handle);

        private static native int nativeLineNumber(long handle);

        private static native int nativeAttributeCount(long handle);

        private static native String nativeAttributeName(long handle, int index);

        private static native String nativeAttributeStringValue(long handle, int index);

        private static native int nativeAttributeNameResource(long handle, int index);

        private static native int[] nativeAttributeValue(long handle, int index);

        private static native int nativeIndexOfName(long handle, String namespace, String name);

        private static native int nativeIndexOfResource(long handle, int resourceId);

        private static native String nativePooledString(long handle, int stringIndex);
    }
}
