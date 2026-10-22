package android.app;

import android.os.Bundle;

public class BroadcastOptions
{
    public static final int DEFERRAL_POLICY_DEFAULT = 0;
    public static final int DEFERRAL_POLICY_NONE = 1;
    public static final int DEFERRAL_POLICY_UNTIL_ACTIVE = 2;

    public static final int DELIVERY_GROUP_POLICY_ALL = 0;
    public static final int DELIVERY_GROUP_POLICY_MOST_RECENT = 1;

    private static final String KEY_SHARE_IDENTITY = "android:broadcast.shareIdentity";
    private static final String KEY_DEFERRAL_POLICY = "android:broadcast.deferralPolicy";
    private static final String KEY_DELIVERY_GROUP_POLICY = "android:broadcast.deliveryGroupPolicy";
    private static final String KEY_DELIVERY_GROUP_NAMESPACE = "android:broadcast.deliveryGroupMatchingNamespace";
    private static final String KEY_DELIVERY_GROUP_KEY = "android:broadcast.deliveryGroupMatchingKey";

    private boolean mShareIdentity;
    private int mDeferralPolicy = DEFERRAL_POLICY_DEFAULT;
    private int mDeliveryGroupPolicy = DELIVERY_GROUP_POLICY_ALL;
    private String mDeliveryGroupNamespace;
    private String mDeliveryGroupKey;

    private BroadcastOptions()
    {
    }

    public static BroadcastOptions makeBasic()
    {
        return new BroadcastOptions();
    }

    public static BroadcastOptions fromBundle(Bundle options)
    {
        BroadcastOptions result = new BroadcastOptions();
        if (options == null) return result;

        result.mShareIdentity = options.getBoolean(KEY_SHARE_IDENTITY, false);
        result.mDeferralPolicy = options.getInt(KEY_DEFERRAL_POLICY, DEFERRAL_POLICY_DEFAULT);
        result.mDeliveryGroupPolicy = options.getInt(KEY_DELIVERY_GROUP_POLICY, DELIVERY_GROUP_POLICY_ALL);
        result.mDeliveryGroupNamespace = options.getString(KEY_DELIVERY_GROUP_NAMESPACE);
        result.mDeliveryGroupKey = options.getString(KEY_DELIVERY_GROUP_KEY);
        return result;
    }

    public BroadcastOptions setShareIdentityEnabled(boolean shareIdentityEnabled)
    {
        mShareIdentity = shareIdentityEnabled;
        return this;
    }

    public boolean isShareIdentityEnabled()
    {
        return mShareIdentity;
    }

    public BroadcastOptions setDeferralPolicy(int deferralPolicy)
    {
        mDeferralPolicy = deferralPolicy;
        return this;
    }

    public int getDeferralPolicy()
    {
        return mDeferralPolicy;
    }

    public void clearDeferralPolicy()
    {
        mDeferralPolicy = DEFERRAL_POLICY_DEFAULT;
    }

    public BroadcastOptions setDeliveryGroupPolicy(int policy)
    {
        mDeliveryGroupPolicy = policy;
        return this;
    }

    public int getDeliveryGroupPolicy()
    {
        return mDeliveryGroupPolicy;
    }

    public void clearDeliveryGroupPolicy()
    {
        mDeliveryGroupPolicy = DELIVERY_GROUP_POLICY_ALL;
    }

    public BroadcastOptions setDeliveryGroupMatchingKey(String namespace, String key)
    {
        if (namespace == null || key == null) throw new NullPointerException("namespace and key must not be null");

        mDeliveryGroupNamespace = namespace;
        mDeliveryGroupKey = key;
        return this;
    }

    public String getDeliveryGroupMatchingKey()
    {
        if (mDeliveryGroupNamespace == null || mDeliveryGroupKey == null) return null;

        return mDeliveryGroupNamespace + ":" + mDeliveryGroupKey;
    }

    public void clearDeliveryGroupMatchingKey()
    {
        mDeliveryGroupNamespace = null;
        mDeliveryGroupKey = null;
    }

    public Bundle toBundle()
    {
        Bundle bundle = new Bundle();

        if (mShareIdentity) bundle.putBoolean(KEY_SHARE_IDENTITY, true);
        if (mDeferralPolicy != DEFERRAL_POLICY_DEFAULT) bundle.putInt(KEY_DEFERRAL_POLICY, mDeferralPolicy);
        if (mDeliveryGroupPolicy != DELIVERY_GROUP_POLICY_ALL) bundle.putInt(KEY_DELIVERY_GROUP_POLICY, mDeliveryGroupPolicy);

        if (mDeliveryGroupNamespace != null)
        {
            bundle.putString(KEY_DELIVERY_GROUP_NAMESPACE, mDeliveryGroupNamespace);
            bundle.putString(KEY_DELIVERY_GROUP_KEY, mDeliveryGroupKey);
        }

        return bundle;
    }
}
