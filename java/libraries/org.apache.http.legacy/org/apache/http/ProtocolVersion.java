package org.apache.http;

public class ProtocolVersion implements java.io.Serializable, Cloneable
{
    protected final String protocol;
    protected final int major;
    protected final int minor;

    public ProtocolVersion(String protocol, int major, int minor)
    {
        if (protocol == null) throw new IllegalArgumentException("Protocol name may not be null");
        if (major < 0) throw new IllegalArgumentException("Protocol major version number must not be negative");
        if (minor < 0) throw new IllegalArgumentException("Protocol minor version number may not be negative");

        this.protocol = protocol;
        this.major = major;
        this.minor = minor;
    }

    public final String getProtocol()
    {
        return protocol;
    }

    public final int getMajor()
    {
        return major;
    }

    public final int getMinor()
    {
        return minor;
    }

    public ProtocolVersion forVersion(int major, int minor)
    {
        if (major == this.major && minor == this.minor) return this;
        return new ProtocolVersion(protocol, major, minor);
    }

    public boolean isComparable(ProtocolVersion that)
    {
        return that != null && protocol.equals(that.protocol);
    }

    public int compareToVersion(ProtocolVersion that)
    {
        if (!isComparable(that)) throw new IllegalArgumentException("Versions for different protocols cannot be compared");

        int delta = getMajor() - that.getMajor();
        return delta != 0 ? delta : getMinor() - that.getMinor();
    }

    public final boolean greaterEquals(ProtocolVersion version)
    {
        return isComparable(version) && compareToVersion(version) >= 0;
    }

    public final boolean lessEquals(ProtocolVersion version)
    {
        return isComparable(version) && compareToVersion(version) <= 0;
    }

    @Override
    public final int hashCode()
    {
        return protocol.hashCode() ^ (major * 100000) ^ minor;
    }

    @Override
    public final boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ProtocolVersion)) return false;

        ProtocolVersion that = (ProtocolVersion)other;
        return protocol.equals(that.protocol) && major == that.major && minor == that.minor;
    }

    @Override
    public String toString()
    {
        return protocol + "/" + major + "." + minor;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
