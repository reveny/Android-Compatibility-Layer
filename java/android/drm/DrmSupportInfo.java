package android.drm;

import java.util.ArrayList;
import java.util.Iterator;

public class DrmSupportInfo
{
    private final ArrayList<String> mFileSuffixList = new ArrayList<>();
    private final ArrayList<String> mMimeTypeList = new ArrayList<>();
    private String mDescription = "";

    public DrmSupportInfo()
    {
    }

    public void addFileSuffix(String fileSuffix)
    {
        if (fileSuffix == null || fileSuffix.isEmpty()) throw new IllegalArgumentException("fileSuffix is null or empty");

        mFileSuffixList.add(fileSuffix);
    }

    public void addMimeType(String mimeType)
    {
        if (mimeType == null) throw new IllegalArgumentException("mimeType is null");
        if (mimeType.isEmpty()) throw new IllegalArgumentException("mimeType is an empty string");

        mMimeTypeList.add(mimeType);
    }

    @Override
    public boolean equals(Object object)
    {
        if (!(object instanceof DrmSupportInfo)) return false;

        DrmSupportInfo other = (DrmSupportInfo)object;
        return mFileSuffixList.equals(other.mFileSuffixList) && mMimeTypeList.equals(other.mMimeTypeList)
            && mDescription.equals(other.mDescription);
    }

    public String getDescriprition()
    {
        return mDescription;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public Iterator<String> getFileSuffixIterator()
    {
        return mFileSuffixList.iterator();
    }

    public Iterator<String> getMimeTypeIterator()
    {
        return mMimeTypeList.iterator();
    }

    @Override
    public int hashCode()
    {
        return mFileSuffixList.hashCode() + mMimeTypeList.hashCode() + mDescription.hashCode();
    }

    public void setDescription(String description)
    {
        if (description == null) throw new IllegalArgumentException("description is null");
        if (description.isEmpty()) throw new IllegalArgumentException("description is an empty string");

        mDescription = description;
    }
}
