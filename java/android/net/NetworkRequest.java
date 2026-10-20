package android.net;

public class NetworkRequest
{
    private NetworkRequest()
    {
    }

    public boolean hasTransport(int transport)
    {
        return false;
    }

    public boolean hasCapability(int capability)
    {
        return false;
    }

    public static class Builder
    {
        public Builder addTransportType(int transport)
        {
            return this;
        }

        public Builder removeTransportType(int transport)
        {
            return this;
        }

        public Builder addCapability(int capability)
        {
            return this;
        }

        public Builder removeCapability(int capability)
        {
            return this;
        }

        public NetworkRequest build()
        {
            return new NetworkRequest();
        }
    }
}
