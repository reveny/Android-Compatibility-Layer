package android.text.method;

public class HideReturnsTransformationMethod extends ReplacementTransformationMethod
{
    private static final char[] ORIGINAL = {'\r'};
    private static final char[] REPLACEMENT = {'﻿'};

    private static HideReturnsTransformationMethod sInstance;

    public HideReturnsTransformationMethod()
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

    public static HideReturnsTransformationMethod getInstance()
    {
        if (sInstance == null) sInstance = new HideReturnsTransformationMethod();

        return sInstance;
    }
}
