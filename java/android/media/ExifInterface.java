package android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.CRC32;

public class ExifInterface
{
    public static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    public static final int ORIENTATION_FLIP_VERTICAL = 4;
    public static final int ORIENTATION_NORMAL = 1;
    public static final int ORIENTATION_ROTATE_180 = 3;
    public static final int ORIENTATION_ROTATE_270 = 8;
    public static final int ORIENTATION_ROTATE_90 = 6;
    public static final int ORIENTATION_TRANSPOSE = 5;
    public static final int ORIENTATION_TRANSVERSE = 7;
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int STREAM_TYPE_EXIF_DATA_ONLY = 1;
    public static final int STREAM_TYPE_FULL_IMAGE_DATA = 0;
    public static final String TAG_APERTURE = "FNumber";
    public static final String TAG_APERTURE_VALUE = "ApertureValue";
    public static final String TAG_ARTIST = "Artist";
    public static final String TAG_BITS_PER_SAMPLE = "BitsPerSample";
    public static final String TAG_BRIGHTNESS_VALUE = "BrightnessValue";
    public static final String TAG_CFA_PATTERN = "CFAPattern";
    public static final String TAG_COLOR_SPACE = "ColorSpace";
    public static final String TAG_COMPONENTS_CONFIGURATION = "ComponentsConfiguration";
    public static final String TAG_COMPRESSED_BITS_PER_PIXEL = "CompressedBitsPerPixel";
    public static final String TAG_COMPRESSION = "Compression";
    public static final String TAG_CONTRAST = "Contrast";
    public static final String TAG_COPYRIGHT = "Copyright";
    public static final String TAG_CUSTOM_RENDERED = "CustomRendered";
    public static final String TAG_DATETIME = "DateTime";
    public static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    public static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    public static final String TAG_DEFAULT_CROP_SIZE = "DefaultCropSize";
    public static final String TAG_DEVICE_SETTING_DESCRIPTION = "DeviceSettingDescription";
    public static final String TAG_DIGITAL_ZOOM_RATIO = "DigitalZoomRatio";
    public static final String TAG_DNG_VERSION = "DNGVersion";
    public static final String TAG_EXIF_VERSION = "ExifVersion";
    public static final String TAG_EXPOSURE_BIAS_VALUE = "ExposureBiasValue";
    public static final String TAG_EXPOSURE_INDEX = "ExposureIndex";
    public static final String TAG_EXPOSURE_MODE = "ExposureMode";
    public static final String TAG_EXPOSURE_PROGRAM = "ExposureProgram";
    public static final String TAG_EXPOSURE_TIME = "ExposureTime";
    public static final String TAG_FILE_SOURCE = "FileSource";
    public static final String TAG_FLASH = "Flash";
    public static final String TAG_FLASHPIX_VERSION = "FlashpixVersion";
    public static final String TAG_FLASH_ENERGY = "FlashEnergy";
    public static final String TAG_FOCAL_LENGTH = "FocalLength";
    public static final String TAG_FOCAL_LENGTH_IN_35MM_FILM = "FocalLengthIn35mmFilm";
    public static final String TAG_FOCAL_PLANE_RESOLUTION_UNIT = "FocalPlaneResolutionUnit";
    public static final String TAG_FOCAL_PLANE_X_RESOLUTION = "FocalPlaneXResolution";
    public static final String TAG_FOCAL_PLANE_Y_RESOLUTION = "FocalPlaneYResolution";
    public static final String TAG_F_NUMBER = "FNumber";
    public static final String TAG_GAIN_CONTROL = "GainControl";
    public static final String TAG_GPS_ALTITUDE = "GPSAltitude";
    public static final String TAG_GPS_ALTITUDE_REF = "GPSAltitudeRef";
    public static final String TAG_GPS_AREA_INFORMATION = "GPSAreaInformation";
    public static final String TAG_GPS_DATESTAMP = "GPSDateStamp";
    public static final String TAG_GPS_DEST_BEARING = "GPSDestBearing";
    public static final String TAG_GPS_DEST_BEARING_REF = "GPSDestBearingRef";
    public static final String TAG_GPS_DEST_DISTANCE = "GPSDestDistance";
    public static final String TAG_GPS_DEST_DISTANCE_REF = "GPSDestDistanceRef";
    public static final String TAG_GPS_DEST_LATITUDE = "GPSDestLatitude";
    public static final String TAG_GPS_DEST_LATITUDE_REF = "GPSDestLatitudeRef";
    public static final String TAG_GPS_DEST_LONGITUDE = "GPSDestLongitude";
    public static final String TAG_GPS_DEST_LONGITUDE_REF = "GPSDestLongitudeRef";
    public static final String TAG_GPS_DIFFERENTIAL = "GPSDifferential";
    public static final String TAG_GPS_DOP = "GPSDOP";
    public static final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
    public static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";
    public static final String TAG_GPS_LATITUDE = "GPSLatitude";
    public static final String TAG_GPS_LATITUDE_REF = "GPSLatitudeRef";
    public static final String TAG_GPS_LONGITUDE = "GPSLongitude";
    public static final String TAG_GPS_LONGITUDE_REF = "GPSLongitudeRef";
    public static final String TAG_GPS_MAP_DATUM = "GPSMapDatum";
    public static final String TAG_GPS_MEASURE_MODE = "GPSMeasureMode";
    public static final String TAG_GPS_PROCESSING_METHOD = "GPSProcessingMethod";
    public static final String TAG_GPS_SATELLITES = "GPSSatellites";
    public static final String TAG_GPS_SPEED = "GPSSpeed";
    public static final String TAG_GPS_SPEED_REF = "GPSSpeedRef";
    public static final String TAG_GPS_STATUS = "GPSStatus";
    public static final String TAG_GPS_TIMESTAMP = "GPSTimeStamp";
    public static final String TAG_GPS_TRACK = "GPSTrack";
    public static final String TAG_GPS_TRACK_REF = "GPSTrackRef";
    public static final String TAG_GPS_VERSION_ID = "GPSVersionID";
    public static final String TAG_IMAGE_DESCRIPTION = "ImageDescription";
    public static final String TAG_IMAGE_LENGTH = "ImageLength";
    public static final String TAG_IMAGE_UNIQUE_ID = "ImageUniqueID";
    public static final String TAG_IMAGE_WIDTH = "ImageWidth";
    public static final String TAG_INTEROPERABILITY_INDEX = "InteroperabilityIndex";
    public static final String TAG_ISO = "ISOSpeedRatings";
    public static final String TAG_ISO_SPEED_RATINGS = "ISOSpeedRatings";
    public static final String TAG_JPEG_INTERCHANGE_FORMAT = "JPEGInterchangeFormat";
    public static final String TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = "JPEGInterchangeFormatLength";
    public static final String TAG_LIGHT_SOURCE = "LightSource";
    public static final String TAG_MAKE = "Make";
    public static final String TAG_MAKER_NOTE = "MakerNote";
    public static final String TAG_MAX_APERTURE_VALUE = "MaxApertureValue";
    public static final String TAG_METERING_MODE = "MeteringMode";
    public static final String TAG_MODEL = "Model";
    public static final String TAG_NEW_SUBFILE_TYPE = "NewSubfileType";
    public static final String TAG_OECF = "OECF";
    public static final String TAG_OFFSET_TIME = "OffsetTime";
    public static final String TAG_OFFSET_TIME_DIGITIZED = "OffsetTimeDigitized";
    public static final String TAG_OFFSET_TIME_ORIGINAL = "OffsetTimeOriginal";
    public static final String TAG_ORF_ASPECT_FRAME = "AspectFrame";
    public static final String TAG_ORF_PREVIEW_IMAGE_LENGTH = "PreviewImageLength";
    public static final String TAG_ORF_PREVIEW_IMAGE_START = "PreviewImageStart";
    public static final String TAG_ORF_THUMBNAIL_IMAGE = "ThumbnailImage";
    public static final String TAG_ORIENTATION = "Orientation";
    public static final String TAG_PHOTOMETRIC_INTERPRETATION = "PhotometricInterpretation";
    public static final String TAG_PIXEL_X_DIMENSION = "PixelXDimension";
    public static final String TAG_PIXEL_Y_DIMENSION = "PixelYDimension";
    public static final String TAG_PLANAR_CONFIGURATION = "PlanarConfiguration";
    public static final String TAG_PRIMARY_CHROMATICITIES = "PrimaryChromaticities";
    public static final String TAG_REFERENCE_BLACK_WHITE = "ReferenceBlackWhite";
    public static final String TAG_RELATED_SOUND_FILE = "RelatedSoundFile";
    public static final String TAG_RESOLUTION_UNIT = "ResolutionUnit";
    public static final String TAG_ROWS_PER_STRIP = "RowsPerStrip";
    public static final String TAG_RW2_ISO = "ISO";
    public static final String TAG_RW2_JPG_FROM_RAW = "JpgFromRaw";
    public static final String TAG_RW2_SENSOR_BOTTOM_BORDER = "SensorBottomBorder";
    public static final String TAG_RW2_SENSOR_LEFT_BORDER = "SensorLeftBorder";
    public static final String TAG_RW2_SENSOR_RIGHT_BORDER = "SensorRightBorder";
    public static final String TAG_RW2_SENSOR_TOP_BORDER = "SensorTopBorder";
    public static final String TAG_SAMPLES_PER_PIXEL = "SamplesPerPixel";
    public static final String TAG_SATURATION = "Saturation";
    public static final String TAG_SCENE_CAPTURE_TYPE = "SceneCaptureType";
    public static final String TAG_SCENE_TYPE = "SceneType";
    public static final String TAG_SENSING_METHOD = "SensingMethod";
    public static final String TAG_SHARPNESS = "Sharpness";
    public static final String TAG_SHUTTER_SPEED_VALUE = "ShutterSpeedValue";
    public static final String TAG_SOFTWARE = "Software";
    public static final String TAG_SPATIAL_FREQUENCY_RESPONSE = "SpatialFrequencyResponse";
    public static final String TAG_SPECTRAL_SENSITIVITY = "SpectralSensitivity";
    public static final String TAG_STRIP_BYTE_COUNTS = "StripByteCounts";
    public static final String TAG_STRIP_OFFSETS = "StripOffsets";
    public static final String TAG_SUBFILE_TYPE = "SubfileType";
    public static final String TAG_SUBJECT_AREA = "SubjectArea";
    public static final String TAG_SUBJECT_DISTANCE = "SubjectDistance";
    public static final String TAG_SUBJECT_DISTANCE_RANGE = "SubjectDistanceRange";
    public static final String TAG_SUBJECT_LOCATION = "SubjectLocation";
    public static final String TAG_SUBSEC_TIME = "SubSecTime";
    public static final String TAG_SUBSEC_TIME_DIG = "SubSecTimeDigitized";
    public static final String TAG_SUBSEC_TIME_DIGITIZED = "SubSecTimeDigitized";
    public static final String TAG_SUBSEC_TIME_ORIG = "SubSecTimeOriginal";
    public static final String TAG_SUBSEC_TIME_ORIGINAL = "SubSecTimeOriginal";
    public static final String TAG_THUMBNAIL_IMAGE_LENGTH = "ThumbnailImageLength";
    public static final String TAG_THUMBNAIL_IMAGE_WIDTH = "ThumbnailImageWidth";
    public static final String TAG_THUMBNAIL_ORIENTATION = "ThumbnailOrientation";
    public static final String TAG_TRANSFER_FUNCTION = "TransferFunction";
    public static final String TAG_USER_COMMENT = "UserComment";
    public static final String TAG_WHITE_BALANCE = "WhiteBalance";
    public static final String TAG_WHITE_POINT = "WhitePoint";
    public static final String TAG_XMP = "Xmp";
    public static final String TAG_X_RESOLUTION = "XResolution";
    public static final String TAG_Y_CB_CR_COEFFICIENTS = "YCbCrCoefficients";
    public static final String TAG_Y_CB_CR_POSITIONING = "YCbCrPositioning";
    public static final String TAG_Y_CB_CR_SUB_SAMPLING = "YCbCrSubSampling";
    public static final String TAG_Y_RESOLUTION = "YResolution";
    public static final int WHITEBALANCE_AUTO = 0;
    public static final int WHITEBALANCE_MANUAL = 1;

    private static final int FORMAT_BYTE = 1;
    private static final int FORMAT_STRING = 2;
    private static final int FORMAT_USHORT = 3;
    private static final int FORMAT_ULONG = 4;
    private static final int FORMAT_URATIONAL = 5;
    private static final int FORMAT_SBYTE = 6;
    private static final int FORMAT_UNDEFINED = 7;
    private static final int FORMAT_SSHORT = 8;
    private static final int FORMAT_SLONG = 9;
    private static final int FORMAT_SRATIONAL = 10;
    private static final int FORMAT_SINGLE = 11;
    private static final int FORMAT_DOUBLE = 12;
    private static final int[] FORMAT_SIZES = {0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};

    private static final int IFD_PRIMARY = 0;
    private static final int IFD_EXIF = 1;
    private static final int IFD_GPS = 2;
    private static final int IFD_INTEROPERABILITY = 3;
    private static final int IFD_THUMBNAIL = 4;
    private static final int IFD_COUNT = 5;

    private static final int POINTER_EXIF = 0x8769;
    private static final int POINTER_GPS = 0x8825;
    private static final int POINTER_INTEROPERABILITY = 0xA005;
    private static final int TAG_NUMBER_JPEG_INTERCHANGE_FORMAT = 0x0201;
    private static final int TAG_NUMBER_JPEG_INTERCHANGE_FORMAT_LENGTH = 0x0202;

    private static final int MIME_UNKNOWN = 0;
    private static final int MIME_JPEG = 1;
    private static final int MIME_PNG = 2;
    private static final int MIME_WEBP = 3;
    private static final int MIME_TIFF = 4;

    private static final int MARKER_PREFIX = 0xFF;
    private static final int MARKER_SOI = 0xD8;
    private static final int MARKER_SOS = 0xDA;
    private static final int MARKER_EOI = 0xD9;
    private static final int MARKER_APP1 = 0xE1;
    private static final byte[] EXIF_IDENTIFIER = {'E', 'x', 'i', 'f', 0, 0};
    private static final byte[] XMP_IDENTIFIER = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG_SIGNATURE = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
    private static final int MAX_SEGMENT_LENGTH = 65535;

    private static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<>(Arrays.asList("image/jpeg", "image/x-adobe-dng",
        "image/x-canon-cr2", "image/x-nikon-nef", "image/x-nikon-nrw", "image/x-sony-arw", "image/x-panasonic-rw2",
        "image/x-olympus-orf", "image/x-pentax-pef", "image/x-samsung-srw", "image/x-fuji-raf", "image/heic", "image/heif",
        "image/png", "image/webp"));

    private static final Tag[][] TAGS = new Tag[IFD_COUNT][];
    private static final List<Map<Integer, Tag>> TAGS_BY_NUMBER = new ArrayList<>();
    private static final List<Map<String, Tag>> TAGS_BY_NAME = new ArrayList<>();

    static
    {
        TAGS[IFD_PRIMARY] = new Tag[] {new Tag(TAG_NEW_SUBFILE_TYPE, 0x00FE, FORMAT_ULONG),
            new Tag(TAG_SUBFILE_TYPE, 0x00FF, FORMAT_ULONG), new Tag(TAG_IMAGE_WIDTH, 0x0100, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_IMAGE_LENGTH, 0x0101, FORMAT_USHORT, FORMAT_ULONG), new Tag(TAG_BITS_PER_SAMPLE, 0x0102, FORMAT_USHORT),
            new Tag(TAG_COMPRESSION, 0x0103, FORMAT_USHORT), new Tag(TAG_PHOTOMETRIC_INTERPRETATION, 0x0106, FORMAT_USHORT),
            new Tag(TAG_IMAGE_DESCRIPTION, 0x010E, FORMAT_STRING), new Tag(TAG_MAKE, 0x010F, FORMAT_STRING),
            new Tag(TAG_MODEL, 0x0110, FORMAT_STRING), new Tag(TAG_STRIP_OFFSETS, 0x0111, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_ORIENTATION, 0x0112, FORMAT_USHORT), new Tag(TAG_SAMPLES_PER_PIXEL, 0x0115, FORMAT_USHORT),
            new Tag(TAG_ROWS_PER_STRIP, 0x0116, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_STRIP_BYTE_COUNTS, 0x0117, FORMAT_USHORT, FORMAT_ULONG), new Tag(TAG_X_RESOLUTION, 0x011A, FORMAT_URATIONAL),
            new Tag(TAG_Y_RESOLUTION, 0x011B, FORMAT_URATIONAL), new Tag(TAG_PLANAR_CONFIGURATION, 0x011C, FORMAT_USHORT),
            new Tag(TAG_RESOLUTION_UNIT, 0x0128, FORMAT_USHORT), new Tag(TAG_TRANSFER_FUNCTION, 0x012D, FORMAT_USHORT),
            new Tag(TAG_SOFTWARE, 0x0131, FORMAT_STRING), new Tag(TAG_DATETIME, 0x0132, FORMAT_STRING),
            new Tag(TAG_ARTIST, 0x013B, FORMAT_STRING), new Tag(TAG_WHITE_POINT, 0x013E, FORMAT_URATIONAL),
            new Tag(TAG_PRIMARY_CHROMATICITIES, 0x013F, FORMAT_URATIONAL),
            new Tag(TAG_JPEG_INTERCHANGE_FORMAT, TAG_NUMBER_JPEG_INTERCHANGE_FORMAT, FORMAT_ULONG),
            new Tag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, TAG_NUMBER_JPEG_INTERCHANGE_FORMAT_LENGTH, FORMAT_ULONG),
            new Tag(TAG_Y_CB_CR_COEFFICIENTS, 0x0211, FORMAT_URATIONAL), new Tag(TAG_Y_CB_CR_SUB_SAMPLING, 0x0212, FORMAT_USHORT),
            new Tag(TAG_Y_CB_CR_POSITIONING, 0x0213, FORMAT_USHORT), new Tag(TAG_REFERENCE_BLACK_WHITE, 0x0214, FORMAT_URATIONAL),
            new Tag(TAG_XMP, 0x02BC, FORMAT_BYTE), new Tag(TAG_COPYRIGHT, 0x8298, FORMAT_STRING),
            new Tag(TAG_DNG_VERSION, 0xC612, FORMAT_BYTE), new Tag(TAG_DEFAULT_CROP_SIZE, 0xC620, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_RW2_SENSOR_TOP_BORDER, 0x0004, FORMAT_ULONG), new Tag(TAG_RW2_SENSOR_LEFT_BORDER, 0x0005, FORMAT_ULONG),
            new Tag(TAG_RW2_SENSOR_BOTTOM_BORDER, 0x0006, FORMAT_ULONG), new Tag(TAG_RW2_SENSOR_RIGHT_BORDER, 0x0007, FORMAT_ULONG),
            new Tag(TAG_RW2_ISO, 0x0017, FORMAT_USHORT), new Tag(TAG_RW2_JPG_FROM_RAW, 0x002E, FORMAT_UNDEFINED)};

        TAGS[IFD_EXIF] = new Tag[] {new Tag(TAG_EXPOSURE_TIME, 0x829A, FORMAT_URATIONAL),
            new Tag(TAG_F_NUMBER, 0x829D, FORMAT_URATIONAL), new Tag(TAG_EXPOSURE_PROGRAM, 0x8822, FORMAT_USHORT),
            new Tag(TAG_SPECTRAL_SENSITIVITY, 0x8824, FORMAT_STRING), new Tag(TAG_ISO_SPEED_RATINGS, 0x8827, FORMAT_USHORT),
            new Tag(TAG_OECF, 0x8828, FORMAT_UNDEFINED), new Tag(TAG_EXIF_VERSION, 0x9000, FORMAT_STRING),
            new Tag(TAG_DATETIME_ORIGINAL, 0x9003, FORMAT_STRING), new Tag(TAG_DATETIME_DIGITIZED, 0x9004, FORMAT_STRING),
            new Tag(TAG_OFFSET_TIME, 0x9010, FORMAT_STRING), new Tag(TAG_OFFSET_TIME_ORIGINAL, 0x9011, FORMAT_STRING),
            new Tag(TAG_OFFSET_TIME_DIGITIZED, 0x9012, FORMAT_STRING), new Tag(TAG_COMPONENTS_CONFIGURATION, 0x9101, FORMAT_UNDEFINED),
            new Tag(TAG_COMPRESSED_BITS_PER_PIXEL, 0x9102, FORMAT_URATIONAL),
            new Tag(TAG_SHUTTER_SPEED_VALUE, 0x9201, FORMAT_SRATIONAL), new Tag(TAG_APERTURE_VALUE, 0x9202, FORMAT_URATIONAL),
            new Tag(TAG_BRIGHTNESS_VALUE, 0x9203, FORMAT_SRATIONAL), new Tag(TAG_EXPOSURE_BIAS_VALUE, 0x9204, FORMAT_SRATIONAL),
            new Tag(TAG_MAX_APERTURE_VALUE, 0x9205, FORMAT_URATIONAL), new Tag(TAG_SUBJECT_DISTANCE, 0x9206, FORMAT_URATIONAL),
            new Tag(TAG_METERING_MODE, 0x9207, FORMAT_USHORT), new Tag(TAG_LIGHT_SOURCE, 0x9208, FORMAT_USHORT),
            new Tag(TAG_FLASH, 0x9209, FORMAT_USHORT), new Tag(TAG_FOCAL_LENGTH, 0x920A, FORMAT_URATIONAL),
            new Tag(TAG_SUBJECT_AREA, 0x9214, FORMAT_USHORT), new Tag(TAG_MAKER_NOTE, 0x927C, FORMAT_UNDEFINED),
            new Tag(TAG_USER_COMMENT, 0x9286, FORMAT_UNDEFINED), new Tag(TAG_SUBSEC_TIME, 0x9290, FORMAT_STRING),
            new Tag(TAG_SUBSEC_TIME_ORIGINAL, 0x9291, FORMAT_STRING), new Tag(TAG_SUBSEC_TIME_DIGITIZED, 0x9292, FORMAT_STRING),
            new Tag(TAG_FLASHPIX_VERSION, 0xA000, FORMAT_UNDEFINED), new Tag(TAG_COLOR_SPACE, 0xA001, FORMAT_USHORT),
            new Tag(TAG_PIXEL_X_DIMENSION, 0xA002, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_PIXEL_Y_DIMENSION, 0xA003, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_RELATED_SOUND_FILE, 0xA004, FORMAT_STRING), new Tag(TAG_FLASH_ENERGY, 0xA20B, FORMAT_URATIONAL),
            new Tag(TAG_SPATIAL_FREQUENCY_RESPONSE, 0xA20C, FORMAT_UNDEFINED),
            new Tag(TAG_FOCAL_PLANE_X_RESOLUTION, 0xA20E, FORMAT_URATIONAL),
            new Tag(TAG_FOCAL_PLANE_Y_RESOLUTION, 0xA20F, FORMAT_URATIONAL),
            new Tag(TAG_FOCAL_PLANE_RESOLUTION_UNIT, 0xA210, FORMAT_USHORT), new Tag(TAG_SUBJECT_LOCATION, 0xA214, FORMAT_USHORT),
            new Tag(TAG_EXPOSURE_INDEX, 0xA215, FORMAT_URATIONAL), new Tag(TAG_SENSING_METHOD, 0xA217, FORMAT_USHORT),
            new Tag(TAG_FILE_SOURCE, 0xA300, FORMAT_UNDEFINED), new Tag(TAG_SCENE_TYPE, 0xA301, FORMAT_UNDEFINED),
            new Tag(TAG_CFA_PATTERN, 0xA302, FORMAT_UNDEFINED), new Tag(TAG_CUSTOM_RENDERED, 0xA401, FORMAT_USHORT),
            new Tag(TAG_EXPOSURE_MODE, 0xA402, FORMAT_USHORT), new Tag(TAG_WHITE_BALANCE, 0xA403, FORMAT_USHORT),
            new Tag(TAG_DIGITAL_ZOOM_RATIO, 0xA404, FORMAT_URATIONAL), new Tag(TAG_FOCAL_LENGTH_IN_35MM_FILM, 0xA405, FORMAT_USHORT),
            new Tag(TAG_SCENE_CAPTURE_TYPE, 0xA406, FORMAT_USHORT), new Tag(TAG_GAIN_CONTROL, 0xA407, FORMAT_USHORT),
            new Tag(TAG_CONTRAST, 0xA408, FORMAT_USHORT), new Tag(TAG_SATURATION, 0xA409, FORMAT_USHORT),
            new Tag(TAG_SHARPNESS, 0xA40A, FORMAT_USHORT), new Tag(TAG_DEVICE_SETTING_DESCRIPTION, 0xA40B, FORMAT_UNDEFINED),
            new Tag(TAG_SUBJECT_DISTANCE_RANGE, 0xA40C, FORMAT_USHORT), new Tag(TAG_IMAGE_UNIQUE_ID, 0xA420, FORMAT_STRING)};

        TAGS[IFD_GPS] = new Tag[] {new Tag(TAG_GPS_VERSION_ID, 0x0000, FORMAT_BYTE),
            new Tag(TAG_GPS_LATITUDE_REF, 0x0001, FORMAT_STRING), new Tag(TAG_GPS_LATITUDE, 0x0002, FORMAT_URATIONAL),
            new Tag(TAG_GPS_LONGITUDE_REF, 0x0003, FORMAT_STRING), new Tag(TAG_GPS_LONGITUDE, 0x0004, FORMAT_URATIONAL),
            new Tag(TAG_GPS_ALTITUDE_REF, 0x0005, FORMAT_BYTE), new Tag(TAG_GPS_ALTITUDE, 0x0006, FORMAT_URATIONAL),
            new Tag(TAG_GPS_TIMESTAMP, 0x0007, FORMAT_URATIONAL), new Tag(TAG_GPS_SATELLITES, 0x0008, FORMAT_STRING),
            new Tag(TAG_GPS_STATUS, 0x0009, FORMAT_STRING), new Tag(TAG_GPS_MEASURE_MODE, 0x000A, FORMAT_STRING),
            new Tag(TAG_GPS_DOP, 0x000B, FORMAT_URATIONAL), new Tag(TAG_GPS_SPEED_REF, 0x000C, FORMAT_STRING),
            new Tag(TAG_GPS_SPEED, 0x000D, FORMAT_URATIONAL), new Tag(TAG_GPS_TRACK_REF, 0x000E, FORMAT_STRING),
            new Tag(TAG_GPS_TRACK, 0x000F, FORMAT_URATIONAL), new Tag(TAG_GPS_IMG_DIRECTION_REF, 0x0010, FORMAT_STRING),
            new Tag(TAG_GPS_IMG_DIRECTION, 0x0011, FORMAT_URATIONAL), new Tag(TAG_GPS_MAP_DATUM, 0x0012, FORMAT_STRING),
            new Tag(TAG_GPS_DEST_LATITUDE_REF, 0x0013, FORMAT_STRING), new Tag(TAG_GPS_DEST_LATITUDE, 0x0014, FORMAT_URATIONAL),
            new Tag(TAG_GPS_DEST_LONGITUDE_REF, 0x0015, FORMAT_STRING), new Tag(TAG_GPS_DEST_LONGITUDE, 0x0016, FORMAT_URATIONAL),
            new Tag(TAG_GPS_DEST_BEARING_REF, 0x0017, FORMAT_STRING), new Tag(TAG_GPS_DEST_BEARING, 0x0018, FORMAT_URATIONAL),
            new Tag(TAG_GPS_DEST_DISTANCE_REF, 0x0019, FORMAT_STRING), new Tag(TAG_GPS_DEST_DISTANCE, 0x001A, FORMAT_URATIONAL),
            new Tag(TAG_GPS_PROCESSING_METHOD, 0x001B, FORMAT_UNDEFINED),
            new Tag(TAG_GPS_AREA_INFORMATION, 0x001C, FORMAT_UNDEFINED), new Tag(TAG_GPS_DATESTAMP, 0x001D, FORMAT_STRING),
            new Tag(TAG_GPS_DIFFERENTIAL, 0x001E, FORMAT_USHORT)};

        TAGS[IFD_INTEROPERABILITY] = new Tag[] {new Tag(TAG_INTEROPERABILITY_INDEX, 0x0001, FORMAT_STRING)};

        TAGS[IFD_THUMBNAIL] = new Tag[] {new Tag(TAG_NEW_SUBFILE_TYPE, 0x00FE, FORMAT_ULONG),
            new Tag(TAG_SUBFILE_TYPE, 0x00FF, FORMAT_ULONG), new Tag(TAG_THUMBNAIL_IMAGE_WIDTH, 0x0100, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_THUMBNAIL_IMAGE_LENGTH, 0x0101, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_BITS_PER_SAMPLE, 0x0102, FORMAT_USHORT), new Tag(TAG_COMPRESSION, 0x0103, FORMAT_USHORT),
            new Tag(TAG_PHOTOMETRIC_INTERPRETATION, 0x0106, FORMAT_USHORT), new Tag(TAG_IMAGE_DESCRIPTION, 0x010E, FORMAT_STRING),
            new Tag(TAG_MAKE, 0x010F, FORMAT_STRING), new Tag(TAG_MODEL, 0x0110, FORMAT_STRING),
            new Tag(TAG_STRIP_OFFSETS, 0x0111, FORMAT_USHORT, FORMAT_ULONG), new Tag(TAG_THUMBNAIL_ORIENTATION, 0x0112, FORMAT_USHORT),
            new Tag(TAG_SAMPLES_PER_PIXEL, 0x0115, FORMAT_USHORT), new Tag(TAG_ROWS_PER_STRIP, 0x0116, FORMAT_USHORT, FORMAT_ULONG),
            new Tag(TAG_STRIP_BYTE_COUNTS, 0x0117, FORMAT_USHORT, FORMAT_ULONG), new Tag(TAG_X_RESOLUTION, 0x011A, FORMAT_URATIONAL),
            new Tag(TAG_Y_RESOLUTION, 0x011B, FORMAT_URATIONAL), new Tag(TAG_PLANAR_CONFIGURATION, 0x011C, FORMAT_USHORT),
            new Tag(TAG_RESOLUTION_UNIT, 0x0128, FORMAT_USHORT), new Tag(TAG_TRANSFER_FUNCTION, 0x012D, FORMAT_USHORT),
            new Tag(TAG_SOFTWARE, 0x0131, FORMAT_STRING), new Tag(TAG_DATETIME, 0x0132, FORMAT_STRING),
            new Tag(TAG_ARTIST, 0x013B, FORMAT_STRING), new Tag(TAG_WHITE_POINT, 0x013E, FORMAT_URATIONAL),
            new Tag(TAG_PRIMARY_CHROMATICITIES, 0x013F, FORMAT_URATIONAL),
            new Tag(TAG_JPEG_INTERCHANGE_FORMAT, TAG_NUMBER_JPEG_INTERCHANGE_FORMAT, FORMAT_ULONG),
            new Tag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, TAG_NUMBER_JPEG_INTERCHANGE_FORMAT_LENGTH, FORMAT_ULONG),
            new Tag(TAG_Y_CB_CR_COEFFICIENTS, 0x0211, FORMAT_URATIONAL), new Tag(TAG_Y_CB_CR_SUB_SAMPLING, 0x0212, FORMAT_USHORT),
            new Tag(TAG_Y_CB_CR_POSITIONING, 0x0213, FORMAT_USHORT), new Tag(TAG_REFERENCE_BLACK_WHITE, 0x0214, FORMAT_URATIONAL),
            new Tag(TAG_COPYRIGHT, 0x8298, FORMAT_STRING)};

        for (Tag[] tags : TAGS)
        {
            Map<Integer, Tag> byNumber = new HashMap<>();
            Map<String, Tag> byName = new HashMap<>();

            for (Tag tag : tags)
            {
                byNumber.put(tag.mNumber, tag);
                byName.put(tag.mName, tag);
            }

            TAGS_BY_NUMBER.add(byNumber);
            TAGS_BY_NAME.add(byName);
        }
    }

    private final String mFilename;
    private final FileDescriptor mSeekableFileDescriptor;
    private final boolean mSeekable;
    private final List<Map<String, Attribute>> mAttributes = new ArrayList<>();
    private ByteOrder mByteOrder = ByteOrder.BIG_ENDIAN;
    private int mMimeType = MIME_UNKNOWN;
    private byte[] mThumbnailBytes;
    private long mThumbnailOffset = -1;
    private boolean mThumbnailCompressed;
    private boolean mXmpFromSegment;
    private boolean mModified;
    private boolean mAttributesValid;

    public ExifInterface(File file) throws IOException
    {
        this(file == null ? null : file.getAbsolutePath());
    }

    public ExifInterface(FileDescriptor fileDescriptor) throws IOException
    {
        if (fileDescriptor == null) throw new NullPointerException("fileDescriptor cannot be null");

        mFilename = null;
        initializeAttributes();

        FileInputStream input = new FileInputStream(fileDescriptor);
        FileChannel channel = input.getChannel();
        boolean seekable;
        long start = 0;

        try
        {
            start = channel.position();
            seekable = true;
        }
        catch (IOException failure)
        {
            seekable = false;
        }

        mSeekable = seekable;
        mSeekableFileDescriptor = seekable ? fileDescriptor : null;

        byte[] data = readAll(input);

        if (seekable) channel.position(start);

        loadAttributes(data, STREAM_TYPE_FULL_IMAGE_DATA);
    }

    public ExifInterface(InputStream inputStream) throws IOException
    {
        this(inputStream, STREAM_TYPE_FULL_IMAGE_DATA);
    }

    public ExifInterface(InputStream inputStream, int streamType) throws IOException
    {
        if (inputStream == null) throw new NullPointerException("inputStream cannot be null");

        mFilename = null;
        mSeekableFileDescriptor = null;
        mSeekable = false;
        initializeAttributes();
        loadAttributes(readAll(inputStream), streamType);
    }

    public ExifInterface(String filename) throws IOException
    {
        if (filename == null) throw new NullPointerException("filename cannot be null");

        mFilename = filename;
        mSeekableFileDescriptor = null;
        mSeekable = true;
        initializeAttributes();

        try (FileInputStream input = new FileInputStream(filename))
        {
            loadAttributes(readAll(input), STREAM_TYPE_FULL_IMAGE_DATA);
        }
    }

    public static boolean isSupportedMimeType(String mimeType)
    {
        if (mimeType == null) throw new NullPointerException("mimeType shouldn't be null");

        return SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase(Locale.ROOT));
    }

    public double getAltitude(double defaultValue)
    {
        double altitude = getAttributeDouble(TAG_GPS_ALTITUDE, -1);
        int reference = getAttributeInt(TAG_GPS_ALTITUDE_REF, -1);

        if (altitude >= 0 && reference >= 0) return altitude * (reference == 1 ? -1 : 1);

        return defaultValue;
    }

    public String getAttribute(String tag)
    {
        if (tag == null) throw new NullPointerException("tag shouldn't be null");

        Attribute attribute = findAttribute(tag);

        if (attribute == null) return null;

        if (TAG_GPS_TIMESTAMP.equals(tag))
        {
            if (attribute.mFormat != FORMAT_URATIONAL && attribute.mFormat != FORMAT_SRATIONAL) return null;

            long[] rationals = attribute.longValues(mByteOrder);

            if (rationals.length != 6 || rationals[1] == 0 || rationals[3] == 0 || rationals[5] == 0) return null;

            return String.format(Locale.ROOT, "%02d:%02d:%02d", rationals[0] / rationals[1], rationals[2] / rationals[3],
                rationals[4] / rationals[5]);
        }

        return attribute.stringValue(mByteOrder);
    }

    public byte[] getAttributeBytes(String tag)
    {
        if (tag == null) throw new NullPointerException("tag shouldn't be null");

        Attribute attribute = findAttribute(tag);

        return attribute == null ? null : attribute.mBytes.clone();
    }

    public double getAttributeDouble(String tag, double defaultValue)
    {
        if (tag == null) throw new NullPointerException("tag shouldn't be null");

        Attribute attribute = findAttribute(tag);

        if (attribute == null) return defaultValue;

        try
        {
            return attribute.doubleValue(mByteOrder);
        }
        catch (NumberFormatException failure)
        {
            return defaultValue;
        }
    }

    public int getAttributeInt(String tag, int defaultValue)
    {
        if (tag == null) throw new NullPointerException("tag shouldn't be null");

        Attribute attribute = findAttribute(tag);

        if (attribute == null) return defaultValue;

        try
        {
            return attribute.intValue(mByteOrder);
        }
        catch (NumberFormatException failure)
        {
            return defaultValue;
        }
    }

    public long[] getAttributeRange(String tag)
    {
        if (tag == null) throw new NullPointerException("tag shouldn't be null");
        if (mModified) throw new IllegalStateException("The underlying file has been modified since being parsed");

        Attribute attribute = findAttribute(tag);

        if (attribute == null || attribute.mOffset < 0) return null;

        return new long[] {attribute.mOffset, attribute.mBytes.length};
    }

    public long getDateTime()
    {
        return parseDateTime(getAttribute(TAG_DATETIME), getAttribute(TAG_SUBSEC_TIME), getAttribute(TAG_OFFSET_TIME));
    }

    public long getDateTimeDigitized()
    {
        return parseDateTime(getAttribute(TAG_DATETIME_DIGITIZED), getAttribute(TAG_SUBSEC_TIME_DIGITIZED),
            getAttribute(TAG_OFFSET_TIME_DIGITIZED));
    }

    public long getDateTimeOriginal()
    {
        return parseDateTime(getAttribute(TAG_DATETIME_ORIGINAL), getAttribute(TAG_SUBSEC_TIME_ORIGINAL),
            getAttribute(TAG_OFFSET_TIME_ORIGINAL));
    }

    public long getGpsDateTime()
    {
        String date = getAttribute(TAG_GPS_DATESTAMP);
        String time = getAttribute(TAG_GPS_TIMESTAMP);

        if (date == null || time == null) return -1;

        return parseDateTime(date + " " + time, null, null);
    }

    public boolean getLatLong(float[] output)
    {
        String latitude = getAttribute(TAG_GPS_LATITUDE);
        String latitudeReference = getAttribute(TAG_GPS_LATITUDE_REF);
        String longitude = getAttribute(TAG_GPS_LONGITUDE);
        String longitudeReference = getAttribute(TAG_GPS_LONGITUDE_REF);

        if (latitude == null || latitudeReference == null || longitude == null || longitudeReference == null) return false;

        try
        {
            output[0] = convertRationalLatLonToFloat(latitude, latitudeReference);
            output[1] = convertRationalLatLonToFloat(longitude, longitudeReference);
            return true;
        }
        catch (IllegalArgumentException failure)
        {
            return false;
        }
    }

    public byte[] getThumbnail()
    {
        if (!mThumbnailCompressed) return null;

        return getThumbnailBytes();
    }

    public Bitmap getThumbnailBitmap()
    {
        byte[] bytes = getThumbnailBytes();

        if (bytes == null) return null;

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public byte[] getThumbnailBytes()
    {
        return mThumbnailBytes == null ? null : mThumbnailBytes.clone();
    }

    public long[] getThumbnailRange()
    {
        if (mModified) throw new IllegalStateException("The underlying file has been modified since being parsed");

        if (mThumbnailBytes == null || mThumbnailOffset < 0) return null;

        return new long[] {mThumbnailOffset, mThumbnailBytes.length};
    }

    public boolean hasAttribute(String tag)
    {
        return findAttribute(tag) != null;
    }

    public boolean hasThumbnail()
    {
        return mThumbnailBytes != null;
    }

    public boolean isThumbnailCompressed()
    {
        return mThumbnailBytes != null && mThumbnailCompressed;
    }

    public void saveAttributes() throws IOException
    {
        if (mMimeType != MIME_JPEG && mMimeType != MIME_PNG)
        {
            throw new IOException("ExifInterface only supports saving attributes on JPEG and PNG formats.");
        }

        if (!mSeekable || (mFilename == null && mSeekableFileDescriptor == null))
        {
            throw new IOException("ExifInterface does not support saving attributes for the current input.");
        }

        byte[] original;

        if (mFilename != null)
        {
            try (FileInputStream input = new FileInputStream(mFilename))
            {
                original = readAll(input);
            }
        }
        else
        {
            FileChannel channel = new FileInputStream(mSeekableFileDescriptor).getChannel();
            channel.position(0);
            original = readAll(new FileInputStream(mSeekableFileDescriptor));
        }

        byte[] exif = buildTiff();
        byte[] rewritten = mMimeType == MIME_JPEG ? rewriteJpeg(original, exif) : rewritePng(original, exif);

        if (mFilename != null)
        {
            try (FileOutputStream output = new FileOutputStream(mFilename))
            {
                output.write(rewritten);
            }
        }
        else
        {
            FileChannel channel = new FileOutputStream(mSeekableFileDescriptor).getChannel();
            channel.position(0);
            ByteBuffer buffer = ByteBuffer.wrap(rewritten);

            while (buffer.hasRemaining())
            {
                channel.write(buffer);
            }

            channel.truncate(rewritten.length);
            channel.position(0);
        }

        initializeAttributes();
        mThumbnailBytes = null;
        mThumbnailOffset = -1;
        mThumbnailCompressed = false;
        mXmpFromSegment = false;
        mModified = false;
        loadAttributes(rewritten, STREAM_TYPE_FULL_IMAGE_DATA);
    }

    public void setAttribute(String tag, String value)
    {
        if (tag == null) throw new NullPointerException("tag shouldn't be null");

        String normalized = value;

        if (normalized != null && TAG_GPS_TIMESTAMP.equals(tag))
        {
            String[] parts = normalized.split(":", -1);

            if (parts.length != 3) return;

            try
            {
                normalized = Integer.parseInt(parts[0].trim()) + "/1," + Integer.parseInt(parts[1].trim()) + "/1,"
                    + Integer.parseInt(parts[2].trim()) + "/1";
            }
            catch (NumberFormatException failure)
            {
                return;
            }
        }

        boolean applied = false;

        for (int ifd = 0; ifd < IFD_COUNT; ifd++)
        {
            Tag definition = TAGS_BY_NAME.get(ifd).get(tag);

            if (definition == null) continue;

            boolean present = mAttributes.get(ifd).containsKey(tag);

            if (applied && !present) continue;

            if (normalized == null)
            {
                mAttributes.get(ifd).remove(tag);
                applied = true;
                continue;
            }

            Attribute attribute = encode(definition, normalized);

            if (attribute == null) continue;

            mAttributes.get(ifd).put(tag, attribute);
            applied = true;
        }

        if (applied)
        {
            mModified = true;

            if (TAG_XMP.equals(tag)) mXmpFromSegment = false;
        }
    }

    int getRotationDegrees()
    {
        switch (getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL))
        {
            case ORIENTATION_ROTATE_90:
            case ORIENTATION_TRANSVERSE:
                return 90;
            case ORIENTATION_ROTATE_180:
            case ORIENTATION_FLIP_VERTICAL:
                return 180;
            case ORIENTATION_ROTATE_270:
            case ORIENTATION_TRANSPOSE:
                return 270;
            default:
                return 0;
        }
    }

    private void initializeAttributes()
    {
        mAttributes.clear();

        for (int ifd = 0; ifd < IFD_COUNT; ifd++)
        {
            mAttributes.add(new LinkedHashMap<String, Attribute>());
        }

        mAttributesValid = false;
    }

    private Attribute findAttribute(String tag)
    {
        if (tag == null) return null;

        for (int ifd = 0; ifd < IFD_COUNT; ifd++)
        {
            Attribute attribute = mAttributes.get(ifd).get(tag);

            if (attribute != null) return attribute;
        }

        return null;
    }

    private static byte[] readAll(InputStream input) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[16 * 1024];

        while (true)
        {
            int read = input.read(chunk);

            if (read < 0) break;

            output.write(chunk, 0, read);
        }

        return output.toByteArray();
    }

    private void loadAttributes(byte[] data, int streamType)
    {
        try
        {
            if (streamType == STREAM_TYPE_EXIF_DATA_ONLY)
            {
                int start = startsWith(data, 0, EXIF_IDENTIFIER) ? EXIF_IDENTIFIER.length : 0;
                parseTiff(data, start, data.length - start);
                return;
            }

            mMimeType = detectMimeType(data);

            if (mMimeType == MIME_JPEG)
            {
                parseJpeg(data);
            }
            else if (mMimeType == MIME_PNG)
            {
                parsePng(data);
            }
            else if (mMimeType == MIME_WEBP)
            {
                parseWebp(data);
            }
            else if (mMimeType == MIME_TIFF)
            {
                parseTiff(data, 0, data.length);
            }
        }
        catch (RuntimeException failure)
        {
            android.util.Log.w("ExifInterface", "Invalid image: ExifInterface got an unsupported image format file", failure);
        }
        finally
        {
            mAttributesValid = true;
        }
    }

    private static int detectMimeType(byte[] data)
    {
        if (data.length >= 3 && (data[0] & 0xff) == MARKER_PREFIX && (data[1] & 0xff) == MARKER_SOI) return MIME_JPEG;
        if (startsWith(data, 0, PNG_SIGNATURE)) return MIME_PNG;
        if (data.length >= 12 && startsWith(data, 0, ascii("RIFF")) && startsWith(data, 8, ascii("WEBP"))) return MIME_WEBP;

        if (data.length >= 4)
        {
            boolean little = data[0] == 'I' && data[1] == 'I' && (data[2] == 0x2a || data[2] == 0x55) && data[3] == 0;
            boolean big = data[0] == 'M' && data[1] == 'M' && data[2] == 0 && (data[3] == 0x2a || data[3] == 0x4f);

            if (little || big) return MIME_TIFF;
        }

        return MIME_UNKNOWN;
    }

    private static byte[] ascii(String text)
    {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix)
    {
        if (offset < 0 || data.length < offset + prefix.length) return false;

        for (int index = 0; index < prefix.length; index++)
        {
            if (data[offset + index] != prefix[index]) return false;
        }

        return true;
    }

    private void parseJpeg(byte[] data)
    {
        int position = 2;

        while (position + 4 <= data.length)
        {
            if ((data[position] & 0xff) != MARKER_PREFIX) return;

            int marker = data[position + 1] & 0xff;

            if (marker == MARKER_PREFIX)
            {
                position++;
                continue;
            }

            if (marker == MARKER_SOS || marker == MARKER_EOI) return;

            int length = ((data[position + 2] & 0xff) << 8) | (data[position + 3] & 0xff);

            if (length < 2 || position + 2 + length > data.length) return;

            int body = position + 4;
            int bodyLength = length - 2;

            if (marker == MARKER_APP1)
            {
                if (startsWith(data, body, EXIF_IDENTIFIER) && findAttribute(TAG_EXIF_VERSION) == null && !hasAnyAttribute())
                {
                    parseTiff(data, body + EXIF_IDENTIFIER.length, bodyLength - EXIF_IDENTIFIER.length);
                }
                else if (startsWith(data, body, XMP_IDENTIFIER) && findAttribute(TAG_XMP) == null)
                {
                    int start = body + XMP_IDENTIFIER.length;
                    byte[] xmp = Arrays.copyOfRange(data, start, body + bodyLength);
                    mAttributes.get(IFD_PRIMARY).put(TAG_XMP, new Attribute(FORMAT_BYTE, xmp.length, xmp, start));
                    mXmpFromSegment = true;
                }
            }

            position += 2 + length;
        }
    }

    private boolean hasAnyAttribute()
    {
        for (Map<String, Attribute> attributes : mAttributes)
        {
            for (String name : attributes.keySet())
            {
                if (!TAG_XMP.equals(name)) return true;
            }
        }

        return false;
    }

    private void parsePng(byte[] data)
    {
        int position = PNG_SIGNATURE.length;

        while (position + 12 <= data.length)
        {
            int length = readInt(data, position, ByteOrder.BIG_ENDIAN);

            if (length < 0 || position + 12L + length > data.length) return;

            if (startsWith(data, position + 4, ascii("eXIf")))
            {
                int start = position + 8;
                int offset = startsWith(data, start, EXIF_IDENTIFIER) ? EXIF_IDENTIFIER.length : 0;
                parseTiff(data, start + offset, length - offset);
                return;
            }

            if (startsWith(data, position + 4, ascii("IEND"))) return;

            position += 12 + length;
        }
    }

    private void parseWebp(byte[] data)
    {
        int position = 12;

        while (position + 8 <= data.length)
        {
            int length = readInt(data, position + 4, ByteOrder.LITTLE_ENDIAN);

            if (length < 0 || position + 8L + length > data.length) return;

            if (startsWith(data, position, ascii("EXIF")))
            {
                int start = position + 8;
                int offset = startsWith(data, start, EXIF_IDENTIFIER) ? EXIF_IDENTIFIER.length : 0;
                parseTiff(data, start + offset, length - offset);
                return;
            }

            position += 8 + length + (length & 1);
        }
    }

    private void parseTiff(byte[] data, int tiffStart, int tiffLength)
    {
        if (tiffLength < 8 || tiffStart + tiffLength > data.length) return;

        if (data[tiffStart] == 'I' && data[tiffStart + 1] == 'I')
        {
            mByteOrder = ByteOrder.LITTLE_ENDIAN;
        }
        else if (data[tiffStart] == 'M' && data[tiffStart + 1] == 'M')
        {
            mByteOrder = ByteOrder.BIG_ENDIAN;
        }
        else
        {
            return;
        }

        TiffReader reader = new TiffReader(data, tiffStart, tiffLength, mByteOrder);
        long firstIfd = reader.readUnsignedInt(4);
        Set<Long> visited = new HashSet<>();
        long nextIfd = readIfd(reader, firstIfd, IFD_PRIMARY, visited);

        if (nextIfd > 0) readIfd(reader, nextIfd, IFD_THUMBNAIL, visited);

        Map<String, Attribute> thumbnail = mAttributes.get(IFD_THUMBNAIL);
        Attribute thumbnailOffset = thumbnail.get(TAG_JPEG_INTERCHANGE_FORMAT);
        Attribute thumbnailLength = thumbnail.get(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

        if (thumbnailOffset != null && thumbnailLength != null)
        {
            try
            {
                long offset = thumbnailOffset.intValue(mByteOrder) & 0xffffffffL;
                long length = thumbnailLength.intValue(mByteOrder) & 0xffffffffL;

                if (length > 0 && offset + length <= tiffLength)
                {
                    mThumbnailBytes = Arrays.copyOfRange(data, (int)(tiffStart + offset), (int)(tiffStart + offset + length));
                    mThumbnailOffset = tiffStart + offset;
                    mThumbnailCompressed = true;
                }
            }
            catch (NumberFormatException ignored)
            {
                mThumbnailBytes = null;
            }
        }
    }

    private long readIfd(TiffReader reader, long ifdOffset, int ifd, Set<Long> visited)
    {
        if (ifdOffset < 8 || ifdOffset + 2 > reader.mLength || !visited.add(ifdOffset)) return 0;

        int count = reader.readUnsignedShort((int)ifdOffset);

        if (ifdOffset + 2 + count * 12L + 4 > reader.mLength) return 0;

        Map<Integer, Tag> definitions = TAGS_BY_NUMBER.get(ifd);

        for (int index = 0; index < count; index++)
        {
            int entry = (int)(ifdOffset + 2 + index * 12L);
            int number = reader.readUnsignedShort(entry);
            int format = reader.readUnsignedShort(entry + 2);
            long components = reader.readUnsignedInt(entry + 4);

            if (format < FORMAT_BYTE || format > FORMAT_DOUBLE) continue;

            long byteCount = components * FORMAT_SIZES[format];

            if (byteCount < 0 || byteCount > Integer.MAX_VALUE) continue;

            long valueOffset = byteCount > 4 ? reader.readUnsignedInt(entry + 8) : entry + 8;

            if (valueOffset + byteCount > reader.mLength) continue;

            if (number == POINTER_EXIF || number == POINTER_GPS || number == POINTER_INTEROPERABILITY)
            {
                if (ifd == IFD_THUMBNAIL) continue;

                long target = format == FORMAT_USHORT ? reader.readUnsignedShort(entry + 8) : reader.readUnsignedInt(entry + 8);
                int child = number == POINTER_EXIF ? IFD_EXIF : number == POINTER_GPS ? IFD_GPS : IFD_INTEROPERABILITY;
                readIfd(reader, target, child, visited);
                continue;
            }

            Tag definition = definitions.get(number);

            if (definition == null) continue;

            byte[] bytes = reader.copy((int)valueOffset, (int)byteCount);
            mAttributes.get(ifd).put(definition.mName,
                new Attribute(format, (int)components, bytes, reader.mStart + valueOffset));
        }

        return reader.readUnsignedInt((int)(ifdOffset + 2 + count * 12L));
    }

    private static int readInt(byte[] data, int offset, ByteOrder order)
    {
        return ByteBuffer.wrap(data, offset, 4).order(order).getInt();
    }

    private Attribute encode(Tag definition, String value)
    {
        int format = definition.mFormat;

        try
        {
            if (format == FORMAT_STRING)
            {
                byte[] text = (value + '\0').getBytes(StandardCharsets.US_ASCII);
                return new Attribute(FORMAT_STRING, text.length, text, -1);
            }

            if (format == FORMAT_UNDEFINED)
            {
                byte[] text = value.getBytes(StandardCharsets.US_ASCII);
                return new Attribute(FORMAT_UNDEFINED, text.length, text, -1);
            }

            String[] parts = value.split(",", -1);

            if (format == FORMAT_BYTE || format == FORMAT_SBYTE)
            {
                if (!value.matches("-?\\d+(,-?\\d+)*"))
                {
                    byte[] text = value.getBytes(StandardCharsets.US_ASCII);
                    return new Attribute(format, text.length, text, -1);
                }

                byte[] values = new byte[parts.length];

                for (int index = 0; index < parts.length; index++)
                {
                    values[index] = (byte)Integer.parseInt(parts[index].trim());
                }

                return new Attribute(format, values.length, values, -1);
            }

            if (format == FORMAT_URATIONAL || format == FORMAT_SRATIONAL)
            {
                ByteBuffer buffer = ByteBuffer.allocate(parts.length * 8).order(mByteOrder);

                for (String part : parts)
                {
                    long[] rational = parseRational(part.trim());
                    buffer.putInt((int)rational[0]);
                    buffer.putInt((int)rational[1]);
                }

                return new Attribute(format, parts.length, buffer.array(), -1);
            }

            long[] numbers = new long[parts.length];
            boolean fitsShort = true;

            for (int index = 0; index < parts.length; index++)
            {
                String part = parts[index].trim();
                numbers[index] = part.contains("/") ? Math.round(rationalToDouble(parseRational(part))) : Long.parseLong(part);

                if (numbers[index] < 0 || numbers[index] > 0xffff) fitsShort = false;
            }

            int target = format;

            if (format == FORMAT_USHORT && !fitsShort) target = definition.mSecondaryFormat == FORMAT_ULONG ? FORMAT_ULONG : FORMAT_SLONG;

            ByteBuffer buffer = ByteBuffer.allocate(numbers.length * FORMAT_SIZES[target]).order(mByteOrder);

            for (long number : numbers)
            {
                if (FORMAT_SIZES[target] == 2)
                {
                    buffer.putShort((short)number);
                }
                else
                {
                    buffer.putInt((int)number);
                }
            }

            return new Attribute(target, numbers.length, buffer.array(), -1);
        }
        catch (NumberFormatException failure)
        {
            android.util.Log.w("ExifInterface", "Invalid value for " + definition.mName + " : " + value);
            return null;
        }
    }

    private static long[] parseRational(String text)
    {
        int slash = text.indexOf('/');

        if (slash >= 0)
        {
            return new long[] {(long)Double.parseDouble(text.substring(0, slash).trim()),
                (long)Double.parseDouble(text.substring(slash + 1).trim())};
        }

        double value = Double.parseDouble(text);

        return new long[] {Math.round(value * 10000), 10000};
    }

    private static double rationalToDouble(long[] rational)
    {
        if (rational[1] == 0) throw new NumberFormatException("Division by zero");

        return (double)rational[0] / rational[1];
    }

    private static float convertRationalLatLonToFloat(String rationalString, String reference)
    {
        try
        {
            String[] parts = rationalString.split(",", -1);

            if (parts.length < 3) throw new IllegalArgumentException("Invalid coordinate");

            double degrees = rationalToDouble(parseRational(parts[0].trim()));
            double minutes = rationalToDouble(parseRational(parts[1].trim()));
            double seconds = rationalToDouble(parseRational(parts[2].trim()));
            double result = degrees + minutes / 60.0 + seconds / 3600.0;

            if (reference.equals("S") || reference.equals("W")) return (float)-result;
            if (reference.equals("N") || reference.equals("E")) return (float)result;

            throw new IllegalArgumentException("Invalid coordinate reference");
        }
        catch (NumberFormatException failure)
        {
            throw new IllegalArgumentException(failure);
        }
    }

    private static long parseDateTime(String dateTime, String subSecond, String offset)
    {
        if (dateTime == null || !dateTime.matches("\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}.*")) return -1;

        SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        java.util.Date parsed = format.parse(dateTime, new ParsePosition(0));

        if (parsed == null) return -1;

        long millis = parsed.getTime();

        if (offset != null && offset.matches("[+-]\\d{2}:\\d{2}"))
        {
            int sign = offset.charAt(0) == '-' ? -1 : 1;
            int hours = Integer.parseInt(offset.substring(1, 3));
            int minutes = Integer.parseInt(offset.substring(4, 6));
            millis -= sign * (hours * 3600000L + minutes * 60000L);
        }

        if (subSecond != null)
        {
            String digits = subSecond.trim();

            if (digits.matches("\\d+"))
            {
                while (digits.length() < 3)
                {
                    digits = digits + "0";
                }

                millis += Long.parseLong(digits.substring(0, 3));
            }
        }

        return millis;
    }

    private byte[] buildTiff()
    {
        List<Map<String, Attribute>> ifds = new ArrayList<>();

        for (int ifd = 0; ifd < IFD_COUNT; ifd++)
        {
            Map<String, Attribute> copy = new LinkedHashMap<>(mAttributes.get(ifd));

            if (ifd == IFD_PRIMARY && mXmpFromSegment) copy.remove(TAG_XMP);

            if (ifd == IFD_THUMBNAIL)
            {
                copy.remove(TAG_JPEG_INTERCHANGE_FORMAT);
                copy.remove(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
            }

            ifds.add(copy);
        }

        boolean hasExif = !ifds.get(IFD_EXIF).isEmpty() || !ifds.get(IFD_INTEROPERABILITY).isEmpty();
        boolean hasGps = !ifds.get(IFD_GPS).isEmpty();
        boolean hasInteroperability = !ifds.get(IFD_INTEROPERABILITY).isEmpty();
        boolean hasThumbnail = mThumbnailBytes != null && mThumbnailCompressed;

        if (hasThumbnail || !ifds.get(IFD_THUMBNAIL).isEmpty()) hasThumbnail = mThumbnailBytes != null;

        List<List<Entry>> entries = new ArrayList<>();

        for (int ifd = 0; ifd < IFD_COUNT; ifd++)
        {
            List<Entry> list = new ArrayList<>();
            Map<String, Tag> definitions = TAGS_BY_NAME.get(ifd);

            for (Map.Entry<String, Attribute> attribute : ifds.get(ifd).entrySet())
            {
                Tag definition = definitions.get(attribute.getKey());

                if (definition != null) list.add(new Entry(definition.mNumber, attribute.getValue()));
            }

            entries.add(list);
        }

        Entry exifPointer = new Entry(POINTER_EXIF, pointerAttribute());
        Entry gpsPointer = new Entry(POINTER_GPS, pointerAttribute());
        Entry interoperabilityPointer = new Entry(POINTER_INTEROPERABILITY, pointerAttribute());
        Entry thumbnailOffset = new Entry(TAG_NUMBER_JPEG_INTERCHANGE_FORMAT, pointerAttribute());
        Entry thumbnailLength = new Entry(TAG_NUMBER_JPEG_INTERCHANGE_FORMAT_LENGTH, pointerAttribute());

        if (hasExif) entries.get(IFD_PRIMARY).add(exifPointer);
        if (hasGps) entries.get(IFD_PRIMARY).add(gpsPointer);
        if (hasInteroperability) entries.get(IFD_EXIF).add(interoperabilityPointer);

        if (hasThumbnail)
        {
            entries.get(IFD_THUMBNAIL).add(thumbnailOffset);
            entries.get(IFD_THUMBNAIL).add(thumbnailLength);
        }

        boolean[] written = new boolean[IFD_COUNT];
        written[IFD_PRIMARY] = true;
        written[IFD_EXIF] = hasExif;
        written[IFD_GPS] = hasGps;
        written[IFD_INTEROPERABILITY] = hasInteroperability;
        written[IFD_THUMBNAIL] = hasThumbnail || !entries.get(IFD_THUMBNAIL).isEmpty();

        int[] order = {IFD_PRIMARY, IFD_EXIF, IFD_GPS, IFD_INTEROPERABILITY, IFD_THUMBNAIL};
        int[] offsets = new int[IFD_COUNT];
        int position = 8;

        for (int ifd : order)
        {
            if (!written[ifd]) continue;

            entries.get(ifd).sort((first, second) -> Integer.compare(first.mNumber, second.mNumber));
            offsets[ifd] = position;
            position += ifdSize(entries.get(ifd));
        }

        int thumbnailPosition = position;

        setPointer(exifPointer, offsets[IFD_EXIF]);
        setPointer(gpsPointer, offsets[IFD_GPS]);
        setPointer(interoperabilityPointer, offsets[IFD_INTEROPERABILITY]);

        if (hasThumbnail)
        {
            setPointer(thumbnailOffset, thumbnailPosition);
            setPointer(thumbnailLength, mThumbnailBytes.length);
            position += mThumbnailBytes.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(position).order(mByteOrder);
        buffer.put(mByteOrder == ByteOrder.LITTLE_ENDIAN ? (byte)'I' : (byte)'M');
        buffer.put(mByteOrder == ByteOrder.LITTLE_ENDIAN ? (byte)'I' : (byte)'M');
        buffer.putShort((short)0x2a);
        buffer.putInt(8);

        for (int ifd : order)
        {
            if (!written[ifd]) continue;

            int next = ifd == IFD_PRIMARY && written[IFD_THUMBNAIL] ? offsets[IFD_THUMBNAIL] : 0;
            writeIfd(buffer, entries.get(ifd), offsets[ifd], next);
        }

        if (hasThumbnail)
        {
            buffer.position(thumbnailPosition);
            buffer.put(mThumbnailBytes);
        }

        return buffer.array();
    }

    private Attribute pointerAttribute()
    {
        return new Attribute(FORMAT_ULONG, 1, new byte[4], -1);
    }

    private void setPointer(Entry entry, int value)
    {
        ByteBuffer.wrap(entry.mAttribute.mBytes).order(mByteOrder).putInt(value);
    }

    private static int ifdSize(List<Entry> entries)
    {
        int size = 2 + entries.size() * 12 + 4;

        for (Entry entry : entries)
        {
            int length = entry.mAttribute.mBytes.length;

            if (length > 4) size += length + (length & 1);
        }

        return size;
    }

    private static void writeIfd(ByteBuffer buffer, List<Entry> entries, int offset, int nextIfd)
    {
        int dataPosition = offset + 2 + entries.size() * 12 + 4;
        buffer.position(offset);
        buffer.putShort((short)entries.size());

        for (Entry entry : entries)
        {
            Attribute attribute = entry.mAttribute;
            buffer.putShort((short)entry.mNumber);
            buffer.putShort((short)attribute.mFormat);
            buffer.putInt(attribute.mComponents);

            if (attribute.mBytes.length > 4)
            {
                buffer.putInt(dataPosition);
                int saved = buffer.position();
                buffer.position(dataPosition);
                buffer.put(attribute.mBytes);
                dataPosition += attribute.mBytes.length + (attribute.mBytes.length & 1);
                buffer.position(saved);
            }
            else
            {
                byte[] inline = Arrays.copyOf(attribute.mBytes, 4);
                buffer.put(inline);
            }
        }

        buffer.putInt(nextIfd);
    }

    private static byte[] rewriteJpeg(byte[] original, byte[] tiff) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream(original.length + tiff.length + 16);
        output.write(MARKER_PREFIX);
        output.write(MARKER_SOI);

        int segmentLength = 2 + EXIF_IDENTIFIER.length + tiff.length;

        if (segmentLength > MAX_SEGMENT_LENGTH) throw new IOException("Size of exif data (" + tiff.length + " bytes) exceeds 64KB");

        output.write(MARKER_PREFIX);
        output.write(MARKER_APP1);
        output.write(segmentLength >> 8);
        output.write(segmentLength & 0xff);
        output.write(EXIF_IDENTIFIER);
        output.write(tiff);

        int position = 2;

        while (position < original.length)
        {
            if (position + 4 > original.length || (original[position] & 0xff) != MARKER_PREFIX)
            {
                throw new IOException("Invalid marker in JPEG stream");
            }

            int marker = original[position + 1] & 0xff;

            if (marker == MARKER_PREFIX)
            {
                position++;
                continue;
            }

            if (marker == MARKER_SOS || marker == MARKER_EOI)
            {
                output.write(original, position, original.length - position);
                return output.toByteArray();
            }

            int length = ((original[position + 2] & 0xff) << 8) | (original[position + 3] & 0xff);

            if (length < 2 || position + 2 + length > original.length) throw new IOException("Invalid JPEG segment length");

            boolean exifSegment = marker == MARKER_APP1 && startsWith(original, position + 4, EXIF_IDENTIFIER);

            if (!exifSegment) output.write(original, position, 2 + length);

            position += 2 + length;
        }

        return output.toByteArray();
    }

    private static byte[] rewritePng(byte[] original, byte[] tiff) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream(original.length + tiff.length + 16);
        output.write(PNG_SIGNATURE);

        int position = PNG_SIGNATURE.length;
        boolean inserted = false;

        while (position + 12 <= original.length)
        {
            int length = readInt(original, position, ByteOrder.BIG_ENDIAN);

            if (length < 0 || position + 12L + length > original.length) throw new IOException("Invalid PNG chunk");

            boolean exifChunk = startsWith(original, position + 4, ascii("eXIf"));

            if (!exifChunk) output.write(original, position, 12 + length);

            position += 12 + length;

            if (!inserted)
            {
                ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
                header.putInt(tiff.length);
                header.put(ascii("eXIf"));
                output.write(header.array());
                output.write(tiff);

                CRC32 checksum = new CRC32();
                checksum.update(ascii("eXIf"));
                checksum.update(tiff);
                ByteBuffer crc = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                crc.putInt((int)checksum.getValue());
                output.write(crc.array());
                inserted = true;
            }
        }

        if (position < original.length) output.write(original, position, original.length - position);

        return output.toByteArray();
    }

    private static final class Tag
    {
        final String mName;
        final int mNumber;
        final int mFormat;
        final int mSecondaryFormat;

        Tag(String name, int number, int format)
        {
            this(name, number, format, -1);
        }

        Tag(String name, int number, int format, int secondaryFormat)
        {
            mName = name;
            mNumber = number;
            mFormat = format;
            mSecondaryFormat = secondaryFormat;
        }
    }

    private static final class Entry
    {
        final int mNumber;
        final Attribute mAttribute;

        Entry(int number, Attribute attribute)
        {
            mNumber = number;
            mAttribute = attribute;
        }
    }

    private static final class TiffReader
    {
        final byte[] mData;
        final int mStart;
        final int mLength;
        final ByteOrder mOrder;

        TiffReader(byte[] data, int start, int length, ByteOrder order)
        {
            mData = data;
            mStart = start;
            mLength = length;
            mOrder = order;
        }

        int readUnsignedShort(int offset)
        {
            if (offset < 0 || offset + 2 > mLength) return 0;

            return ByteBuffer.wrap(mData, mStart + offset, 2).order(mOrder).getShort() & 0xffff;
        }

        long readUnsignedInt(int offset)
        {
            if (offset < 0 || offset + 4 > mLength) return 0;

            return ByteBuffer.wrap(mData, mStart + offset, 4).order(mOrder).getInt() & 0xffffffffL;
        }

        byte[] copy(int offset, int length)
        {
            return Arrays.copyOfRange(mData, mStart + offset, mStart + offset + length);
        }
    }

    private static final class Attribute
    {
        final int mFormat;
        final int mComponents;
        final byte[] mBytes;
        final long mOffset;

        Attribute(int format, int components, byte[] bytes, long offset)
        {
            mFormat = format;
            mComponents = components;
            mBytes = bytes;
            mOffset = offset;
        }

        long[] longValues(ByteOrder order)
        {
            ByteBuffer buffer = ByteBuffer.wrap(mBytes).order(order);
            int count = mFormat == FORMAT_URATIONAL || mFormat == FORMAT_SRATIONAL ? mComponents * 2 : mComponents;
            long[] values = new long[count];

            for (int index = 0; index < count; index++)
            {
                switch (mFormat)
                {
                    case FORMAT_BYTE:
                    case FORMAT_UNDEFINED:
                    case FORMAT_STRING:
                        values[index] = buffer.get() & 0xff;
                        break;
                    case FORMAT_SBYTE:
                        values[index] = buffer.get();
                        break;
                    case FORMAT_USHORT:
                        values[index] = buffer.getShort() & 0xffff;
                        break;
                    case FORMAT_SSHORT:
                        values[index] = buffer.getShort();
                        break;
                    case FORMAT_ULONG:
                    case FORMAT_URATIONAL:
                        values[index] = buffer.getInt() & 0xffffffffL;
                        break;
                    case FORMAT_SLONG:
                    case FORMAT_SRATIONAL:
                        values[index] = buffer.getInt();
                        break;
                    case FORMAT_SINGLE:
                        values[index] = (long)buffer.getFloat();
                        break;
                    default:
                        values[index] = (long)buffer.getDouble();
                        break;
                }
            }

            return values;
        }

        String stringValue(ByteOrder order)
        {
            if (mFormat == FORMAT_STRING) return asciiValue();

            if (mFormat == FORMAT_BYTE || mFormat == FORMAT_SBYTE || mFormat == FORMAT_UNDEFINED)
            {
                if (isPrintable()) return asciiValue();

                return joinNumbers(longValues(order));
            }

            if (mFormat == FORMAT_URATIONAL || mFormat == FORMAT_SRATIONAL)
            {
                long[] values = longValues(order);
                StringBuilder builder = new StringBuilder();

                for (int index = 0; index + 1 < values.length; index += 2)
                {
                    if (builder.length() > 0) builder.append(',');

                    builder.append(values[index]).append('/').append(values[index + 1]);
                }

                return builder.toString();
            }

            if (mFormat == FORMAT_SINGLE || mFormat == FORMAT_DOUBLE)
            {
                ByteBuffer buffer = ByteBuffer.wrap(mBytes).order(order);
                StringBuilder builder = new StringBuilder();

                for (int index = 0; index < mComponents; index++)
                {
                    if (builder.length() > 0) builder.append(',');

                    builder.append(mFormat == FORMAT_SINGLE ? (double)buffer.getFloat() : buffer.getDouble());
                }

                return builder.toString();
            }

            return joinNumbers(longValues(order));
        }

        int intValue(ByteOrder order)
        {
            if (mFormat == FORMAT_STRING) return Integer.parseInt(asciiValue().trim());

            if (mFormat == FORMAT_URATIONAL || mFormat == FORMAT_SRATIONAL)
            {
                long[] values = longValues(order);

                if (values.length < 2) throw new NumberFormatException("Empty rational");

                return (int)rationalToDouble(new long[] {values[0], values[1]});
            }

            if ((mFormat == FORMAT_BYTE || mFormat == FORMAT_UNDEFINED) && mComponents > 1 && isPrintable())
            {
                return Integer.parseInt(asciiValue().trim());
            }

            long[] values = longValues(order);

            if (values.length == 0) throw new NumberFormatException("Empty value");

            return (int)values[0];
        }

        double doubleValue(ByteOrder order)
        {
            if (mFormat == FORMAT_STRING) return Double.parseDouble(asciiValue().trim());

            if (mFormat == FORMAT_URATIONAL || mFormat == FORMAT_SRATIONAL)
            {
                long[] values = longValues(order);

                if (values.length < 2) throw new NumberFormatException("Empty rational");

                return rationalToDouble(new long[] {values[0], values[1]});
            }

            if (mFormat == FORMAT_SINGLE) return ByteBuffer.wrap(mBytes).order(order).getFloat();
            if (mFormat == FORMAT_DOUBLE) return ByteBuffer.wrap(mBytes).order(order).getDouble();

            return intValue(order);
        }

        private boolean isPrintable()
        {
            if (mBytes.length == 0) return false;

            for (int index = 0; index < mBytes.length; index++)
            {
                int value = mBytes[index] & 0xff;

                if (value == 0 && index > 0) continue;
                if (value < 0x20 || value > 0x7e) return false;
            }

            return true;
        }

        private String asciiValue()
        {
            int end = 0;

            while (end < mBytes.length && mBytes[end] != 0)
            {
                end++;
            }

            return new String(mBytes, 0, end, StandardCharsets.US_ASCII);
        }

        private static String joinNumbers(long[] values)
        {
            StringBuilder builder = new StringBuilder();

            for (long value : values)
            {
                if (builder.length() > 0) builder.append(',');

                builder.append(value);
            }

            return builder.toString();
        }
    }
}
