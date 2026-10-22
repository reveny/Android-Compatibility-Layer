package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;
import java.util.Set;

public class NetworkRequest implements Parcelable
{
    private final NetworkCapabilities mNetworkCapabilities;

    private NetworkRequest(NetworkCapabilities capabilities)
    {
        mNetworkCapabilities = capabilities;
    }

    public boolean hasTransport(int transportType)
    {
        return mNetworkCapabilities.hasTransport(transportType);
    }

    public boolean hasCapability(int capability)
    {
        return mNetworkCapabilities.hasCapability(capability);
    }

    public int[] getCapabilities()
    {
        return mNetworkCapabilities.getCapabilities();
    }

    public int[] getTransportTypes()
    {
        return mNetworkCapabilities.getTransportTypes();
    }

    public NetworkSpecifier getNetworkSpecifier()
    {
        return mNetworkCapabilities.getNetworkSpecifier();
    }

    public Set<Integer> getSubscriptionIds()
    {
        return mNetworkCapabilities.getSubscriptionIds();
    }

    public boolean canBeSatisfiedBy(NetworkCapabilities capabilities)
    {
        return mNetworkCapabilities.satisfiedByNetworkCapabilities(capabilities);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof NetworkRequest && mNetworkCapabilities.equals(((NetworkRequest)other).mNetworkCapabilities);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mNetworkCapabilities);
    }

    @Override
    public String toString()
    {
        return "NetworkRequest [ LISTEN id=0, " + mNetworkCapabilities + " ]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        mNetworkCapabilities.writeToParcel(dest, flags);
    }

    public static final Creator<NetworkRequest> CREATOR = new Creator<NetworkRequest>()
    {
        @Override
        public NetworkRequest createFromParcel(Parcel source)
        {
            return new NetworkRequest(NetworkCapabilities.CREATOR.createFromParcel(source));
        }

        @Override
        public NetworkRequest[] newArray(int size)
        {
            return new NetworkRequest[size];
        }
    };

    private static final class StringNetworkSpecifier extends NetworkSpecifier
    {
        private final String mSpecifier;

        StringNetworkSpecifier(String specifier)
        {
            mSpecifier = specifier;
        }

        @Override
        public boolean canBeSatisfiedBy(NetworkSpecifier other)
        {
            return equals(other);
        }

        @Override
        public boolean equals(Object other)
        {
            return other instanceof StringNetworkSpecifier && mSpecifier.equals(((StringNetworkSpecifier)other).mSpecifier);
        }

        @Override
        public int hashCode()
        {
            return mSpecifier.hashCode();
        }

        @Override
        public String toString()
        {
            return mSpecifier;
        }
    }

    public static class Builder
    {
        private final NetworkCapabilities mNetworkCapabilities;

        public Builder()
        {
            mNetworkCapabilities = new NetworkCapabilities();
        }

        public Builder(NetworkRequest request)
        {
            if (request == null) throw new NullPointerException("request");

            mNetworkCapabilities = new NetworkCapabilities(request.mNetworkCapabilities);
        }

        public NetworkRequest build()
        {
            return new NetworkRequest(new NetworkCapabilities(mNetworkCapabilities));
        }

        public Builder addCapability(int capability)
        {
            mNetworkCapabilities.addCapability(capability);
            return this;
        }

        public Builder removeCapability(int capability)
        {
            mNetworkCapabilities.removeCapability(capability);
            return this;
        }

        public Builder clearCapabilities()
        {
            mNetworkCapabilities.clearCapabilities();
            return this;
        }

        public Builder addTransportType(int transportType)
        {
            mNetworkCapabilities.addTransportType(transportType);
            return this;
        }

        public Builder removeTransportType(int transportType)
        {
            mNetworkCapabilities.removeTransportType(transportType);
            return this;
        }

        public Builder setIncludeOtherUidNetworks(boolean include)
        {
            return this;
        }

        @Deprecated
        public Builder setNetworkSpecifier(String networkSpecifier)
        {
            if (networkSpecifier == null || networkSpecifier.isEmpty()) return setNetworkSpecifier((NetworkSpecifier)null);

            return setNetworkSpecifier(new StringNetworkSpecifier(networkSpecifier));
        }

        public Builder setNetworkSpecifier(NetworkSpecifier networkSpecifier)
        {
            mNetworkCapabilities.setNetworkSpecifier(networkSpecifier);
            return this;
        }

        public Builder setSubscriptionIds(Set<Integer> subscriptionIds)
        {
            mNetworkCapabilities.setSubscriptionIds(subscriptionIds);
            return this;
        }
    }
}
