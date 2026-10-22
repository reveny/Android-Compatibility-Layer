package android.telephony;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class SubscriptionInfo implements Parcelable
{
    public static final Parcelable.Creator<SubscriptionInfo> CREATOR = new Parcelable.Creator<SubscriptionInfo>()
    {
        @Override
        public SubscriptionInfo createFromParcel(Parcel source)
        {
            return new SubscriptionInfo(source);
        }

        @Override
        public SubscriptionInfo[] newArray(int size)
        {
            return new SubscriptionInfo[size];
        }
    };

    private final int mId;
    private final String mIccId;
    private final int mSimSlotIndex;
    private final String mDisplayName;
    private final String mCarrierName;
    private final int mIconTint;
    private final String mNumber;
    private final int mDataRoaming;
    private final String mMcc;
    private final String mMnc;
    private final String mCountryIso;
    private final boolean mIsEmbedded;
    private final int mCardId;
    private final boolean mIsOpportunistic;
    private final ParcelUuid mGroupUuid;
    private final int mCarrierId;
    private final int mSubscriptionType;
    private final int mPortIndex;
    private final int mUsageSetting;
    private final boolean mOnlyNonTerrestrialNetwork;
    private final Set<Integer> mServiceCapabilities;

    SubscriptionInfo(int id, String iccId, int simSlotIndex, String displayName, String carrierName, int iconTint, String number,
        int dataRoaming, String mcc, String mnc, String countryIso, boolean isEmbedded, int cardId, boolean isOpportunistic,
        ParcelUuid groupUuid, int carrierId, int subscriptionType, int portIndex, int usageSetting, boolean onlyNonTerrestrialNetwork,
        Set<Integer> serviceCapabilities)
    {
        mId = id;
        mIccId = iccId == null ? "" : iccId;
        mSimSlotIndex = simSlotIndex;
        mDisplayName = displayName;
        mCarrierName = carrierName;
        mIconTint = iconTint;
        mNumber = number == null ? "" : number;
        mDataRoaming = dataRoaming;
        mMcc = mcc;
        mMnc = mnc;
        mCountryIso = countryIso == null ? "" : countryIso;
        mIsEmbedded = isEmbedded;
        mCardId = cardId;
        mIsOpportunistic = isOpportunistic;
        mGroupUuid = groupUuid;
        mCarrierId = carrierId;
        mSubscriptionType = subscriptionType;
        mPortIndex = portIndex;
        mUsageSetting = usageSetting;
        mOnlyNonTerrestrialNetwork = onlyNonTerrestrialNetwork;
        mServiceCapabilities = serviceCapabilities == null ? new LinkedHashSet<>() : new LinkedHashSet<>(serviceCapabilities);
    }

    private SubscriptionInfo(Parcel source)
    {
        mId = source.readInt();
        mIccId = source.readString();
        mSimSlotIndex = source.readInt();
        mDisplayName = source.readString();
        mCarrierName = source.readString();
        mIconTint = source.readInt();
        mNumber = source.readString();
        mDataRoaming = source.readInt();
        mMcc = source.readString();
        mMnc = source.readString();
        mCountryIso = source.readString();
        mIsEmbedded = source.readBoolean();
        mCardId = source.readInt();
        mIsOpportunistic = source.readBoolean();
        String groupUuid = source.readString();
        mGroupUuid = groupUuid == null ? null : ParcelUuid.fromString(groupUuid);
        mCarrierId = source.readInt();
        mSubscriptionType = source.readInt();
        mPortIndex = source.readInt();
        mUsageSetting = source.readInt();
        mOnlyNonTerrestrialNetwork = source.readBoolean();
        mServiceCapabilities = new LinkedHashSet<>();
        for (int capability : source.createIntArray()) mServiceCapabilities.add(capability);
    }

    public int getSubscriptionId()
    {
        return mId;
    }

    public String getIccId()
    {
        return mIccId;
    }

    public int getSimSlotIndex()
    {
        return mSimSlotIndex;
    }

    public int getCarrierId()
    {
        return mCarrierId;
    }

    public CharSequence getDisplayName()
    {
        return mDisplayName;
    }

    public CharSequence getCarrierName()
    {
        return mCarrierName;
    }

    public Bitmap createIconBitmap(Context context)
    {
        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(mIconTint);
        return bitmap;
    }

    public int getIconTint()
    {
        return mIconTint;
    }

    @Deprecated
    public String getNumber()
    {
        return mNumber;
    }

    public int getDataRoaming()
    {
        return mDataRoaming;
    }

    @Deprecated
    public int getMcc()
    {
        return CellIdentity.parseCode(mMcc) == CellInfo.UNAVAILABLE ? 0 : CellIdentity.parseCode(mMcc);
    }

    @Deprecated
    public int getMnc()
    {
        return CellIdentity.parseCode(mMnc) == CellInfo.UNAVAILABLE ? 0 : CellIdentity.parseCode(mMnc);
    }

    public String getMccString()
    {
        return mMcc;
    }

    public String getMncString()
    {
        return mMnc;
    }

    public String getCountryIso()
    {
        return mCountryIso;
    }

    public boolean isEmbedded()
    {
        return mIsEmbedded;
    }

    public boolean isOpportunistic()
    {
        return mIsOpportunistic;
    }

    public ParcelUuid getGroupUuid()
    {
        return mGroupUuid;
    }

    public int getSubscriptionType()
    {
        return mSubscriptionType;
    }

    public int getCardId()
    {
        return mCardId;
    }

    public int getPortIndex()
    {
        return mPortIndex;
    }

    public int getUsageSetting()
    {
        return mUsageSetting;
    }

    public boolean isOnlyNonTerrestrialNetwork()
    {
        return mOnlyNonTerrestrialNetwork;
    }

    public Set<Integer> getServiceCapabilities()
    {
        return Collections.unmodifiableSet(mServiceCapabilities);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mId);
        dest.writeString(mIccId);
        dest.writeInt(mSimSlotIndex);
        dest.writeString(mDisplayName);
        dest.writeString(mCarrierName);
        dest.writeInt(mIconTint);
        dest.writeString(mNumber);
        dest.writeInt(mDataRoaming);
        dest.writeString(mMcc);
        dest.writeString(mMnc);
        dest.writeString(mCountryIso);
        dest.writeBoolean(mIsEmbedded);
        dest.writeInt(mCardId);
        dest.writeBoolean(mIsOpportunistic);
        dest.writeString(mGroupUuid == null ? null : mGroupUuid.toString());
        dest.writeInt(mCarrierId);
        dest.writeInt(mSubscriptionType);
        dest.writeInt(mPortIndex);
        dest.writeInt(mUsageSetting);
        dest.writeBoolean(mOnlyNonTerrestrialNetwork);
        int[] capabilities = new int[mServiceCapabilities.size()];
        int index = 0;
        for (int capability : mServiceCapabilities) capabilities[index++] = capability;
        dest.writeIntArray(capabilities);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof SubscriptionInfo)) return false;
        SubscriptionInfo info = (SubscriptionInfo)other;
        return mId == info.mId && mSimSlotIndex == info.mSimSlotIndex && mIconTint == info.mIconTint && mDataRoaming == info.mDataRoaming
            && mIsEmbedded == info.mIsEmbedded && mIsOpportunistic == info.mIsOpportunistic && mCarrierId == info.mCarrierId
            && mCardId == info.mCardId && mPortIndex == info.mPortIndex && mSubscriptionType == info.mSubscriptionType
            && mUsageSetting == info.mUsageSetting && mOnlyNonTerrestrialNetwork == info.mOnlyNonTerrestrialNetwork
            && Objects.equals(mIccId, info.mIccId) && Objects.equals(mDisplayName, info.mDisplayName)
            && Objects.equals(mCarrierName, info.mCarrierName) && Objects.equals(mNumber, info.mNumber) && Objects.equals(mMcc, info.mMcc)
            && Objects.equals(mMnc, info.mMnc) && Objects.equals(mCountryIso, info.mCountryIso)
            && Objects.equals(mGroupUuid, info.mGroupUuid) && mServiceCapabilities.equals(info.mServiceCapabilities);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mId, mSimSlotIndex, mIconTint, mDataRoaming, mIsEmbedded, mIsOpportunistic, mCarrierId, mCardId, mPortIndex,
            mSubscriptionType, mUsageSetting, mIccId, mDisplayName, mCarrierName, mNumber, mMcc, mMnc, mCountryIso, mGroupUuid,
            mServiceCapabilities);
    }

    @Override
    public String toString()
    {
        return "[SubscriptionInfo: id=" + mId + " iccId=" + mIccId + " simSlotIndex=" + mSimSlotIndex + " portIndex=" + mPortIndex
            + " isEmbedded=" + mIsEmbedded + " carrierId=" + mCarrierId + " displayName=" + mDisplayName + " carrierName=" + mCarrierName
            + " isOpportunistic=" + mIsOpportunistic + " groupUuid=" + mGroupUuid + " countryIso=" + mCountryIso + " mcc=" + mMcc
            + " mnc=" + mMnc + " cardId=" + mCardId + " subscriptionType=" + mSubscriptionType + " usageSetting=" + mUsageSetting + "]";
    }
}
