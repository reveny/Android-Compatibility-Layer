package org.xmlpull.v1;

import java.util.ArrayList;
import java.util.HashMap;

public class XmlPullParserFactory
{
    public static final String PROPERTY_NAME = "org.xmlpull.v1.XmlPullParserFactory";

    protected ArrayList parserClasses;
    protected ArrayList serializerClasses;
    protected String classNamesLocation;
    protected HashMap<String, Boolean> features = new HashMap<String, Boolean>();

    protected XmlPullParserFactory()
    {
    }

    public static XmlPullParserFactory newInstance() throws XmlPullParserException
    {
        return new XmlPullParserFactory();
    }

    public static XmlPullParserFactory newInstance(String classNames, Class context) throws XmlPullParserException
    {
        return newInstance();
    }

    public XmlPullParser newPullParser() throws XmlPullParserException
    {
        throw new XmlPullParserException("no pull parser is available in this runtime");
    }

    public void setNamespaceAware(boolean awareness)
    {
        features.put(XmlPullParser.FEATURE_PROCESS_NAMESPACES, Boolean.valueOf(awareness));
    }

    public boolean isNamespaceAware()
    {
        return getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
    }

    public void setValidating(boolean validating)
    {
        features.put(XmlPullParser.FEATURE_VALIDATION, Boolean.valueOf(validating));
    }

    public boolean isValidating()
    {
        return getFeature(XmlPullParser.FEATURE_VALIDATION);
    }

    public void setFeature(String name, boolean state) throws XmlPullParserException
    {
        features.put(name, Boolean.valueOf(state));
    }

    public boolean getFeature(String name)
    {
        return Boolean.TRUE.equals(features.get(name));
    }
}
