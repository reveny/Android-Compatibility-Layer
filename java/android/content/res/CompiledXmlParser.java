package android.content.res;

import android.util.TypedValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;

final class CompiledXmlParser implements XmlResourceParser
{
    private static final int CHUNK_STRING_POOL = 0x0001;
    private static final int CHUNK_XML = 0x0003;
    private static final int CHUNK_START_NAMESPACE = 0x0100;
    private static final int CHUNK_END_NAMESPACE = 0x0101;
    private static final int CHUNK_START_ELEMENT = 0x0102;
    private static final int CHUNK_END_ELEMENT = 0x0103;
    private static final int CHUNK_CDATA = 0x0104;
    private static final int CHUNK_RESOURCE_MAP = 0x0180;
    private static final int UTF8_FLAG = 1 << 8;
    private static final int NO_INDEX = -1;
    private static final int ATTRIBUTE_ENTRY_SIZE = 20;

    private final ByteBuffer mBuffer;
    private final ArrayList<String> mPrefixes = new ArrayList<>();
    private final ArrayList<String> mUris = new ArrayList<>();
    private final ArrayList<Integer> mNamespaceCounts = new ArrayList<>();
    private String[] mStrings = new String[0];
    private int[] mResourceIds = new int[0];
    private int mCursor;
    private int mEnd;
    private int mEventType = START_DOCUMENT;
    private int mDepth;
    private int mLineNumber = -1;
    private boolean mPendingEndDepth;
    private boolean mClosed;

    private int mNamespaceIndex = NO_INDEX;
    private int mNameIndex = NO_INDEX;
    private String mText;
    private int mAttributeOffset;
    private int mAttributeSize;
    private int mAttributeCount;
    private int mIdIndex;
    private int mClassIndex;
    private int mStyleIndex;

    CompiledXmlParser(byte[] data) throws IOException
    {
        mBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        if (data.length < 8 || (mBuffer.getShort(0) & 0xffff) != CHUNK_XML) throw new IOException("not a compiled XML file");

        int headerSize = mBuffer.getShort(2) & 0xffff;
        mEnd = Math.min(data.length, mBuffer.getInt(4));
        mCursor = headerSize;

        while (mCursor + 8 <= mEnd)
        {
            int type = mBuffer.getShort(mCursor) & 0xffff;
            int size = mBuffer.getInt(mCursor + 4);
            if (size < 8) throw new IOException("corrupt compiled XML chunk");

            if (type == CHUNK_STRING_POOL)
            {
                readStringPool(mCursor);
            }
            else if (type == CHUNK_RESOURCE_MAP)
            {
                int chunkHeader = mBuffer.getShort(mCursor + 2) & 0xffff;
                int count = (size - chunkHeader) / 4;
                mResourceIds = new int[count];
                for (int index = 0; index < count; index++) mResourceIds[index] = mBuffer.getInt(mCursor + chunkHeader + index * 4);
            }
            else if (type >= CHUNK_START_NAMESPACE && type <= CHUNK_CDATA)
            {
                break;
            }

            mCursor += size;
        }

        mNamespaceCounts.add(0);
    }

    static CompiledXmlParser fromStream(InputStream stream) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] chunk = new byte[16384];
        int got;

        while ((got = stream.read(chunk)) > 0) bytes.write(chunk, 0, got);

        return new CompiledXmlParser(bytes.toByteArray());
    }

    private void readStringPool(int start)
    {
        int headerSize = mBuffer.getShort(start + 2) & 0xffff;
        int stringCount = mBuffer.getInt(start + 8);
        int flags = mBuffer.getInt(start + 16);
        int stringsStart = mBuffer.getInt(start + 20);
        boolean utf8 = (flags & UTF8_FLAG) != 0;

        mStrings = new String[stringCount];

        for (int index = 0; index < stringCount; index++)
        {
            int offset = start + stringsStart + mBuffer.getInt(start + headerSize + index * 4);
            mStrings[index] = utf8 ? readUtf8(offset) : readUtf16(offset);
        }
    }

    private String readUtf8(int offset)
    {
        int position = offset;
        int first = mBuffer.get(position++) & 0xff;
        if ((first & 0x80) != 0) position++;

        int length = mBuffer.get(position++) & 0xff;
        if ((length & 0x80) != 0) length = ((length & 0x7f) << 8) | (mBuffer.get(position++) & 0xff);

        return new String(mBuffer.array(), position, length, StandardCharsets.UTF_8);
    }

    private String readUtf16(int offset)
    {
        int position = offset;
        int length = mBuffer.getShort(position) & 0xffff;
        position += 2;

        if ((length & 0x8000) != 0)
        {
            length = ((length & 0x7fff) << 16) | (mBuffer.getShort(position) & 0xffff);
            position += 2;
        }

        char[] characters = new char[length];
        for (int index = 0; index < length; index++) characters[index] = mBuffer.getChar(position + index * 2);
        return new String(characters);
    }

    private String stringAt(int index)
    {
        if (index < 0 || index >= mStrings.length) return null;
        return mStrings[index];
    }

    @Override
    public int next() throws XmlPullParserException
    {
        if (mClosed) throw new XmlPullParserException("the parser is closed");

        if (mPendingEndDepth)
        {
            mDepth--;
            mNamespaceCounts.remove(mNamespaceCounts.size() - 1);
            mPendingEndDepth = false;
        }

        if (mEventType == END_DOCUMENT) return END_DOCUMENT;

        while (mCursor + 8 <= mEnd)
        {
            int chunk = mCursor;
            int type = mBuffer.getShort(chunk) & 0xffff;
            int headerSize = mBuffer.getShort(chunk + 2) & 0xffff;
            int size = mBuffer.getInt(chunk + 4);
            if (size < 8) break;

            mCursor += size;

            if (type < CHUNK_START_NAMESPACE || type > CHUNK_CDATA) continue;

            mLineNumber = mBuffer.getInt(chunk + 8);
            int extension = chunk + headerSize;

            if (type == CHUNK_START_NAMESPACE)
            {
                mPrefixes.add(stringAt(mBuffer.getInt(extension)));
                mUris.add(stringAt(mBuffer.getInt(extension + 4)));
                continue;
            }

            if (type == CHUNK_END_NAMESPACE) continue;

            if (type == CHUNK_START_ELEMENT)
            {
                mNamespaceIndex = mBuffer.getInt(extension);
                mNameIndex = mBuffer.getInt(extension + 4);
                mAttributeOffset = extension + (mBuffer.getShort(extension + 8) & 0xffff);
                mAttributeSize = mBuffer.getShort(extension + 10) & 0xffff;
                if (mAttributeSize == 0) mAttributeSize = ATTRIBUTE_ENTRY_SIZE;
                mAttributeCount = mBuffer.getShort(extension + 12) & 0xffff;
                mIdIndex = mBuffer.getShort(extension + 14) & 0xffff;
                mClassIndex = mBuffer.getShort(extension + 16) & 0xffff;
                mStyleIndex = mBuffer.getShort(extension + 18) & 0xffff;
                mText = null;
                mDepth++;
                mNamespaceCounts.add(mPrefixes.size());
                mEventType = START_TAG;
                return mEventType;
            }

            if (type == CHUNK_END_ELEMENT)
            {
                mNamespaceIndex = mBuffer.getInt(extension);
                mNameIndex = mBuffer.getInt(extension + 4);
                mAttributeCount = 0;
                mText = null;
                mPendingEndDepth = true;
                mEventType = END_TAG;
                return mEventType;
            }

            mText = stringAt(mBuffer.getInt(extension));
            mAttributeCount = 0;
            mEventType = TEXT;
            return mEventType;
        }

        mEventType = END_DOCUMENT;
        return mEventType;
    }

    @Override
    public int nextToken() throws XmlPullParserException
    {
        return next();
    }

    @Override
    public int nextTag() throws XmlPullParserException
    {
        int event = next();
        if (event == TEXT && isWhitespace()) event = next();
        if (event != START_TAG && event != END_TAG) throw new XmlPullParserException("expected start or end tag " + getPositionDescription());
        return event;
    }

    @Override
    public String nextText() throws XmlPullParserException
    {
        if (getEventType() != START_TAG) throw new XmlPullParserException("parser must be on START_TAG to read next text");

        int event = next();
        if (event == TEXT)
        {
            String result = getText();
            event = next();
            if (event != END_TAG) throw new XmlPullParserException("event TEXT must be immediately followed by END_TAG");
            return result;
        }
        if (event == END_TAG) return "";

        throw new XmlPullParserException("parser must be on START_TAG or TEXT to read text");
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
        return mDepth;
    }

    @Override
    public String getName()
    {
        if (mEventType != START_TAG && mEventType != END_TAG) return null;
        return stringAt(mNameIndex);
    }

    @Override
    public String getText()
    {
        return mEventType == TEXT ? mText : null;
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
        return mLineNumber;
    }

    @Override
    public int getColumnNumber()
    {
        return -1;
    }

    @Override
    public boolean isWhitespace()
    {
        return mText != null && mText.trim().isEmpty();
    }

    @Override
    public boolean isEmptyElementTag()
    {
        return false;
    }

    @Override
    public String getNamespace()
    {
        String namespace = stringAt(mNamespaceIndex);
        return namespace == null ? NO_NAMESPACE : namespace;
    }

    @Override
    public String getNamespace(String prefix)
    {
        for (int index = mPrefixes.size() - 1; index >= 0; index--)
        {
            if (java.util.Objects.equals(prefix, mPrefixes.get(index))) return mUris.get(index);
        }
        return null;
    }

    @Override
    public int getNamespaceCount(int depth)
    {
        if (depth < 0 || depth >= mNamespaceCounts.size()) return mPrefixes.size();
        return mNamespaceCounts.get(depth);
    }

    @Override
    public String getNamespacePrefix(int position)
    {
        return position >= 0 && position < mPrefixes.size() ? mPrefixes.get(position) : null;
    }

    @Override
    public String getNamespaceUri(int position)
    {
        return position >= 0 && position < mUris.size() ? mUris.get(position) : null;
    }

    @Override
    public String getPrefix()
    {
        String namespace = stringAt(mNamespaceIndex);
        if (namespace == null) return null;

        for (int index = mUris.size() - 1; index >= 0; index--)
        {
            if (namespace.equals(mUris.get(index))) return mPrefixes.get(index);
        }
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
        return FEATURE_PROCESS_NAMESPACES.equals(name) || FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name);
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
    public void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException
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
        mClosed = true;
    }

    private int attributeBase(int index)
    {
        if (index < 0 || index >= mAttributeCount) throw new IndexOutOfBoundsException("attribute " + index);
        return mAttributeOffset + index * mAttributeSize;
    }

    private int attributeNameIndex(int index)
    {
        return mBuffer.getInt(attributeBase(index) + 4);
    }

    private int attributeType(int index)
    {
        return mBuffer.get(attributeBase(index) + 15) & 0xff;
    }

    private int attributeData(int index)
    {
        return mBuffer.getInt(attributeBase(index) + 16);
    }

    private int indexOfAttribute(String namespace, String name)
    {
        if (name == null) return -1;

        for (int index = 0; index < mAttributeCount; index++)
        {
            if (!name.equals(getAttributeName(index))) continue;
            if (namespace == null || namespace.equals(getAttributeNamespace(index))) return index;
        }
        return -1;
    }

    private int indexOfResource(int resourceId)
    {
        for (int index = 0; index < mAttributeCount; index++)
        {
            if (getAttributeNameResource(index) == resourceId) return index;
        }
        return -1;
    }

    @Override
    public int getAttributeCount()
    {
        return mEventType == START_TAG ? mAttributeCount : -1;
    }

    @Override
    public String getAttributeName(int index)
    {
        return stringAt(attributeNameIndex(index));
    }

    @Override
    public String getAttributeNamespace(int index)
    {
        String namespace = stringAt(mBuffer.getInt(attributeBase(index)));
        return namespace == null ? NO_NAMESPACE : namespace;
    }

    @Override
    public String getAttributePrefix(int index)
    {
        String namespace = stringAt(mBuffer.getInt(attributeBase(index)));
        if (namespace == null) return null;

        for (int position = mUris.size() - 1; position >= 0; position--)
        {
            if (namespace.equals(mUris.get(position))) return mPrefixes.get(position);
        }
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
        int raw = mBuffer.getInt(attributeBase(index) + 8);
        if (raw >= 0) return stringAt(raw);

        int type = attributeType(index);
        if (type == TypedValue.TYPE_STRING) return stringAt(attributeData(index));
        return TypedValue.coerceToString(type, attributeData(index));
    }

    @Override
    public String getAttributeValue(String namespace, String name)
    {
        int index = indexOfAttribute(namespace, name);
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
        int nameIndex = attributeNameIndex(index);
        return nameIndex >= 0 && nameIndex < mResourceIds.length ? mResourceIds[nameIndex] : 0;
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue)
    {
        int type = attributeType(index);
        if (type == TypedValue.TYPE_STRING)
        {
            String value = stringAt(attributeData(index));
            if (options != null && value != null)
            {
                for (int position = 0; position < options.length; position++)
                {
                    if (value.equals(options[position])) return position;
                }
            }
            return defaultValue;
        }
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) return attributeData(index);
        return defaultValue;
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue)
    {
        int type = attributeType(index);
        if (type < TypedValue.TYPE_FIRST_INT || type > TypedValue.TYPE_LAST_INT) return defaultValue;
        return attributeData(index) != 0;
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue)
    {
        if (attributeType(index) != TypedValue.TYPE_REFERENCE) return defaultValue;
        return attributeData(index);
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue)
    {
        int type = attributeType(index);
        if (type < TypedValue.TYPE_FIRST_INT || type > TypedValue.TYPE_LAST_INT) return defaultValue;
        return attributeData(index);
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue)
    {
        return getAttributeIntValue(index, defaultValue);
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue)
    {
        if (attributeType(index) != TypedValue.TYPE_FLOAT) return defaultValue;
        return Float.intBitsToFloat(attributeData(index));
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue)
    {
        int index = indexOfAttribute(namespace, attribute);
        return index < 0 ? defaultValue : getAttributeListValue(index, options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue)
    {
        int index = indexOfAttribute(namespace, attribute);
        return index < 0 ? defaultValue : getAttributeBooleanValue(index, defaultValue);
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue)
    {
        int index = indexOfAttribute(namespace, attribute);
        return index < 0 ? defaultValue : getAttributeResourceValue(index, defaultValue);
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue)
    {
        int index = indexOfAttribute(namespace, attribute);
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
        int index = indexOfAttribute(namespace, attribute);
        return index < 0 ? defaultValue : getAttributeFloatValue(index, defaultValue);
    }

    @Override
    public String getIdAttribute()
    {
        return mIdIndex > 0 && mIdIndex <= mAttributeCount ? getAttributeValue(mIdIndex - 1) : null;
    }

    @Override
    public String getClassAttribute()
    {
        return mClassIndex > 0 && mClassIndex <= mAttributeCount ? getAttributeValue(mClassIndex - 1) : null;
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue)
    {
        return mIdIndex > 0 && mIdIndex <= mAttributeCount ? getAttributeResourceValue(mIdIndex - 1, defaultValue) : defaultValue;
    }

    @Override
    public int getStyleAttribute()
    {
        if (mStyleIndex > 0 && mStyleIndex <= mAttributeCount) return attributeData(mStyleIndex - 1);

        int index = indexOfResource(0x01010000);
        return index < 0 ? 0 : getAttributeResourceValue(index, 0);
    }
}
