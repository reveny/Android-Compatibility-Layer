package android.media;

import android.os.Parcel;
import android.os.Parcelable;

public final class Rating implements Parcelable
{
    public static final int RATING_3_STARS = 3;
    public static final int RATING_4_STARS = 4;
    public static final int RATING_5_STARS = 5;
    public static final int RATING_HEART = 1;
    public static final int RATING_NONE = 0;
    public static final int RATING_PERCENTAGE = 6;
    public static final int RATING_THUMB_UP_DOWN = 2;

    private static final float RATING_NOT_RATED = -1.0f;

    public static final Parcelable.Creator<Rating> CREATOR = new Parcelable.Creator<Rating>()
    {
        @Override
        public Rating createFromParcel(Parcel source)
        {
            return new Rating(source.readInt(), source.readFloat());
        }

        @Override
        public Rating[] newArray(int size)
        {
            return new Rating[size];
        }
    };

    private final int mRatingStyle;
    private final float mRatingValue;

    private Rating(int ratingStyle, float ratingValue)
    {
        mRatingStyle = ratingStyle;
        mRatingValue = ratingValue;
    }

    public static Rating newUnratedRating(int ratingStyle)
    {
        switch (ratingStyle)
        {
            case RATING_HEART:
            case RATING_THUMB_UP_DOWN:
            case RATING_3_STARS:
            case RATING_4_STARS:
            case RATING_5_STARS:
            case RATING_PERCENTAGE:
                return new Rating(ratingStyle, RATING_NOT_RATED);
            default:
                return null;
        }
    }

    public static Rating newHeartRating(boolean hasHeart)
    {
        return new Rating(RATING_HEART, hasHeart ? 1.0f : 0.0f);
    }

    public static Rating newThumbRating(boolean thumbIsUp)
    {
        return new Rating(RATING_THUMB_UP_DOWN, thumbIsUp ? 1.0f : 0.0f);
    }

    public static Rating newStarRating(int starRatingStyle, float starRating)
    {
        float maxRating;

        switch (starRatingStyle)
        {
            case RATING_3_STARS:
                maxRating = 3.0f;
                break;
            case RATING_4_STARS:
                maxRating = 4.0f;
                break;
            case RATING_5_STARS:
                maxRating = 5.0f;
                break;
            default:
                return null;
        }

        if (starRating < 0.0f || starRating > maxRating) return null;

        return new Rating(starRatingStyle, starRating);
    }

    public static Rating newPercentageRating(float percent)
    {
        if (percent < 0.0f || percent > 100.0f) return null;

        return new Rating(RATING_PERCENTAGE, percent);
    }

    public boolean isRated()
    {
        return mRatingValue >= 0.0f;
    }

    public int getRatingStyle()
    {
        return mRatingStyle;
    }

    public boolean hasHeart()
    {
        return mRatingStyle == RATING_HEART && mRatingValue == 1.0f;
    }

    public boolean isThumbUp()
    {
        return mRatingStyle == RATING_THUMB_UP_DOWN && mRatingValue == 1.0f;
    }

    public float getStarRating()
    {
        switch (mRatingStyle)
        {
            case RATING_3_STARS:
            case RATING_4_STARS:
            case RATING_5_STARS:
                return isRated() ? mRatingValue : -1.0f;
            default:
                return -1.0f;
        }
    }

    public float getPercentRating()
    {
        if (mRatingStyle != RATING_PERCENTAGE || !isRated()) return -1.0f;

        return mRatingValue;
    }

    @Override
    public int describeContents()
    {
        return mRatingStyle;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mRatingStyle);
        dest.writeFloat(mRatingValue);
    }

    @Override
    public String toString()
    {
        return "Rating:style=" + mRatingStyle + " rating=" + (mRatingValue < 0.0f ? "unrated" : String.valueOf(mRatingValue));
    }
}
