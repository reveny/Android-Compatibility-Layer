package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class RouteDiscoveryPreference implements Parcelable
{
    public static final Parcelable.Creator<RouteDiscoveryPreference> CREATOR = new Parcelable.Creator<RouteDiscoveryPreference>()
    {
        @Override
        public RouteDiscoveryPreference createFromParcel(Parcel source)
        {
            List<String> features = source.createStringArrayList();
            boolean activeScan = source.readBoolean();
            return new RouteDiscoveryPreference(features, activeScan);
        }

        @Override
        public RouteDiscoveryPreference[] newArray(int size)
        {
            return new RouteDiscoveryPreference[size];
        }
    };

    static final RouteDiscoveryPreference EMPTY = new RouteDiscoveryPreference(new ArrayList<>(), false);

    private final List<String> mPreferredFeatures;
    private final boolean mShouldPerformActiveScan;

    private RouteDiscoveryPreference(List<String> preferredFeatures, boolean shouldPerformActiveScan)
    {
        mPreferredFeatures = Collections.unmodifiableList(new ArrayList<>(preferredFeatures));
        mShouldPerformActiveScan = shouldPerformActiveScan;
    }

    public List<String> getPreferredFeatures()
    {
        return mPreferredFeatures;
    }

    public boolean shouldPerformActiveScan()
    {
        return mShouldPerformActiveScan;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeStringList(mPreferredFeatures);
        dest.writeBoolean(mShouldPerformActiveScan);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) return true;
        if (!(object instanceof RouteDiscoveryPreference)) return false;

        RouteDiscoveryPreference other = (RouteDiscoveryPreference)object;

        return new HashSet<>(mPreferredFeatures).equals(new HashSet<>(other.mPreferredFeatures))
                && mShouldPerformActiveScan == other.mShouldPerformActiveScan;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(new HashSet<>(mPreferredFeatures), mShouldPerformActiveScan);
    }

    @Override
    public String toString()
    {
        return "RouteDiscoveryRequest{ preferredFeatures={" + String.join(", ", mPreferredFeatures) + "}, activeScan="
                + mShouldPerformActiveScan + " }";
    }

    public static final class Builder
    {
        private List<String> mPreferredFeatures;
        private boolean mActiveScan;

        public Builder(List<String> preferredFeatures, boolean activeScan)
        {
            if (preferredFeatures == null) throw new NullPointerException("preferredFeatures must not be null");

            mPreferredFeatures = new ArrayList<>();

            for (String feature : preferredFeatures)
            {
                if (feature != null && !feature.isEmpty()) mPreferredFeatures.add(feature);
            }

            mActiveScan = activeScan;
        }

        public Builder(RouteDiscoveryPreference preference)
        {
            if (preference == null) throw new NullPointerException("preference must not be null");

            mPreferredFeatures = new ArrayList<>(preference.getPreferredFeatures());
            mActiveScan = preference.shouldPerformActiveScan();
        }

        public Builder setPreferredFeatures(List<String> preferredFeatures)
        {
            if (preferredFeatures == null) throw new NullPointerException("preferredFeatures must not be null");

            mPreferredFeatures = new ArrayList<>();

            for (String feature : preferredFeatures)
            {
                if (feature != null && !feature.isEmpty()) mPreferredFeatures.add(feature);
            }

            return this;
        }

        public Builder setShouldPerformActiveScan(boolean activeScan)
        {
            mActiveScan = activeScan;
            return this;
        }

        public RouteDiscoveryPreference build()
        {
            return new RouteDiscoveryPreference(mPreferredFeatures, mActiveScan);
        }
    }
}
