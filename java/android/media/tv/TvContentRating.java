package android.media.tv;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TvContentRating
{
    private static final String DELIMITER = "/";

    public static final TvContentRating UNRATED = new TvContentRating("null", "null", "null", null);

    private final String mDomain;
    private final String mRatingSystem;
    private final String mRating;
    private final String[] mSubRatings;
    private final int mHashCode;

    private TvContentRating(String domain, String ratingSystem, String rating, String[] subRatings)
    {
        mDomain = domain;
        mRatingSystem = ratingSystem;
        mRating = rating;

        if (subRatings == null || subRatings.length == 0)
        {
            mSubRatings = null;
        }
        else
        {
            mSubRatings = subRatings.clone();
            Arrays.sort(mSubRatings);
        }

        mHashCode = 31 * Objects.hash(mDomain, mRating) + Arrays.hashCode(mSubRatings);
    }

    public boolean contains(TvContentRating rating)
    {
        if (rating == null) throw new IllegalArgumentException("rating cannot be null");
        if (!rating.getMainRating().equals(mRating)) return false;
        if (!rating.getDomain().equals(mDomain) || !rating.getRatingSystem().equals(mRatingSystem)) return false;

        List<String> subRatings = getSubRatings();
        List<String> otherSubRatings = rating.getSubRatings();

        if (subRatings == null && otherSubRatings == null) return true;
        if (subRatings == null) return false;
        if (otherSubRatings == null) return true;

        return subRatings.containsAll(otherSubRatings);
    }

    public static TvContentRating createRating(String domain, String ratingSystem, String rating, String... subRatings)
    {
        if (TextUtils.isEmpty(domain)) throw new IllegalArgumentException("domain cannot be empty");
        if (TextUtils.isEmpty(ratingSystem)) throw new IllegalArgumentException("ratingSystem cannot be empty");
        if (TextUtils.isEmpty(rating)) throw new IllegalArgumentException("rating cannot be empty");

        return new TvContentRating(domain, ratingSystem, rating, subRatings);
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof TvContentRating)) return false;

        TvContentRating other = (TvContentRating)object;
        return mHashCode == other.mHashCode && mDomain.equals(other.mDomain) && mRatingSystem.equals(other.mRatingSystem)
            && mRating.equals(other.mRating) && Arrays.equals(mSubRatings, other.mSubRatings);
    }

    public String flattenToString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(mDomain).append(DELIMITER).append(mRatingSystem).append(DELIMITER).append(mRating);

        if (mSubRatings != null)
        {
            for (String subRating : mSubRatings)
            {
                builder.append(DELIMITER).append(subRating);
            }
        }

        return builder.toString();
    }

    public String getDomain()
    {
        return mDomain;
    }

    public String getMainRating()
    {
        return mRating;
    }

    public String getRatingSystem()
    {
        return mRatingSystem;
    }

    public List<String> getSubRatings()
    {
        if (mSubRatings == null) return null;

        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(mSubRatings)));
    }

    @Override
    public int hashCode()
    {
        return mHashCode;
    }

    public static TvContentRating unflattenFromString(String ratingString)
    {
        if (TextUtils.isEmpty(ratingString)) throw new IllegalArgumentException("ratingString cannot be empty");

        String[] strings = ratingString.split(DELIMITER);

        if (strings.length < 3) throw new IllegalArgumentException("Invalid rating string: " + ratingString);
        if (strings.length == 3) return new TvContentRating(strings[0], strings[1], strings[2], null);

        String[] subRatings = Arrays.copyOfRange(strings, 3, strings.length);
        return new TvContentRating(strings[0], strings[1], strings[2], subRatings);
    }
}
