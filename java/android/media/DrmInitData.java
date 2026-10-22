package android.media;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public abstract class DrmInitData
{
    DrmInitData()
    {
    }

    public abstract DrmInitData.SchemeInitData get(UUID schemeUuid);

    public DrmInitData.SchemeInitData getSchemeInitDataAt(int index)
    {
        throw new IndexOutOfBoundsException("index: " + index);
    }

    public int getSchemeInitDataCount()
    {
        return 0;
    }

    public static final class SchemeInitData
    {
        public static final UUID UUID_NIL = new UUID(0L, 0L);
        public final byte[] data;
        public final String mimeType;
        public final UUID uuid;

        public SchemeInitData(UUID uuid, String mimeType, byte[] data)
        {
            this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
            this.mimeType = Objects.requireNonNull(mimeType, "mimeType must not be null");
            this.data = Objects.requireNonNull(data, "data must not be null");
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof SchemeInitData)) return false;

            SchemeInitData other = (SchemeInitData)object;
            return uuid.equals(other.uuid) && mimeType.equals(other.mimeType) && Arrays.equals(data, other.data);
        }

        @Override
        public int hashCode()
        {
            return uuid.hashCode() + 31 * (mimeType.hashCode() + 31 * Arrays.hashCode(data));
        }
    }
}
