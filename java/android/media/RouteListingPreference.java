package android.media;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RouteListingPreference implements Parcelable
{
    public static final String ACTION_TRANSFER_MEDIA = "android.media.action.TRANSFER_MEDIA";
    public static final String EXTRA_ROUTE_ID = "android.media.extra.ROUTE_ID";

    public static final Parcelable.Creator<RouteListingPreference> CREATOR = new Parcelable.Creator<RouteListingPreference>()
    {
        @Override
        public RouteListingPreference createFromParcel(Parcel source)
        {
            List<Item> items = source.createTypedArrayList(Item.CREATOR);
            boolean useSystemOrdering = source.readBoolean();
            ComponentName linkedItemComponentName = source.readTypedObject(ComponentName.CREATOR);
            return new RouteListingPreference(items, useSystemOrdering, linkedItemComponentName);
        }

        @Override
        public RouteListingPreference[] newArray(int size)
        {
            return new RouteListingPreference[size];
        }
    };

    private final List<Item> mItems;
    private final boolean mUseSystemOrdering;
    private final ComponentName mLinkedItemComponentName;

    private RouteListingPreference(List<Item> items, boolean useSystemOrdering, ComponentName linkedItemComponentName)
    {
        mItems = Collections.unmodifiableList(new ArrayList<>(items == null ? new ArrayList<>() : items));
        mUseSystemOrdering = useSystemOrdering;
        mLinkedItemComponentName = linkedItemComponentName;
    }

    public List<Item> getItems()
    {
        return mItems;
    }

    public boolean getUseSystemOrdering()
    {
        return mUseSystemOrdering;
    }

    public ComponentName getLinkedItemComponentName()
    {
        return mLinkedItemComponentName;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeTypedList(mItems, flags);
        dest.writeBoolean(mUseSystemOrdering);
        dest.writeTypedObject(mLinkedItemComponentName, flags);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) return true;
        if (!(object instanceof RouteListingPreference)) return false;

        RouteListingPreference other = (RouteListingPreference)object;

        return mItems.equals(other.mItems) && mUseSystemOrdering == other.mUseSystemOrdering
                && Objects.equals(mLinkedItemComponentName, other.mLinkedItemComponentName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mItems, mUseSystemOrdering, mLinkedItemComponentName);
    }

    public static final class Builder
    {
        private List<Item> mItems = new ArrayList<>();
        private boolean mUseSystemOrdering = true;
        private ComponentName mLinkedItemComponentName;

        public Builder()
        {
        }

        public Builder setItems(List<Item> items)
        {
            if (items == null) throw new NullPointerException("items must not be null");

            mItems = new ArrayList<>(items);
            return this;
        }

        public Builder setUseSystemOrdering(boolean useSystemOrdering)
        {
            mUseSystemOrdering = useSystemOrdering;
            return this;
        }

        public Builder setLinkedItemComponentName(ComponentName linkedItemComponentName)
        {
            mLinkedItemComponentName = linkedItemComponentName;
            return this;
        }

        public RouteListingPreference build()
        {
            Set<String> routeIds = new HashSet<>();

            for (Item item : mItems)
            {
                if (!routeIds.add(item.getRouteId()))
                {
                    throw new IllegalArgumentException("Each Item must have a unique route id");
                }
            }

            return new RouteListingPreference(mItems, mUseSystemOrdering, mLinkedItemComponentName);
        }
    }

    public static final class Item implements Parcelable
    {
        public static final int FLAG_ONGOING_SESSION = 1;
        public static final int FLAG_ONGOING_SESSION_MANAGED = 2;
        public static final int FLAG_SUGGESTED = 4;
        public static final int SELECTION_BEHAVIOR_GO_TO_APP = 2;
        public static final int SELECTION_BEHAVIOR_NONE = 0;
        public static final int SELECTION_BEHAVIOR_TRANSFER = 1;
        public static final int SUBTEXT_AD_ROUTING_DISALLOWED = 4;
        public static final int SUBTEXT_CUSTOM = 10000;
        public static final int SUBTEXT_DEVICE_LOW_POWER = 5;
        public static final int SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED = 3;
        public static final int SUBTEXT_ERROR_UNKNOWN = 1;
        public static final int SUBTEXT_NONE = 0;
        public static final int SUBTEXT_SUBSCRIPTION_REQUIRED = 2;
        public static final int SUBTEXT_TRACK_UNSUPPORTED = 7;
        public static final int SUBTEXT_UNAUTHORIZED = 6;

        public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>()
        {
            @Override
            public Item createFromParcel(Parcel source)
            {
                String routeId = source.readString();
                int selectionBehavior = source.readInt();
                int flags = source.readInt();
                int subText = source.readInt();
                CharSequence customSubtextMessage = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
                return new Item(routeId, selectionBehavior, flags, subText, customSubtextMessage);
            }

            @Override
            public Item[] newArray(int size)
            {
                return new Item[size];
            }
        };

        private final String mRouteId;
        private final int mSelectionBehavior;
        private final int mFlags;
        private final int mSubText;
        private final CharSequence mCustomSubtextMessage;

        private Item(String routeId, int selectionBehavior, int flags, int subText, CharSequence customSubtextMessage)
        {
            mRouteId = routeId;
            mSelectionBehavior = selectionBehavior;
            mFlags = flags;
            mSubText = subText;
            mCustomSubtextMessage = customSubtextMessage;
        }

        public String getRouteId()
        {
            return mRouteId;
        }

        public int getSelectionBehavior()
        {
            return mSelectionBehavior;
        }

        public int getFlags()
        {
            return mFlags;
        }

        public int getSubText()
        {
            return mSubText;
        }

        public CharSequence getCustomSubtextMessage()
        {
            return mCustomSubtextMessage;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(mRouteId);
            dest.writeInt(mSelectionBehavior);
            dest.writeInt(mFlags);
            dest.writeInt(mSubText);
            TextUtils.writeToParcel(mCustomSubtextMessage, dest, flags);
        }

        @Override
        public boolean equals(Object object)
        {
            if (object == this) return true;
            if (!(object instanceof Item)) return false;

            Item other = (Item)object;

            return Objects.equals(mRouteId, other.mRouteId) && mSelectionBehavior == other.mSelectionBehavior
                    && mFlags == other.mFlags && mSubText == other.mSubText
                    && TextUtils.equals(mCustomSubtextMessage, other.mCustomSubtextMessage);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mRouteId, mSelectionBehavior, mFlags, mSubText, String.valueOf(mCustomSubtextMessage));
        }

        public static final class Builder
        {
            private final String mRouteId;
            private int mSelectionBehavior = SELECTION_BEHAVIOR_TRANSFER;
            private int mFlags;
            private int mSubText = SUBTEXT_NONE;
            private CharSequence mCustomSubtextMessage;

            public Builder(String routeId)
            {
                if (TextUtils.isEmpty(routeId)) throw new IllegalArgumentException("routeId must not be empty");

                mRouteId = routeId;
            }

            public Builder setSelectionBehavior(int selectionBehavior)
            {
                mSelectionBehavior = selectionBehavior;
                return this;
            }

            public Builder setFlags(int flags)
            {
                mFlags = flags;
                return this;
            }

            public Builder setSubText(int subText)
            {
                mSubText = subText;
                return this;
            }

            public Builder setCustomSubtextMessage(CharSequence customSubtextMessage)
            {
                mCustomSubtextMessage = customSubtextMessage;
                return this;
            }

            public Item build()
            {
                if (mSubText == SUBTEXT_CUSTOM && mCustomSubtextMessage == null)
                {
                    throw new IllegalStateException("The custom subtext message cannot be null if subtext is SUBTEXT_CUSTOM");
                }

                return new Item(mRouteId, mSelectionBehavior, mFlags, mSubText, mCustomSubtextMessage);
            }
        }
    }
}
