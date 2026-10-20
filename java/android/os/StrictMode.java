package android.os;

public final class StrictMode
{
    private static ThreadPolicy sThreadPolicy = ThreadPolicy.LAX;
    private static VmPolicy sVmPolicy = VmPolicy.LAX;

    private StrictMode()
    {
    }

    public static ThreadPolicy getThreadPolicy()
    {
        return sThreadPolicy;
    }

    public static void setThreadPolicy(ThreadPolicy policy)
    {
        if (policy != null) sThreadPolicy = policy;
    }

    public static VmPolicy getVmPolicy()
    {
        return sVmPolicy;
    }

    public static void setVmPolicy(VmPolicy policy)
    {
        if (policy != null) sVmPolicy = policy;
    }

    public static void enableDefaults()
    {
    }

    public static void noteSlowCall(String name)
    {
    }

    public static ThreadPolicy allowThreadDiskReads()
    {
        return sThreadPolicy;
    }

    public static ThreadPolicy allowThreadDiskWrites()
    {
        return sThreadPolicy;
    }

    public static VmPolicy allowVmViolations()
    {
        return sVmPolicy;
    }

    public interface OnThreadViolationListener
    {
        void onThreadViolation(Violation violation);
    }

    public interface OnVmViolationListener
    {
        void onVmViolation(Violation violation);
    }

    public static class Violation extends Throwable
    {
        Violation(String message)
        {
            super(message);
        }
    }

    public static final class ThreadPolicy
    {
        public static final ThreadPolicy LAX = new ThreadPolicy();

        public static final class Builder
        {
            public Builder()
            {
            }

            public Builder(ThreadPolicy policy)
            {
            }

            public Builder detectAll()
            {
                return this;
            }

            public Builder detectDiskReads()
            {
                return this;
            }

            public Builder detectDiskWrites()
            {
                return this;
            }

            public Builder detectNetwork()
            {
                return this;
            }

            public Builder detectCustomSlowCalls()
            {
                return this;
            }

            public Builder detectResourceMismatches()
            {
                return this;
            }

            public Builder detectUnbufferedIo()
            {
                return this;
            }

            public Builder detectExplicitGc()
            {
                return this;
            }

            public Builder permitAll()
            {
                return this;
            }

            public Builder permitDiskReads()
            {
                return this;
            }

            public Builder permitDiskWrites()
            {
                return this;
            }

            public Builder permitNetwork()
            {
                return this;
            }

            public Builder permitCustomSlowCalls()
            {
                return this;
            }

            public Builder permitResourceMismatches()
            {
                return this;
            }

            public Builder permitUnbufferedIo()
            {
                return this;
            }

            public Builder permitExplicitGc()
            {
                return this;
            }

            public Builder penaltyLog()
            {
                return this;
            }

            public Builder penaltyDeath()
            {
                return this;
            }

            public Builder penaltyDeathOnNetwork()
            {
                return this;
            }

            public Builder penaltyDialog()
            {
                return this;
            }

            public Builder penaltyDropBox()
            {
                return this;
            }

            public Builder penaltyFlashScreen()
            {
                return this;
            }

            public Builder penaltyListener(java.util.concurrent.Executor executor, OnThreadViolationListener listener)
            {
                return this;
            }

            public ThreadPolicy build()
            {
                return LAX;
            }
        }
    }

    public static final class VmPolicy
    {
        public static final VmPolicy LAX = new VmPolicy();

        public static final class Builder
        {
            public Builder()
            {
            }

            public Builder(VmPolicy policy)
            {
            }

            public Builder detectAll()
            {
                return this;
            }

            public Builder detectLeakedSqlLiteObjects()
            {
                return this;
            }

            public Builder detectLeakedClosableObjects()
            {
                return this;
            }

            public Builder detectActivityLeaks()
            {
                return this;
            }

            public Builder detectFileUriExposure()
            {
                return this;
            }

            public Builder detectLeakedRegistrationObjects()
            {
                return this;
            }

            public Builder detectCleartextNetwork()
            {
                return this;
            }

            public Builder detectContentUriWithoutPermission()
            {
                return this;
            }

            public Builder detectUntaggedSockets()
            {
                return this;
            }

            public Builder detectNonSdkApiUsage()
            {
                return this;
            }

            public Builder permitNonSdkApiUsage()
            {
                return this;
            }

            public Builder detectImplicitDirectBoot()
            {
                return this;
            }

            public Builder detectCredentialProtectedWhileLocked()
            {
                return this;
            }

            public Builder detectIncorrectContextUse()
            {
                return this;
            }

            public Builder detectUnsafeIntentLaunch()
            {
                return this;
            }

            public Builder permitUnsafeIntentLaunch()
            {
                return this;
            }

            public Builder setClassInstanceLimit(Class<?> watched, int instanceLimit)
            {
                return this;
            }

            public Builder penaltyLog()
            {
                return this;
            }

            public Builder penaltyDeath()
            {
                return this;
            }

            public Builder penaltyDropBox()
            {
                return this;
            }

            public Builder penaltyDeathOnFileUriExposure()
            {
                return this;
            }

            public Builder penaltyDeathOnCleartextNetwork()
            {
                return this;
            }

            public Builder penaltyListener(java.util.concurrent.Executor executor, OnVmViolationListener listener)
            {
                return this;
            }

            public VmPolicy build()
            {
                return LAX;
            }
        }
    }
}
