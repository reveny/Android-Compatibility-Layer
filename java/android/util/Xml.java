package android.util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class Xml
{
    public enum Encoding
    {
        US_ASCII("US-ASCII"),
        UTF_8("UTF-8"),
        UTF_16("UTF-16"),
        ISO_8859_1("ISO-8859-1");

        private final String mName;

        Encoding(String name)
        {
            mName = name;
        }

        public String expectedName()
        {
            return mName;
        }
    }

    public static String FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";

    Xml()
    {
    }

    public static XmlPullParser newPullParser()
    {
        try
        {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            return factory.newPullParser();
        }
        catch (XmlPullParserException failure)
        {
            throw new AssertionError(failure);
        }
    }

    public static XmlSerializer newSerializer()
    {
        try
        {
            return (XmlSerializer)Class.forName("com.android.org.kxml2.io.KXmlSerializer").newInstance();
        }
        catch (ClassNotFoundException failure)
        {
            throw new AssertionError(failure);
        }
        catch (InstantiationException failure)
        {
            throw new AssertionError(failure);
        }
        catch (IllegalAccessException failure)
        {
            throw new AssertionError(failure);
        }
    }

    public static AttributeSet asAttributeSet(XmlPullParser parser)
    {
        if (parser instanceof AttributeSet) return (AttributeSet)parser;

        return new ParserAttributeSet(parser);
    }

    public static Encoding findEncodingByName(String name) throws java.io.UnsupportedEncodingException
    {
        if (name == null) return Encoding.UTF_8;

        for (Encoding encoding : Encoding.values())
        {
            if (encoding.expectedName().equalsIgnoreCase(name)) return encoding;
        }

        throw new java.io.UnsupportedEncodingException(name);
    }

    public static void parse(String xml, org.xml.sax.ContentHandler contentHandler) throws org.xml.sax.SAXException
    {
        try
        {
            parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)), contentHandler);
        }
        catch (java.io.IOException failure)
        {
            throw new AssertionError(failure);
        }
    }

    public static void parse(java.io.Reader in, org.xml.sax.ContentHandler contentHandler) throws java.io.IOException,
                                                                                               org.xml.sax.SAXException
    {
        parse(new org.xml.sax.InputSource(in), contentHandler);
    }

    public static void parse(java.io.InputStream in, Encoding encoding, org.xml.sax.ContentHandler contentHandler)
        throws java.io.IOException, org.xml.sax.SAXException
    {
        org.xml.sax.InputSource source = new org.xml.sax.InputSource(in);
        source.setEncoding(encoding == null ? Encoding.UTF_8.expectedName() : encoding.expectedName());
        parse(source, contentHandler);
    }

    private static void parse(org.xml.sax.InputSource source, org.xml.sax.ContentHandler contentHandler)
        throws java.io.IOException, org.xml.sax.SAXException
    {
        try
        {
            javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);

            org.xml.sax.XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setContentHandler(contentHandler);
            reader.parse(source);
        }
        catch (javax.xml.parsers.ParserConfigurationException failure)
        {
            throw new org.xml.sax.SAXException(failure);
        }
    }

    private static final class ParserAttributeSet implements AttributeSet
    {
        private final XmlPullParser mParser;

        ParserAttributeSet(XmlPullParser parser)
        {
            mParser = parser;
        }

        @Override
        public int getAttributeCount()
        {
            return mParser.getAttributeCount();
        }

        @Override
        public String getAttributeName(int index)
        {
            return mParser.getAttributeName(index);
        }

        @Override
        public String getAttributeValue(int index)
        {
            return mParser.getAttributeValue(index);
        }

        @Override
        public String getAttributeValue(String namespace, String name)
        {
            return mParser.getAttributeValue(namespace, name);
        }

        @Override
        public String getPositionDescription()
        {
            return mParser.getPositionDescription();
        }

        @Override
        public int getAttributeNameResource(int index)
        {
            return 0;
        }

        @Override
        public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue)
        {
            return indexOf(options, getAttributeValue(namespace, attribute), defaultValue);
        }

        @Override
        public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue)
        {
            return booleanOf(getAttributeValue(namespace, attribute), defaultValue);
        }

        @Override
        public int getAttributeResourceValue(String namespace, String attribute, int defaultValue)
        {
            return integerOf(getAttributeValue(namespace, attribute), defaultValue);
        }

        @Override
        public int getAttributeIntValue(String namespace, String attribute, int defaultValue)
        {
            return integerOf(getAttributeValue(namespace, attribute), defaultValue);
        }

        @Override
        public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue)
        {
            return integerOf(getAttributeValue(namespace, attribute), defaultValue);
        }

        @Override
        public float getAttributeFloatValue(String namespace, String attribute, float defaultValue)
        {
            return floatOf(getAttributeValue(namespace, attribute), defaultValue);
        }

        @Override
        public int getAttributeListValue(int index, String[] options, int defaultValue)
        {
            return indexOf(options, getAttributeValue(index), defaultValue);
        }

        @Override
        public boolean getAttributeBooleanValue(int index, boolean defaultValue)
        {
            return booleanOf(getAttributeValue(index), defaultValue);
        }

        @Override
        public int getAttributeResourceValue(int index, int defaultValue)
        {
            return integerOf(getAttributeValue(index), defaultValue);
        }

        @Override
        public int getAttributeIntValue(int index, int defaultValue)
        {
            return integerOf(getAttributeValue(index), defaultValue);
        }

        @Override
        public int getAttributeUnsignedIntValue(int index, int defaultValue)
        {
            return integerOf(getAttributeValue(index), defaultValue);
        }

        @Override
        public float getAttributeFloatValue(int index, float defaultValue)
        {
            return floatOf(getAttributeValue(index), defaultValue);
        }

        @Override
        public String getIdAttribute()
        {
            return getAttributeValue(null, "id");
        }

        @Override
        public String getClassAttribute()
        {
            return getAttributeValue(null, "class");
        }

        @Override
        public int getIdAttributeResourceValue(int defaultValue)
        {
            return integerOf(getIdAttribute(), defaultValue);
        }

        @Override
        public int getStyleAttribute()
        {
            return 0;
        }

        private static int indexOf(String[] options, String value, int defaultValue)
        {
            if (options == null || value == null) return defaultValue;

            for (int index = 0; index < options.length; index++)
            {
                if (value.equals(options[index])) return index;
            }

            return defaultValue;
        }

        private static boolean booleanOf(String value, boolean defaultValue)
        {
            if (value == null) return defaultValue;

            return "true".equalsIgnoreCase(value) || "1".equals(value);
        }

        private static int integerOf(String value, int defaultValue)
        {
            if (value == null) return defaultValue;

            try
            {
                if (value.startsWith("@")) return Integer.parseInt(value.substring(1));
                if (value.startsWith("0x") || value.startsWith("0X")) return (int)Long.parseLong(value.substring(2), 16);

                return Integer.parseInt(value);
            }
            catch (NumberFormatException failure)
            {
                return defaultValue;
            }
        }

        private static float floatOf(String value, float defaultValue)
        {
            if (value == null) return defaultValue;

            try
            {
                return Float.parseFloat(value);
            }
            catch (NumberFormatException failure)
            {
                return defaultValue;
            }
        }
    }
}
