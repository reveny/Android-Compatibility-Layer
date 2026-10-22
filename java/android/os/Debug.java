package android.os;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Debug
{
    public static final int SHOW_FULL_DETAIL = 1;
    public static final int SHOW_CLASSLOADER = 2;
    public static final int SHOW_INITIALIZED = 4;
    public static final int TRACE_COUNT_ALLOCS = 1;

    private static final String STATUS_FILE = "/proc/self/status";
    private static final String ROLLUP_FILE = "/proc/self/smaps_rollup";

    private Debug()
    {
    }

    public static class MemoryInfo implements Parcelable
    {
        public static final Parcelable.Creator<MemoryInfo> CREATOR = new Parcelable.Creator<MemoryInfo>()
        {
            @Override
            public MemoryInfo createFromParcel(Parcel source)
            {
                MemoryInfo info = new MemoryInfo();
                info.readFromParcel(source);
                return info;
            }

            @Override
            public MemoryInfo[] newArray(int size)
            {
                return new MemoryInfo[size];
            }
        };

        public int dalvikPss;
        public int dalvikPrivateDirty;
        public int dalvikSharedDirty;
        public int nativePss;
        public int nativePrivateDirty;
        public int nativeSharedDirty;
        public int otherPss;
        public int otherPrivateDirty;
        public int otherSharedDirty;

        int mPrivateClean;
        int mSharedClean;
        int mSwappedOut;
        int mStack;
        int mCode;

        public MemoryInfo()
        {
        }

        public int getTotalPss()
        {
            return dalvikPss + nativePss + otherPss;
        }

        public int getTotalSwappablePss()
        {
            return otherPss;
        }

        public int getTotalPrivateDirty()
        {
            return dalvikPrivateDirty + nativePrivateDirty + otherPrivateDirty;
        }

        public int getTotalSharedDirty()
        {
            return dalvikSharedDirty + nativeSharedDirty + otherSharedDirty;
        }

        public int getTotalPrivateClean()
        {
            return mPrivateClean;
        }

        public int getTotalSharedClean()
        {
            return mSharedClean;
        }

        public String getMemoryStat(String statName)
        {
            return getMemoryStats().get(statName);
        }

        public Map<String, String> getMemoryStats()
        {
            Map<String, String> stats = new HashMap<>();
            stats.put("summary.java-heap", Integer.toString(dalvikPrivateDirty));
            stats.put("summary.native-heap", Integer.toString(nativePrivateDirty));
            stats.put("summary.code", Integer.toString(mCode));
            stats.put("summary.stack", Integer.toString(mStack));
            stats.put("summary.graphics", "0");
            stats.put("summary.private-other", Integer.toString(otherPrivateDirty));
            stats.put("summary.system", Integer.toString(Math.max(0, getTotalPss() - dalvikPrivateDirty - nativePrivateDirty
                                                                  - mCode - mStack - otherPrivateDirty)));
            stats.put("summary.total-pss", Integer.toString(getTotalPss()));
            stats.put("summary.total-swap", Integer.toString(mSwappedOut));
            return stats;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(dalvikPss);
            dest.writeInt(dalvikPrivateDirty);
            dest.writeInt(dalvikSharedDirty);
            dest.writeInt(nativePss);
            dest.writeInt(nativePrivateDirty);
            dest.writeInt(nativeSharedDirty);
            dest.writeInt(otherPss);
            dest.writeInt(otherPrivateDirty);
            dest.writeInt(otherSharedDirty);
            dest.writeInt(mPrivateClean);
            dest.writeInt(mSharedClean);
            dest.writeInt(mSwappedOut);
            dest.writeInt(mStack);
            dest.writeInt(mCode);
        }

        public void readFromParcel(Parcel source)
        {
            dalvikPss = source.readInt();
            dalvikPrivateDirty = source.readInt();
            dalvikSharedDirty = source.readInt();
            nativePss = source.readInt();
            nativePrivateDirty = source.readInt();
            nativeSharedDirty = source.readInt();
            otherPss = source.readInt();
            otherPrivateDirty = source.readInt();
            otherSharedDirty = source.readInt();
            mPrivateClean = source.readInt();
            mSharedClean = source.readInt();
            mSwappedOut = source.readInt();
            mStack = source.readInt();
            mCode = source.readInt();
        }
    }

    public static void waitForDebugger()
    {
    }

    public static boolean waitingForDebugger()
    {
        return false;
    }

    public static boolean isDebuggerConnected()
    {
        return false;
    }

    public static void changeDebugPort(int port)
    {
    }

    public static void startNativeTracing()
    {
    }

    public static void stopNativeTracing()
    {
    }

    public static void enableEmulatorTraceOutput()
    {
    }

    public static void startMethodTracing()
    {
    }

    public static void startMethodTracing(String tracePath)
    {
    }

    public static void startMethodTracing(String tracePath, int bufferSize)
    {
    }

    public static void startMethodTracing(String tracePath, int bufferSize, int flags)
    {
    }

    public static void startMethodTracingSampling(String tracePath, int bufferSize, int intervalUs)
    {
    }

    public static void stopMethodTracing()
    {
    }

    public static long threadCpuTimeNanos()
    {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/thread-self/schedstat")))
        {
            String line = reader.readLine();
            if (line == null) return -1L;

            String[] fields = line.trim().split("\\s+");
            return Long.parseLong(fields[0]);
        }
        catch (IOException | NumberFormatException unreadable)
        {
            return -1L;
        }
    }

    public static void startAllocCounting()
    {
    }

    public static void stopAllocCounting()
    {
    }

    public static int getGlobalAllocCount()
    {
        return 0;
    }

    public static void resetGlobalAllocCount()
    {
    }

    public static int getGlobalAllocSize()
    {
        return 0;
    }

    public static void resetGlobalAllocSize()
    {
    }

    public static int getGlobalFreedCount()
    {
        return 0;
    }

    public static void resetGlobalFreedCount()
    {
    }

    public static int getGlobalFreedSize()
    {
        return 0;
    }

    public static void resetGlobalFreedSize()
    {
    }

    public static int getGlobalGcInvocationCount()
    {
        return 0;
    }

    public static void resetGlobalGcInvocationCount()
    {
    }

    public static int getGlobalClassInitCount()
    {
        return 0;
    }

    public static void resetGlobalClassInitCount()
    {
    }

    public static int getGlobalClassInitTime()
    {
        return 0;
    }

    public static void resetGlobalClassInitTime()
    {
    }

    public static int getGlobalExternalAllocCount()
    {
        return 0;
    }

    public static void resetGlobalExternalAllocSize()
    {
    }

    public static void resetGlobalExternalAllocCount()
    {
    }

    public static int getGlobalExternalAllocSize()
    {
        return 0;
    }

    public static int getGlobalExternalFreedCount()
    {
        return 0;
    }

    public static void resetGlobalExternalFreedCount()
    {
    }

    public static int getGlobalExternalFreedSize()
    {
        return 0;
    }

    public static void resetGlobalExternalFreedSize()
    {
    }

    public static int getThreadAllocCount()
    {
        return 0;
    }

    public static void resetThreadAllocCount()
    {
    }

    public static int getThreadAllocSize()
    {
        return 0;
    }

    public static void resetThreadAllocSize()
    {
    }

    public static int getThreadExternalAllocCount()
    {
        return 0;
    }

    public static void resetThreadExternalAllocCount()
    {
    }

    public static int getThreadExternalAllocSize()
    {
        return 0;
    }

    public static void resetThreadExternalAllocSize()
    {
    }

    public static int getThreadGcInvocationCount()
    {
        return 0;
    }

    public static void resetThreadGcInvocationCount()
    {
    }

    public static void resetAllCounts()
    {
    }

    public static String getRuntimeStat(String statName)
    {
        return getRuntimeStats().get(statName);
    }

    public static Map<String, String> getRuntimeStats()
    {
        Map<String, String> stats = new HashMap<>();
        stats.put("art.gc.gc-count", "0");
        stats.put("art.gc.gc-time", "0");
        stats.put("art.gc.bytes-allocated", Long.toString(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        stats.put("art.gc.bytes-freed", "0");
        stats.put("art.gc.blocking-gc-count", "0");
        stats.put("art.gc.blocking-gc-time", "0");
        stats.put("art.gc.gc-count-rate-histogram", "");
        stats.put("art.gc.blocking-gc-count-rate-histogram", "");
        return stats;
    }

    public static long getNativeHeapSize()
    {
        long data = statusKilobytes("VmData");
        return data < 0 ? 0L : data * 1024L;
    }

    public static long getNativeHeapAllocatedSize()
    {
        long anonymous = statusKilobytes("RssAnon");
        if (anonymous < 0) return 0L;

        long allocated = anonymous * 1024L - javaHeapUsedBytes();
        return allocated < 0 ? 0L : allocated;
    }

    public static long getNativeHeapFreeSize()
    {
        long free = getNativeHeapSize() - getNativeHeapAllocatedSize();
        return free < 0 ? 0L : free;
    }

    public static void getMemoryInfo(MemoryInfo info)
    {
        if (info == null) return;

        Map<String, Long> status = readKilobytes(STATUS_FILE);
        Map<String, Long> rollup = readKilobytes(ROLLUP_FILE);

        int javaHeap = (int)(javaHeapUsedBytes() / 1024L);
        int anonymous = (int)value(status, "RssAnon", 0L);
        int file = (int)value(status, "RssFile", 0L);
        int shared = (int)value(status, "RssShmem", 0L);
        int nativeHeap = Math.max(0, anonymous - javaHeap);

        info.dalvikPss = javaHeap;
        info.dalvikPrivateDirty = javaHeap;
        info.dalvikSharedDirty = 0;
        info.nativePss = nativeHeap;
        info.nativePrivateDirty = nativeHeap;
        info.nativeSharedDirty = 0;

        long totalPss = value(rollup, "Pss", -1L);
        int other = totalPss >= 0 ? (int)Math.max(0L, totalPss - javaHeap - nativeHeap) : file + shared;

        info.otherPss = other;
        info.otherPrivateDirty = (int)value(rollup, "Private_Dirty", 0L) > anonymous
                                     ? (int)value(rollup, "Private_Dirty", 0L) - anonymous
                                     : 0;
        info.otherSharedDirty = (int)value(rollup, "Shared_Dirty", 0L);
        info.mPrivateClean = (int)value(rollup, "Private_Clean", 0L);
        info.mSharedClean = (int)value(rollup, "Shared_Clean", 0L);
        info.mSwappedOut = (int)value(status, "VmSwap", 0L);
        info.mStack = (int)value(status, "VmStk", 0L);
        info.mCode = (int)value(status, "VmExe", 0L) + (int)value(status, "VmLib", 0L);
    }

    public static long getPss()
    {
        Map<String, Long> rollup = readKilobytes(ROLLUP_FILE);
        long pss = value(rollup, "Pss", -1L);
        if (pss >= 0) return pss;

        return Math.max(0L, statusKilobytes("VmRSS"));
    }

    public static int setAllocationLimit(int limit)
    {
        return -1;
    }

    public static int setGlobalAllocationLimit(int limit)
    {
        return -1;
    }

    public static void printLoadedClasses(int flags)
    {
    }

    public static int getLoadedClassCount()
    {
        return 0;
    }

    public static void dumpHprofData(String fileName) throws IOException
    {
        throw new UnsupportedOperationException("heap dumps are not supported on this runtime");
    }

    public static int getBinderSentTransactions()
    {
        return 0;
    }

    public static int getBinderReceivedTransactions()
    {
        return 0;
    }

    public static int getBinderLocalObjectCount()
    {
        return 0;
    }

    public static int getBinderProxyObjectCount()
    {
        return 0;
    }

    public static int getBinderDeathObjectCount()
    {
        return 0;
    }

    public static boolean dumpService(String name, FileDescriptor descriptor, String[] arguments)
    {
        return false;
    }

    public static void attachJvmtiAgent(String library, String options, ClassLoader classLoader) throws IOException
    {
        throw new IOException("agent attachment is not supported on this runtime");
    }

    private static long javaHeapUsedBytes()
    {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long statusKilobytes(String key)
    {
        return value(readKilobytes(STATUS_FILE), key, -1L);
    }

    private static long value(Map<String, Long> values, String key, long fallback)
    {
        Long value = values.get(key);
        return value == null ? fallback : value;
    }

    private static Map<String, Long> readKilobytes(String path)
    {
        Map<String, Long> values = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;

                String rest = line.substring(colon + 1).trim();
                if (!rest.endsWith("kB")) continue;

                try
                {
                    values.put(line.substring(0, colon).trim(), Long.parseLong(rest.substring(0, rest.length() - 2).trim()));
                }
                catch (NumberFormatException ignored)
                {
                    continue;
                }
            }
        }
        catch (IOException unreadable)
        {
            return values;
        }

        return values;
    }
}
