package android.hardware;

public final class Sensor
{
    public static final int TYPE_ACCELEROMETER = 1;
    public static final int TYPE_MAGNETIC_FIELD = 2;
    public static final int TYPE_ORIENTATION = 3;
    public static final int TYPE_GYROSCOPE = 4;
    public static final int TYPE_LIGHT = 5;
    public static final int TYPE_PRESSURE = 6;
    public static final int TYPE_TEMPERATURE = 7;
    public static final int TYPE_PROXIMITY = 8;
    public static final int TYPE_GRAVITY = 9;
    public static final int TYPE_LINEAR_ACCELERATION = 10;
    public static final int TYPE_ROTATION_VECTOR = 11;
    public static final int TYPE_RELATIVE_HUMIDITY = 12;
    public static final int TYPE_AMBIENT_TEMPERATURE = 13;
    public static final int TYPE_MAGNETIC_FIELD_UNCALIBRATED = 14;
    public static final int TYPE_GAME_ROTATION_VECTOR = 15;
    public static final int TYPE_GYROSCOPE_UNCALIBRATED = 16;
    public static final int TYPE_SIGNIFICANT_MOTION = 17;
    public static final int TYPE_STEP_DETECTOR = 18;
    public static final int TYPE_STEP_COUNTER = 19;
    public static final int TYPE_GEOMAGNETIC_ROTATION_VECTOR = 20;
    public static final int TYPE_HEART_RATE = 21;
    public static final int TYPE_POSE_6DOF = 28;
    public static final int TYPE_STATIONARY_DETECT = 29;
    public static final int TYPE_MOTION_DETECT = 30;
    public static final int TYPE_HEART_BEAT = 31;
    public static final int TYPE_LOW_LATENCY_OFFBODY_DETECT = 34;
    public static final int TYPE_ACCELEROMETER_UNCALIBRATED = 35;
    public static final int TYPE_HINGE_ANGLE = 36;
    public static final int TYPE_HEAD_TRACKER = 37;
    public static final int TYPE_ACCELEROMETER_LIMITED_AXES = 38;
    public static final int TYPE_GYROSCOPE_LIMITED_AXES = 39;
    public static final int TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED = 40;
    public static final int TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED = 41;
    public static final int TYPE_HEADING = 42;
    public static final int TYPE_DEVICE_PRIVATE_BASE = 65536;
    public static final int TYPE_ALL = -1;

    public static final String STRING_TYPE_ACCELEROMETER = "android.sensor.accelerometer";
    public static final String STRING_TYPE_ACCELEROMETER_LIMITED_AXES = "android.sensor.accelerometer_limited_axes";
    public static final String STRING_TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED =
        "android.sensor.accelerometer_limited_axes_uncalibrated";
    public static final String STRING_TYPE_ACCELEROMETER_UNCALIBRATED = "android.sensor.accelerometer_uncalibrated";
    public static final String STRING_TYPE_AMBIENT_TEMPERATURE = "android.sensor.ambient_temperature";
    public static final String STRING_TYPE_GAME_ROTATION_VECTOR = "android.sensor.game_rotation_vector";
    public static final String STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR = "android.sensor.geomagnetic_rotation_vector";
    public static final String STRING_TYPE_GRAVITY = "android.sensor.gravity";
    public static final String STRING_TYPE_GYROSCOPE = "android.sensor.gyroscope";
    public static final String STRING_TYPE_GYROSCOPE_LIMITED_AXES = "android.sensor.gyroscope_limited_axes";
    public static final String STRING_TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED = "android.sensor.gyroscope_limited_axes_uncalibrated";
    public static final String STRING_TYPE_GYROSCOPE_UNCALIBRATED = "android.sensor.gyroscope_uncalibrated";
    public static final String STRING_TYPE_HEADING = "android.sensor.heading";
    public static final String STRING_TYPE_HEAD_TRACKER = "android.sensor.head_tracker";
    public static final String STRING_TYPE_HEART_BEAT = "android.sensor.heart_beat";
    public static final String STRING_TYPE_HEART_RATE = "android.sensor.heart_rate";
    public static final String STRING_TYPE_HINGE_ANGLE = "android.sensor.hinge_angle";
    public static final String STRING_TYPE_LIGHT = "android.sensor.light";
    public static final String STRING_TYPE_LINEAR_ACCELERATION = "android.sensor.linear_acceleration";
    public static final String STRING_TYPE_LOW_LATENCY_OFFBODY_DETECT = "android.sensor.low_latency_offbody_detect";
    public static final String STRING_TYPE_MAGNETIC_FIELD = "android.sensor.magnetic_field";
    public static final String STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED = "android.sensor.magnetic_field_uncalibrated";
    public static final String STRING_TYPE_MOTION_DETECT = "android.sensor.motion_detect";
    public static final String STRING_TYPE_ORIENTATION = "android.sensor.orientation";
    public static final String STRING_TYPE_POSE_6DOF = "android.sensor.pose_6dof";
    public static final String STRING_TYPE_PRESSURE = "android.sensor.pressure";
    public static final String STRING_TYPE_PROXIMITY = "android.sensor.proximity";
    public static final String STRING_TYPE_RELATIVE_HUMIDITY = "android.sensor.relative_humidity";
    public static final String STRING_TYPE_ROTATION_VECTOR = "android.sensor.rotation_vector";
    public static final String STRING_TYPE_SIGNIFICANT_MOTION = "android.sensor.significant_motion";
    public static final String STRING_TYPE_STATIONARY_DETECT = "android.sensor.stationary_detect";
    public static final String STRING_TYPE_STEP_COUNTER = "android.sensor.step_counter";
    public static final String STRING_TYPE_STEP_DETECTOR = "android.sensor.step_detector";
    public static final String STRING_TYPE_TEMPERATURE = "android.sensor.temperature";

    public static final int REPORTING_MODE_CONTINUOUS = 0;
    public static final int REPORTING_MODE_ON_CHANGE = 1;
    public static final int REPORTING_MODE_ONE_SHOT = 2;
    public static final int REPORTING_MODE_SPECIAL_TRIGGER = 3;

    private final int mType;
    private final String mName;
    private final int mId;
    private final boolean mWakeUp;

    Sensor(int type, String name)
    {
        this(type, name, type, false);
    }

    Sensor(int type, String name, int id, boolean wakeUp)
    {
        mType = type;
        mName = name;
        mId = id;
        mWakeUp = wakeUp;
    }

    public int getType()
    {
        return mType;
    }

    public String getName()
    {
        return mName;
    }

    public String getVendor()
    {
        return "";
    }

    public int getVersion()
    {
        return 1;
    }

    public float getMaximumRange()
    {
        return 0.0f;
    }

    public float getResolution()
    {
        return 0.0f;
    }

    public float getPower()
    {
        return 0.0f;
    }

    public int getMinDelay()
    {
        return 0;
    }

    public int getMaxDelay()
    {
        return 0;
    }

    public int getFifoReservedEventCount()
    {
        return 0;
    }

    public int getFifoMaxEventCount()
    {
        return 0;
    }

    public String getStringType()
    {
        switch (mType)
        {
            case TYPE_ACCELEROMETER: return STRING_TYPE_ACCELEROMETER;
            case TYPE_MAGNETIC_FIELD: return STRING_TYPE_MAGNETIC_FIELD;
            case TYPE_ORIENTATION: return STRING_TYPE_ORIENTATION;
            case TYPE_GYROSCOPE: return STRING_TYPE_GYROSCOPE;
            case TYPE_LIGHT: return STRING_TYPE_LIGHT;
            case TYPE_PRESSURE: return STRING_TYPE_PRESSURE;
            case TYPE_TEMPERATURE: return STRING_TYPE_TEMPERATURE;
            case TYPE_PROXIMITY: return STRING_TYPE_PROXIMITY;
            case TYPE_GRAVITY: return STRING_TYPE_GRAVITY;
            case TYPE_LINEAR_ACCELERATION: return STRING_TYPE_LINEAR_ACCELERATION;
            case TYPE_ROTATION_VECTOR: return STRING_TYPE_ROTATION_VECTOR;
            case TYPE_RELATIVE_HUMIDITY: return STRING_TYPE_RELATIVE_HUMIDITY;
            case TYPE_AMBIENT_TEMPERATURE: return STRING_TYPE_AMBIENT_TEMPERATURE;
            case TYPE_MAGNETIC_FIELD_UNCALIBRATED: return STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED;
            case TYPE_GAME_ROTATION_VECTOR: return STRING_TYPE_GAME_ROTATION_VECTOR;
            case TYPE_GYROSCOPE_UNCALIBRATED: return STRING_TYPE_GYROSCOPE_UNCALIBRATED;
            case TYPE_SIGNIFICANT_MOTION: return STRING_TYPE_SIGNIFICANT_MOTION;
            case TYPE_STEP_DETECTOR: return STRING_TYPE_STEP_DETECTOR;
            case TYPE_STEP_COUNTER: return STRING_TYPE_STEP_COUNTER;
            case TYPE_GEOMAGNETIC_ROTATION_VECTOR: return STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR;
            case TYPE_HEART_RATE: return STRING_TYPE_HEART_RATE;
            case TYPE_POSE_6DOF: return STRING_TYPE_POSE_6DOF;
            case TYPE_STATIONARY_DETECT: return STRING_TYPE_STATIONARY_DETECT;
            case TYPE_MOTION_DETECT: return STRING_TYPE_MOTION_DETECT;
            case TYPE_HEART_BEAT: return STRING_TYPE_HEART_BEAT;
            case TYPE_LOW_LATENCY_OFFBODY_DETECT: return STRING_TYPE_LOW_LATENCY_OFFBODY_DETECT;
            case TYPE_ACCELEROMETER_UNCALIBRATED: return STRING_TYPE_ACCELEROMETER_UNCALIBRATED;
            case TYPE_HINGE_ANGLE: return STRING_TYPE_HINGE_ANGLE;
            case TYPE_HEAD_TRACKER: return STRING_TYPE_HEAD_TRACKER;
            case TYPE_ACCELEROMETER_LIMITED_AXES: return STRING_TYPE_ACCELEROMETER_LIMITED_AXES;
            case TYPE_GYROSCOPE_LIMITED_AXES: return STRING_TYPE_GYROSCOPE_LIMITED_AXES;
            case TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED: return STRING_TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED;
            case TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED: return STRING_TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED;
            case TYPE_HEADING: return STRING_TYPE_HEADING;
            default: return "";
        }
    }

    public int getId()
    {
        return mId;
    }

    public int getReportingMode()
    {
        switch (mType)
        {
            case TYPE_LIGHT:
            case TYPE_PROXIMITY:
            case TYPE_RELATIVE_HUMIDITY:
            case TYPE_AMBIENT_TEMPERATURE:
            case TYPE_STEP_COUNTER:
            case TYPE_HEART_RATE:
            case TYPE_HINGE_ANGLE:
                return REPORTING_MODE_ON_CHANGE;
            case TYPE_SIGNIFICANT_MOTION:
            case TYPE_STATIONARY_DETECT:
            case TYPE_MOTION_DETECT:
                return REPORTING_MODE_ONE_SHOT;
            case TYPE_STEP_DETECTOR:
            case TYPE_HEART_BEAT:
            case TYPE_LOW_LATENCY_OFFBODY_DETECT:
                return REPORTING_MODE_SPECIAL_TRIGGER;
            default:
                return REPORTING_MODE_CONTINUOUS;
        }
    }

    public int getHighestDirectReportRateLevel()
    {
        return SensorDirectChannel.RATE_STOP;
    }

    public boolean isDirectChannelTypeSupported(int sharedMemType)
    {
        return false;
    }

    public boolean isWakeUpSensor()
    {
        return mWakeUp;
    }

    public boolean isDynamicSensor()
    {
        return false;
    }

    public boolean isAdditionalInfoSupported()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "{Sensor name=\"" + mName + "\", vendor=\"" + getVendor() + "\", version=" + getVersion() + ", type=" + mType
            + ", maxRange=" + getMaximumRange() + ", resolution=" + getResolution() + ", power=" + getPower() + ", minDelay="
            + getMinDelay() + "}";
    }
}
