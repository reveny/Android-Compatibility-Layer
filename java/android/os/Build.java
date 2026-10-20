package android.os;

public class Build
{
    public static final String BOARD = runtime.PlatformConfig.BOARD;
    public static final String BRAND = runtime.PlatformConfig.BRAND;
    public static final String DEVICE = runtime.PlatformConfig.DEVICE;
    public static final String FINGERPRINT = runtime.PlatformConfig.FINGERPRINT;
    public static final String HARDWARE = runtime.PlatformConfig.HARDWARE;
    public static final String ID = runtime.PlatformConfig.BUILD_ID;
    public static final String MANUFACTURER = runtime.PlatformConfig.MANUFACTURER;
    public static final String MODEL = runtime.PlatformConfig.MODEL;
    public static final String SOC_MODEL = runtime.PlatformConfig.SOC_MODEL;
    public static final String SOC_MANUFACTURER = runtime.PlatformConfig.SOC_MANUFACTURER;
    public static final String PRODUCT = runtime.PlatformConfig.PRODUCT;
    public static final String TAGS = runtime.PlatformConfig.BUILD_TAGS;
    public static final String TYPE = runtime.PlatformConfig.BUILD_TYPE;
    public static final String HOST = runtime.PlatformConfig.BUILD_HOST;
    public static final String USER = runtime.PlatformConfig.BUILD_USER;
    public static final String DISPLAY = runtime.PlatformConfig.BUILD_ID;
    public static final String SKU = "unknown";
    public static final String ODM_SKU = "unknown";
    public static final String BOOTLOADER = "unknown";
    public static final String RADIO = "unknown";
    public static final String SERIAL = "unknown";
    public static final long TIME = 0L;
    public static final String[] SUPPORTED_ABIS = {"arm64-v8a"};
    public static final String[] SUPPORTED_64_BIT_ABIS = {"arm64-v8a"};
    public static final String[] SUPPORTED_32_BIT_ABIS = {};

    public static String getRadioVersion()
    {
        return RADIO;
    }

    public static String getSerial()
    {
        return SERIAL;
    }

    public static class VERSION
    {
        public static final int SDK_INT = runtime.PlatformConfig.SDK_INT;
        public static final int PREVIEW_SDK_INT = 0;
        public static final int RESOURCES_SDK_INT = runtime.PlatformConfig.SDK_INT;
        public static final int MEDIA_PERFORMANCE_CLASS = 0;
        public static final String RELEASE = runtime.PlatformConfig.RELEASE;
        public static final String RELEASE_OR_CODENAME = runtime.PlatformConfig.RELEASE;
        public static final String CODENAME = "REL";
        public static final String INCREMENTAL = runtime.PlatformConfig.INCREMENTAL;
        public static final String SECURITY_PATCH = runtime.PlatformConfig.SECURITY_PATCH;
        public static final String BASE_OS = "";
        public static final String SDK = String.valueOf(runtime.PlatformConfig.SDK_INT);
    }

    public static class VERSION_CODES
    {
        public static final int LOLLIPOP = 21;
        public static final int LOLLIPOP_MR1 = 22;
        public static final int M = 23;
        public static final int N = 24;
        public static final int N_MR1 = 25;
        public static final int O = 26;
        public static final int O_MR1 = 27;
        public static final int P = 28;
        public static final int Q = 29;
        public static final int R = 30;
        public static final int S = 31;
        public static final int S_V2 = 32;
        public static final int TIRAMISU = 33;
        public static final int UPSIDE_DOWN_CAKE = 34;
        public static final int VANILLA_ICE_CREAM = 35;
    }
}
