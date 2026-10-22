package android.media.metrics;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class NetworkEvent extends Event implements Parcelable
{
    public static final int NETWORK_TYPE_2G = 4;
    public static final int NETWORK_TYPE_3G = 5;
    public static final int NETWORK_TYPE_4G = 6;
    public static final int NETWORK_TYPE_5G_NSA = 7;
    public static final int NETWORK_TYPE_5G_SA = 8;
    public static final int NETWORK_TYPE_ETHERNET = 3;
    public static final int NETWORK_TYPE_OFFLINE = 9;
    public static final int NETWORK_TYPE_OTHER = 1;
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int NETWORK_TYPE_WIFI = 2;

    public static final Parcelable.Creator<NetworkEvent> CREATOR = new Parcelable.Creator<NetworkEvent>()
    {
        @Override
        public NetworkEvent createFromParcel(Parcel source)
        {
            int networkType = source.readInt();
            long timeSinceCreatedMillis = source.readLong();
            Bundle metricsBundle = source.readBundle();
            return new NetworkEvent(networkType, timeSinceCreatedMillis, metricsBundle);
        }

        @Override
        public NetworkEvent[] newArray(int size)
        {
            return new NetworkEvent[size];
        }
    };

    private final int mNetworkType;

    NetworkEvent(int networkType, long timeSinceCreatedMillis, Bundle metricsBundle)
    {
        super(timeSinceCreatedMillis, metricsBundle);
        mNetworkType = networkType;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof NetworkEvent)) return false;

        NetworkEvent other = (NetworkEvent)object;
        return mNetworkType == other.mNetworkType && getTimeSinceCreatedMillis() == other.getTimeSinceCreatedMillis();
    }

    @Override
    public Bundle getMetricsBundle()
    {
        return super.getMetricsBundle();
    }

    public int getNetworkType()
    {
        return mNetworkType;
    }

    @Override
    public long getTimeSinceCreatedMillis()
    {
        return super.getTimeSinceCreatedMillis();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mNetworkType, getTimeSinceCreatedMillis());
    }

    @Override
    public String toString()
    {
        return "NetworkEvent { networkType = " + mNetworkType + ", timeSinceCreatedMillis = " + getTimeSinceCreatedMillis() + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mNetworkType);
        destination.writeLong(getTimeSinceCreatedMillis());
        destination.writeBundle(getMetricsBundle());
    }

    public static final class Builder
    {
        private int mNetworkType = NETWORK_TYPE_UNKNOWN;
        private long mTimeSinceCreatedMillis = -1;
        private Bundle mMetricsBundle = new Bundle();

        public Builder()
        {
        }

        public NetworkEvent build()
        {
            return new NetworkEvent(mNetworkType, mTimeSinceCreatedMillis, mMetricsBundle);
        }

        public Builder setMetricsBundle(Bundle metricsBundle)
        {
            mMetricsBundle = metricsBundle;
            return this;
        }

        public Builder setNetworkType(int networkType)
        {
            mNetworkType = networkType;
            return this;
        }

        public Builder setTimeSinceCreatedMillis(long timeSinceCreatedMillis)
        {
            if (timeSinceCreatedMillis < 0) throw new IllegalArgumentException("Negative value: " + timeSinceCreatedMillis);

            mTimeSinceCreatedMillis = timeSinceCreatedMillis;
            return this;
        }
    }
}
