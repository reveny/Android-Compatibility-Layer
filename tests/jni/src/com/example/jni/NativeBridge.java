package com.example.jni;

public final class NativeBridge
{
    public int seed;

    public NativeBridge(int seed)
    {
        this.seed = seed;
    }

    public static int doubled(int value)
    {
        return value * 2;
    }

    public int scaled(int value)
    {
        return value * seed;
    }

    public static String decorate(String value)
    {
        return "[" + value + "]";
    }

    public static void refuse(int value) throws IllegalStateException
    {
        throw new IllegalStateException("refused " + value);
    }

    public static native int registeredUpcall(int value);

    public static native int manglingUpcall(int value);

    public static native String stringRoundTrip(String value);

    public static native int instanceUpcall(NativeBridge target, int value);

    public static native int mutateArray(byte[] buffer);

    public static native int bumpField(NativeBridge target, int delta);

    public static native String catchJavaException(int value);

    public static native int throwFromNative(int value);

    public static native int attachedThreadUpcall(int value);
}
