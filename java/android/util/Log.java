package android.util;
import android.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
public final class Log
{
    @IntDef({ASSERT, ERROR, WARN, INFO, DEBUG, VERBOSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Level {}
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;
      /** @hide */ public static final int LOG_ID_MAIN = 0;
        /** @hide */ public static final int LOG_ID_RADIO = 1;
        /** @hide */ public static final int LOG_ID_EVENTS = 2;
        /** @hide */ public static final int LOG_ID_SYSTEM = 3;
        /** @hide */ public static final int LOG_ID_CRASH = 4;
    private Log()
    {
    }

    public static native int println_native(int bufferId, int priority, String tag, String message);

    public static int v(String tag, String message)
    {
        return println_native(0, VERBOSE, tag, message);
    }

    public static int v(String tag, String message, Throwable cause)
    {
        return println_native(0, VERBOSE, tag, message + '\n' + getStackTraceString(cause));
    }

    public static int d(String tag, String message)
    {
        return println_native(0, DEBUG, tag, message);
    }

    public static int d(String tag, String message, Throwable cause)
    {
        return println_native(0, DEBUG, tag, message + '\n' + getStackTraceString(cause));
    }

    public static int i(String tag, String message)
    {
        return println_native(0, INFO, tag, message);
    }

    public static int i(String tag, String message, Throwable cause)
    {
        return println_native(0, INFO, tag, message + '\n' + getStackTraceString(cause));
    }

    public static int w(String tag, String message)
    {
        return println_native(0, WARN, tag, message);
    }

    public static int w(String tag, String message, Throwable cause)
    {
        return println_native(0, WARN, tag, message + '\n' + getStackTraceString(cause));
    }

    public static int w(String tag, Throwable cause)
    {
        return println_native(0, WARN, tag, getStackTraceString(cause));
    }

    public static int e(String tag, String message)
    {
        return println_native(0, ERROR, tag, message);
    }

    public static int e(String tag, String message, Throwable cause)
    {
        return println_native(0, ERROR, tag, message + '\n' + getStackTraceString(cause));
    }

    public static int wtf(String tag, String message)
    {
        return println_native(0, ASSERT, tag, message);
    }

    public static int wtf(String tag, Throwable cause)
    {
        return println_native(0, ASSERT, tag, getStackTraceString(cause));
    }

    public static int wtf(String tag, String message, Throwable cause)
    {
        return println_native(0, ASSERT, tag, message + '\n' + getStackTraceString(cause));
    }

    public static int println(int priority, String tag, String message)
    {
        return println_native(0, priority, tag, message);
    }

    public static boolean isLoggable(String tag, int level)
    {
        return level >= INFO;
    }

    public static String getStackTraceString(Throwable cause)
    {
        if (cause == null) return "";

        java.io.StringWriter writer = new java.io.StringWriter();
        cause.printStackTrace(new java.io.PrintWriter(writer));
        return writer.toString();
    }
}
