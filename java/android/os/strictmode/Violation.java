package android.os.strictmode;

public abstract class Violation extends Throwable
{
    private int mHashCode;
    private boolean mHashCodeValid;

    Violation()
    {
        super();
    }

    Violation(String message)
    {
        super(message);
    }

    @Override
    public int hashCode()
    {
        synchronized (this)
        {
            if (mHashCodeValid) return mHashCode;

            String message = getMessage();
            Throwable cause = getCause();
            int hashCode = message != null ? message.hashCode() : getClass().hashCode();
            hashCode = hashCode * 37 + calcStackTraceHashCode(getStackTrace());
            hashCode = hashCode * 37 + (cause != null ? cause.toString().hashCode() : 0);
            mHashCodeValid = true;
            mHashCode = hashCode;
            return hashCode;
        }
    }

    @Override
    public synchronized Throwable initCause(Throwable cause)
    {
        mHashCodeValid = false;
        return super.initCause(cause);
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace)
    {
        super.setStackTrace(stackTrace);
        synchronized (this)
        {
            mHashCodeValid = false;
        }
    }

    @Override
    public synchronized Throwable fillInStackTrace()
    {
        mHashCodeValid = false;
        return super.fillInStackTrace();
    }

    private static int calcStackTraceHashCode(StackTraceElement[] stackTrace)
    {
        int hashCode = 17;
        if (stackTrace == null) return hashCode;

        for (StackTraceElement element : stackTrace)
        {
            if (element != null) hashCode = hashCode * 37 + element.hashCode();
        }
        return hashCode;
    }
}
