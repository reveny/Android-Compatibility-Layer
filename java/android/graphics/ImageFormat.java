package android.graphics;

public class ImageFormat
{
    public static final int UNKNOWN = 0;
    public static final int RGB_565 = 4;
    public static final int YV12 = 0x32315659;
    public static final int Y8 = 0x20203859;
    public static final int Y16 = 0x20363159;
    public static final int YCBCR_P010 = 0x36;
    public static final int YCBCR_P210 = 0x3C;
    public static final int NV16 = 0x10;
    public static final int NV21 = 0x11;
    public static final int YUY2 = 0x14;
    public static final int JPEG = 0x100;
    public static final int DEPTH_JPEG = 0x69656963;
    public static final int JPEG_R = 0x1005;
    public static final int HEIC_ULTRAHDR = 0x1006;
    public static final int YUV_420_888 = 0x23;
    public static final int YUV_422_888 = 0x27;
    public static final int YUV_444_888 = 0x28;
    public static final int FLEX_RGB_888 = 0x29;
    public static final int FLEX_RGBA_8888 = 0x2A;
    public static final int RAW_SENSOR = 0x20;
    public static final int RAW_PRIVATE = 0x24;
    public static final int RAW10 = 0x25;
    public static final int RAW12 = 0x26;
    public static final int RAW_DEPTH = 0x1002;
    public static final int RAW_DEPTH10 = 0x1003;
    public static final int DEPTH16 = 0x44363159;
    public static final int DEPTH_POINT_CLOUD = 0x101;
    public static final int PRIVATE = 0x22;
    public static final int HEIC = 0x48454946;

    public ImageFormat()
    {
    }

    public static int getBitsPerPixel(int format)
    {
        switch (format)
        {
            case RGB_565:
                return 16;
            case NV16:
                return 16;
            case YUY2:
                return 16;
            case YV12:
                return 12;
            case Y8:
                return 8;
            case Y16:
            case DEPTH16:
                return 16;
            case NV21:
                return 12;
            case YUV_420_888:
                return 12;
            case YUV_422_888:
                return 16;
            case YUV_444_888:
                return 24;
            case FLEX_RGB_888:
                return 24;
            case FLEX_RGBA_8888:
                return 32;
            case RAW_DEPTH:
            case RAW_SENSOR:
                return 16;
            case YCBCR_P010:
                return 24;
            case YCBCR_P210:
                return 32;
            case RAW_DEPTH10:
            case RAW10:
                return 10;
            case RAW12:
                return 12;
            default:
                return -1;
        }
    }

    public static boolean isPublicFormat(int format)
    {
        switch (format)
        {
            case RGB_565:
            case NV16:
            case YUY2:
            case YV12:
            case JPEG:
            case NV21:
            case YUV_420_888:
            case YUV_422_888:
            case YUV_444_888:
            case YCBCR_P010:
            case YCBCR_P210:
            case FLEX_RGB_888:
            case FLEX_RGBA_8888:
            case RAW_SENSOR:
            case RAW_PRIVATE:
            case RAW10:
            case RAW12:
            case DEPTH16:
            case DEPTH_POINT_CLOUD:
            case PRIVATE:
            case RAW_DEPTH:
            case RAW_DEPTH10:
            case Y8:
            case DEPTH_JPEG:
            case HEIC:
            case JPEG_R:
            case HEIC_ULTRAHDR:
                return true;
            default:
                return false;
        }
    }
}
