package org.xmlpull.v1;

public class XmlPullParserException extends Exception
{
    protected Throwable detail;
    protected int row = -1;
    protected int column = -1;

    public XmlPullParserException(String message)
    {
        super(message);
    }

    public XmlPullParserException(String message, XmlPullParser parser, Throwable chain)
    {
        super((message == null ? "" : message + " ")
              + (parser == null ? "" : "(position:" + parser.getPositionDescription() + ") ")
              + (chain == null ? "" : "caused by: " + chain));

        if (parser != null)
        {
            row = parser.getLineNumber();
            column = parser.getColumnNumber();
        }
        detail = chain;
    }

    public Throwable getDetail()
    {
        return detail;
    }

    public int getLineNumber()
    {
        return row;
    }

    public int getColumnNumber()
    {
        return column;
    }
}
