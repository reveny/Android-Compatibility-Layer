package android.text.method;

public class SingleLineTransformationMethod extends ReplacementTransformationMethod
{
    private static final char[] ORIGINAL = {'\n', '\r'};
    private static final char[] REPLACEMENT = {' ', '﻿'};

    private static SingleLineTransformationMethod sInstance;

    public SingleLineTransformationMethod()
    {
    }

    @Override
    protected char[] getOriginal()
    {
        return ORIGINAL;
    }

    @Override
    protected char[] getReplacement()
    {
        return REPLACEMENT;
    }

    public static SingleLineTransformationMethod getInstance()
    {
        if (sInstance == null) sInstance = new SingleLineTransformationMethod();

        return sInstance;
    }
}
