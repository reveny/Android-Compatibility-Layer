package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

public class Network implements Parcelable
{
    private static final long HANDLE_MAGIC = 0xcafedL;
    private static final int HANDLE_MAGIC_SIZE = 32;

    private final int mHandle;

    public Network(int handle)
    {
        mHandle = handle;
    }

    public static Network fromNetworkHandle(long networkHandle)
    {
        if (networkHandle == 0)
        {
            throw new IllegalArgumentException("Network.fromNetworkHandle refusing to instantiate NETID_UNSET Network.");
        }

        if ((networkHandle & ((1L << HANDLE_MAGIC_SIZE) - 1)) != HANDLE_MAGIC || networkHandle < 0)
        {
            throw new IllegalArgumentException("Value passed to fromNetworkHandle() is not a network handle.");
        }

        return new Network((int)(networkHandle >> HANDLE_MAGIC_SIZE));
    }

    public long getNetworkHandle()
    {
        if (mHandle == 0) return 0L;

        return (((long)mHandle) << HANDLE_MAGIC_SIZE) | HANDLE_MAGIC;
    }

    public InetAddress[] getAllByName(String host) throws UnknownHostException
    {
        return InetAddress.getAllByName(host);
    }

    public InetAddress getByName(String host) throws UnknownHostException
    {
        return InetAddress.getByName(host);
    }

    public SocketFactory getSocketFactory()
    {
        return SocketFactory.getDefault();
    }

    public URLConnection openConnection(URL url) throws IOException
    {
        return url.openConnection();
    }

    public URLConnection openConnection(URL url, Proxy proxy) throws IOException
    {
        if (proxy == null) throw new IllegalArgumentException("proxy is null");

        return url.openConnection(proxy);
    }

    public void bindSocket(DatagramSocket socket) throws IOException
    {
    }

    public void bindSocket(Socket socket) throws IOException
    {
        if (socket.isConnected()) throw new java.net.SocketException("Socket is connected");
    }

    public void bindSocket(FileDescriptor fd) throws IOException
    {
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mHandle);
    }

    public static final Creator<Network> CREATOR = new Creator<Network>()
    {
        @Override
        public Network createFromParcel(Parcel source)
        {
            return new Network(source.readInt());
        }

        @Override
        public Network[] newArray(int size)
        {
            return new Network[size];
        }
    };

    @Override
    public boolean equals(Object other)
    {
        return other instanceof Network && ((Network)other).mHandle == mHandle;
    }

    @Override
    public int hashCode()
    {
        return mHandle * 11;
    }

    @Override
    public String toString()
    {
        return Integer.toString(mHandle);
    }
}
