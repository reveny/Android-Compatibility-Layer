package android.opengl;

import android.graphics.Bitmap;
import android.util.Half;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;

public final class GLUtils
{
    private static final int GL_RGBA16F = 0x881A;
    private static final int GL_HALF_FLOAT = 0x140B;

    GLUtils()
    {
    }

    public static int getInternalFormat(Bitmap bitmap)
    {
        checkBitmap(bitmap);

        switch (bitmap.getConfig())
        {
            case ALPHA_8:
                return GLES20.GL_ALPHA;
            case ARGB_4444:
            case ARGB_8888:
                return GLES20.GL_RGBA;
            case RGB_565:
                return GLES20.GL_RGB;
            case RGBA_F16:
                return GL_RGBA16F;
            default:
                return -1;
        }
    }

    public static int getType(Bitmap bitmap)
    {
        checkBitmap(bitmap);

        switch (bitmap.getConfig())
        {
            case ALPHA_8:
            case ARGB_8888:
                return GLES20.GL_UNSIGNED_BYTE;
            case ARGB_4444:
                return GLES20.GL_UNSIGNED_SHORT_4_4_4_4;
            case RGB_565:
                return GLES20.GL_UNSIGNED_SHORT_5_6_5;
            case RGBA_F16:
                return GL_HALF_FLOAT;
            default:
                return -1;
        }
    }

    public static void texImage2D(int target, int level, int internalformat, Bitmap bitmap, int border)
    {
        checkBitmap(bitmap);
        int format = baseFormat(bitmap);
        int type = getType(bitmap);
        if (internalformat < 0) internalformat = getInternalFormat(bitmap);
        upload(target, level, internalformat, bitmap, format, type, border);
    }

    public static void texImage2D(int target, int level, int internalformat, Bitmap bitmap, int type, int border)
    {
        checkBitmap(bitmap);
        int format = baseFormat(bitmap);
        if (internalformat < 0) internalformat = getInternalFormat(bitmap);
        upload(target, level, internalformat, bitmap, format, type, border);
    }

    public static void texImage2D(int target, int level, Bitmap bitmap, int border)
    {
        checkBitmap(bitmap);
        upload(target, level, getInternalFormat(bitmap), bitmap, baseFormat(bitmap), getType(bitmap), border);
    }

    public static void texSubImage2D(int target, int level, int xoffset, int yoffset, Bitmap bitmap)
    {
        checkBitmap(bitmap);
        int format = baseFormat(bitmap);
        int type = getType(bitmap);
        ByteBuffer pixels = pixels(bitmap, type);
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, bitmap.getWidth(), bitmap.getHeight(), format, type, pixels);
    }

    public static void texSubImage2D(int target, int level, int xoffset, int yoffset, Bitmap bitmap, int format, int type)
    {
        checkBitmap(bitmap);
        ByteBuffer pixels = pixels(bitmap, type);
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, bitmap.getWidth(), bitmap.getHeight(), format, type, pixels);
    }

    public static String getEGLErrorString(int error)
    {
        switch (error)
        {
            case EGL10.EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL10.EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL10.EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL10.EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL10.EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL10.EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL10.EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL10.EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL10.EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL10.EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL10.EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL10.EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL10.EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL10.EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL11.EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default:
                return "0x" + Integer.toHexString(error);
        }
    }

    private static void checkBitmap(Bitmap bitmap)
    {
        if (bitmap == null) throw new NullPointerException("texImage2D can't be used with a null Bitmap");
        if (bitmap.isRecycled()) throw new IllegalArgumentException("bitmap is recycled");
        if (bitmap.getConfig() == Bitmap.Config.HARDWARE)
        {
            throw new IllegalArgumentException("hardware bitmaps cannot be used with texImage2D");
        }
    }

    private static int baseFormat(Bitmap bitmap)
    {
        switch (bitmap.getConfig())
        {
            case ALPHA_8:
                return GLES20.GL_ALPHA;
            case RGB_565:
                return GLES20.GL_RGB;
            default:
                return GLES20.GL_RGBA;
        }
    }

    private static void upload(int target, int level, int internalformat, Bitmap bitmap, int format, int type, int border)
    {
        if (internalformat < 0 || type < 0) throw new IllegalArgumentException("invalid Bitmap format");

        ByteBuffer pixels = pixels(bitmap, type);
        GLES20.glTexImage2D(target, level, internalformat, bitmap.getWidth(), bitmap.getHeight(), border, format, type, pixels);
    }

    private static ByteBuffer pixels(Bitmap bitmap, int type)
    {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] colors = new int[width * height];
        bitmap.getPixels(colors, 0, width, 0, 0, width, height);

        boolean alphaOnly = bitmap.getConfig() == Bitmap.Config.ALPHA_8;
        boolean premultiply = bitmap.isPremultiplied();
        int bytesPerPixel = alphaOnly ? 1 : bytesPerPixel(type);
        ByteBuffer buffer = ByteBuffer.allocateDirect(colors.length * bytesPerPixel).order(ByteOrder.nativeOrder());

        for (int color : colors)
        {
            int alpha = color >>> 24;
            int red = (color >> 16) & 0xFF;
            int green = (color >> 8) & 0xFF;
            int blue = color & 0xFF;
            if (premultiply && alpha != 0xFF)
            {
                red = red * alpha / 255;
                green = green * alpha / 255;
                blue = blue * alpha / 255;
            }

            if (alphaOnly)
            {
                buffer.put((byte) alpha);
                continue;
            }

            switch (type)
            {
                case GLES20.GL_UNSIGNED_SHORT_5_6_5:
                    buffer.putShort((short) (((red >> 3) << 11) | ((green >> 2) << 5) | (blue >> 3)));
                    break;
                case GLES20.GL_UNSIGNED_SHORT_4_4_4_4:
                    buffer.putShort((short) (((red >> 4) << 12) | ((green >> 4) << 8) | ((blue >> 4) << 4) | (alpha >> 4)));
                    break;
                case GLES20.GL_UNSIGNED_SHORT_5_5_5_1:
                    buffer.putShort((short) (((red >> 3) << 11) | ((green >> 3) << 6) | ((blue >> 3) << 1) | (alpha >> 7)));
                    break;
                case GL_HALF_FLOAT:
                    buffer.putShort(Half.toHalf(red / 255.0f));
                    buffer.putShort(Half.toHalf(green / 255.0f));
                    buffer.putShort(Half.toHalf(blue / 255.0f));
                    buffer.putShort(Half.toHalf(alpha / 255.0f));
                    break;
                default:
                    buffer.put((byte) red);
                    buffer.put((byte) green);
                    buffer.put((byte) blue);
                    buffer.put((byte) alpha);
                    break;
            }
        }

        buffer.rewind();
        return buffer;
    }

    private static int bytesPerPixel(int type)
    {
        switch (type)
        {
            case GLES20.GL_UNSIGNED_SHORT_5_6_5:
            case GLES20.GL_UNSIGNED_SHORT_4_4_4_4:
            case GLES20.GL_UNSIGNED_SHORT_5_5_5_1:
                return 2;
            case GL_HALF_FLOAT:
                return 8;
            default:
                return 4;
        }
    }
}
