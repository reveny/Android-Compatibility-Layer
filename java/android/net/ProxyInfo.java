package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProxyInfo implements Parcelable
{
    private final String mHost;
    private final int mPort;
    private final String mExclusionList;
    private final String[] mParsedExclusionList;
    private final Uri mPacFileUrl;

    private ProxyInfo(String host, int port, String exclusionList, Uri pacFileUrl)
    {
        mHost = host;
        mPort = port;
        mExclusionList = exclusionList == null ? "" : exclusionList.toLowerCase(Locale.ROOT);
        mParsedExclusionList = mExclusionList.isEmpty() ? new String[0] : mExclusionList.split(",");
        mPacFileUrl = pacFileUrl == null ? Uri.EMPTY : pacFileUrl;
    }

    public ProxyInfo(ProxyInfo source)
    {
        this(source == null ? null : source.mHost, source == null ? 0 : source.mPort, source == null ? null : source.mExclusionList,
             source == null ? null : source.mPacFileUrl);
    }

    public static ProxyInfo buildDirectProxy(String host, int port)
    {
        return new ProxyInfo(host, port, null, null);
    }

    public static ProxyInfo buildDirectProxy(String host, int port, List<String> exclList)
    {
        String joined = exclList == null ? null : String.join(",", exclList);
        return new ProxyInfo(host, port, joined, null);
    }

    public static ProxyInfo buildPacProxy(Uri pacUri)
    {
        return new ProxyInfo("localhost", -1, null, pacUri);
    }

    public static ProxyInfo buildPacProxy(Uri pacUrl, int localProxyPort)
    {
        return new ProxyInfo("localhost", localProxyPort, null, pacUrl);
    }

    public Uri getPacFileUrl()
    {
        return mPacFileUrl;
    }

    public String getHost()
    {
        return mHost;
    }

    public int getPort()
    {
        return mPort;
    }

    public String[] getExclusionList()
    {
        return mParsedExclusionList.clone();
    }

    public boolean isValid()
    {
        if (!Uri.EMPTY.equals(mPacFileUrl)) return true;

        return mHost != null && !mHost.isEmpty() && mPort > 0 && mPort <= 65535;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        if (!Uri.EMPTY.equals(mPacFileUrl))
        {
            builder.append("PAC Script: ").append(mPacFileUrl);
        }

        if (mHost != null)
        {
            builder.append("[").append(mHost).append("] ").append(mPort);
            if (!mExclusionList.isEmpty()) builder.append(" xl=").append(mExclusionList);
        }
        else
        {
            builder.append("[ProxyProperties.mHost == null]");
        }

        return builder.toString();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ProxyInfo)) return false;

        ProxyInfo proxy = (ProxyInfo)other;
        return Objects.equals(mHost, proxy.mHost) && mPort == proxy.mPort && mExclusionList.equals(proxy.mExclusionList)
            && mPacFileUrl.equals(proxy.mPacFileUrl);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mHost, mPort, mExclusionList) + Arrays.hashCode(mParsedExclusionList);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mHost);
        dest.writeInt(mPort);
        dest.writeString(mExclusionList);
        dest.writeString(Uri.EMPTY.equals(mPacFileUrl) ? null : mPacFileUrl.toString());
    }

    public static final Creator<ProxyInfo> CREATOR = new Creator<ProxyInfo>()
    {
        @Override
        public ProxyInfo createFromParcel(Parcel source)
        {
            String host = source.readString();
            int port = source.readInt();
            String exclusionList = source.readString();
            String pac = source.readString();
            return new ProxyInfo(host, port, exclusionList, pac == null ? null : Uri.parse(pac));
        }

        @Override
        public ProxyInfo[] newArray(int size)
        {
            return new ProxyInfo[size];
        }
    };
}
