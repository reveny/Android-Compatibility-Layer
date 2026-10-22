package android.location;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Address implements Parcelable
{
    private Locale mLocale;
    private String mFeatureName;
    private HashMap<Integer, String> mAddressLines;
    private int mMaxAddressLineIndex = -1;
    private String mAdminArea;
    private String mSubAdminArea;
    private String mLocality;
    private String mSubLocality;
    private String mThoroughfare;
    private String mSubThoroughfare;
    private String mPremises;
    private String mPostalCode;
    private String mCountryCode;
    private String mCountryName;
    private double mLatitude;
    private double mLongitude;
    private boolean mHasLatitude;
    private boolean mHasLongitude;
    private String mPhone;
    private String mUrl;
    private Bundle mExtras;

    public Address(Locale locale)
    {
        mLocale = locale;
    }

    public Locale getLocale()
    {
        return mLocale;
    }

    public int getMaxAddressLineIndex()
    {
        return mMaxAddressLineIndex;
    }

    public String getAddressLine(int index)
    {
        if (index < 0) throw new IllegalArgumentException("index = " + index + " < 0");

        return mAddressLines == null ? null : mAddressLines.get(index);
    }

    public void setAddressLine(int index, String line)
    {
        if (index < 0) throw new IllegalArgumentException("index = " + index + " < 0");

        if (mAddressLines == null) mAddressLines = new HashMap<>();
        mAddressLines.put(index, line);

        if (line == null)
        {
            mMaxAddressLineIndex = -1;
            for (Map.Entry<Integer, String> entry : mAddressLines.entrySet())
            {
                if (entry.getValue() != null) mMaxAddressLineIndex = Math.max(mMaxAddressLineIndex, entry.getKey());
            }
        }
        else
        {
            mMaxAddressLineIndex = Math.max(mMaxAddressLineIndex, index);
        }
    }

    public String getFeatureName()
    {
        return mFeatureName;
    }

    public void setFeatureName(String featureName)
    {
        mFeatureName = featureName;
    }

    public String getAdminArea()
    {
        return mAdminArea;
    }

    public void setAdminArea(String adminArea)
    {
        mAdminArea = adminArea;
    }

    public String getSubAdminArea()
    {
        return mSubAdminArea;
    }

    public void setSubAdminArea(String subAdminArea)
    {
        mSubAdminArea = subAdminArea;
    }

    public String getLocality()
    {
        return mLocality;
    }

    public void setLocality(String locality)
    {
        mLocality = locality;
    }

    public String getSubLocality()
    {
        return mSubLocality;
    }

    public void setSubLocality(String subLocality)
    {
        mSubLocality = subLocality;
    }

    public String getThoroughfare()
    {
        return mThoroughfare;
    }

    public void setThoroughfare(String thoroughfare)
    {
        mThoroughfare = thoroughfare;
    }

    public String getSubThoroughfare()
    {
        return mSubThoroughfare;
    }

    public void setSubThoroughfare(String subThoroughfare)
    {
        mSubThoroughfare = subThoroughfare;
    }

    public String getPremises()
    {
        return mPremises;
    }

    public void setPremises(String premises)
    {
        mPremises = premises;
    }

    public String getPostalCode()
    {
        return mPostalCode;
    }

    public void setPostalCode(String postalCode)
    {
        mPostalCode = postalCode;
    }

    public String getCountryCode()
    {
        return mCountryCode;
    }

    public void setCountryCode(String countryCode)
    {
        mCountryCode = countryCode;
    }

    public String getCountryName()
    {
        return mCountryName;
    }

    public void setCountryName(String countryName)
    {
        mCountryName = countryName;
    }

    public boolean hasLatitude()
    {
        return mHasLatitude;
    }

    public double getLatitude()
    {
        if (!mHasLatitude) throw new IllegalStateException();

        return mLatitude;
    }

    public void setLatitude(double latitude)
    {
        mLatitude = latitude;
        mHasLatitude = true;
    }

    public void clearLatitude()
    {
        mHasLatitude = false;
    }

    public boolean hasLongitude()
    {
        return mHasLongitude;
    }

    public double getLongitude()
    {
        if (!mHasLongitude) throw new IllegalStateException();

        return mLongitude;
    }

    public void setLongitude(double longitude)
    {
        mLongitude = longitude;
        mHasLongitude = true;
    }

    public void clearLongitude()
    {
        mHasLongitude = false;
    }

    public String getPhone()
    {
        return mPhone;
    }

    public void setPhone(String phone)
    {
        mPhone = phone;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        mUrl = url;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public void setExtras(Bundle extras)
    {
        mExtras = extras == null ? null : new Bundle(extras);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Address[addressLines=[");

        for (int index = 0; index <= mMaxAddressLineIndex; index++)
        {
            if (index > 0) builder.append(',');
            builder.append(index).append(':');

            String line = mAddressLines.get(index);
            if (line == null) builder.append("null");
            else builder.append('"').append(line).append('"');
        }

        builder.append(']');
        builder.append(",feature=").append(mFeatureName);
        builder.append(",admin=").append(mAdminArea);
        builder.append(",sub-admin=").append(mSubAdminArea);
        builder.append(",locality=").append(mLocality);
        builder.append(",thoroughfare=").append(mThoroughfare);
        builder.append(",postalCode=").append(mPostalCode);
        builder.append(",countryCode=").append(mCountryCode);
        builder.append(",countryName=").append(mCountryName);
        builder.append(",hasLatitude=").append(mHasLatitude);
        builder.append(",latitude=").append(mLatitude);
        builder.append(",hasLongitude=").append(mHasLongitude);
        builder.append(",longitude=").append(mLongitude);
        builder.append(",phone=").append(mPhone);
        builder.append(",url=").append(mUrl);
        builder.append(",extras=").append(mExtras);
        builder.append(']');
        return builder.toString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mLocale.getLanguage());
        parcel.writeString(mLocale.getCountry());

        int count = mMaxAddressLineIndex + 1;
        parcel.writeInt(count);

        for (int index = 0; index < count; index++)
        {
            parcel.writeString(mAddressLines.get(index));
        }

        parcel.writeString(mFeatureName);
        parcel.writeString(mAdminArea);
        parcel.writeString(mSubAdminArea);
        parcel.writeString(mLocality);
        parcel.writeString(mSubLocality);
        parcel.writeString(mThoroughfare);
        parcel.writeString(mSubThoroughfare);
        parcel.writeString(mPremises);
        parcel.writeString(mPostalCode);
        parcel.writeString(mCountryCode);
        parcel.writeString(mCountryName);
        parcel.writeInt(mHasLatitude ? 1 : 0);
        parcel.writeLong(Double.doubleToRawLongBits(mLatitude));
        parcel.writeInt(mHasLongitude ? 1 : 0);
        parcel.writeLong(Double.doubleToRawLongBits(mLongitude));
        parcel.writeString(mPhone);
        parcel.writeString(mUrl);
        parcel.writeBundle(mExtras);
    }

    public static final Creator<Address> CREATOR = new Creator<Address>()
    {
        @Override
        public Address createFromParcel(Parcel parcel)
        {
            String language = parcel.readString();
            String country = parcel.readString();
            Locale locale = country.length() > 0 ? new Locale(language, country) : new Locale(language);
            Address address = new Address(locale);

            int count = parcel.readInt();
            for (int index = 0; index < count; index++)
            {
                address.setAddressLine(index, parcel.readString());
            }

            address.mFeatureName = parcel.readString();
            address.mAdminArea = parcel.readString();
            address.mSubAdminArea = parcel.readString();
            address.mLocality = parcel.readString();
            address.mSubLocality = parcel.readString();
            address.mThoroughfare = parcel.readString();
            address.mSubThoroughfare = parcel.readString();
            address.mPremises = parcel.readString();
            address.mPostalCode = parcel.readString();
            address.mCountryCode = parcel.readString();
            address.mCountryName = parcel.readString();
            address.mHasLatitude = parcel.readInt() != 0;
            address.mLatitude = Double.longBitsToDouble(parcel.readLong());
            address.mHasLongitude = parcel.readInt() != 0;
            address.mLongitude = Double.longBitsToDouble(parcel.readLong());
            address.mPhone = parcel.readString();
            address.mUrl = parcel.readString();
            address.mExtras = parcel.readBundle();
            return address;
        }

        @Override
        public Address[] newArray(int size)
        {
            return new Address[size];
        }
    };
}
