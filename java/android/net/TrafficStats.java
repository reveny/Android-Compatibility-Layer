package android.net;

public class TrafficStats
{
    public static final int UNSUPPORTED = -1;

    public static void setThreadStatsTag(int tag)
    {
    }

    public static int getThreadStatsTag()
    {
        return 0;
    }

    public static void clearThreadStatsTag()
    {
    }

    public static void setThreadStatsUid(int uid)
    {
    }

    public static void clearThreadStatsUid()
    {
    }

    public static void tagSocket(java.net.Socket socket)
    {
    }

    public static void untagSocket(java.net.Socket socket)
    {
    }

    public static void tagDatagramSocket(java.net.DatagramSocket socket)
    {
    }

    public static void untagDatagramSocket(java.net.DatagramSocket socket)
    {
    }

    public static void incrementOperationCount(int operationCount)
    {
    }

    public static void incrementOperationCount(int tag, int operationCount)
    {
    }

    public static long getMobileTxPackets()
    {
        return UNSUPPORTED;
    }

    public static long getMobileRxPackets()
    {
        return UNSUPPORTED;
    }

    public static long getMobileTxBytes()
    {
        return UNSUPPORTED;
    }

    public static long getMobileRxBytes()
    {
        return UNSUPPORTED;
    }

    public static long getTotalTxPackets()
    {
        return UNSUPPORTED;
    }

    public static long getTotalRxPackets()
    {
        return UNSUPPORTED;
    }

    public static long getTotalTxBytes()
    {
        return UNSUPPORTED;
    }

    public static long getTotalRxBytes()
    {
        return UNSUPPORTED;
    }

    public static long getUidTxBytes(int uid)
    {
        return UNSUPPORTED;
    }

    public static long getUidRxBytes(int uid)
    {
        return UNSUPPORTED;
    }

    public static long getUidTxPackets(int uid)
    {
        return UNSUPPORTED;
    }

    public static long getUidRxPackets(int uid)
    {
        return UNSUPPORTED;
    }
}
