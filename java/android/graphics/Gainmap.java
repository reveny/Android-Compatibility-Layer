package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public final class Gainmap implements Parcelable
{
    public static final int GAINMAP_DIRECTION_SDR_TO_HDR = 0;
    public static final int GAINMAP_DIRECTION_HDR_TO_SDR = 1;

    public static final Parcelable.Creator<Gainmap> CREATOR = new Parcelable.Creator<Gainmap>()
    {
        @Override
        public Gainmap createFromParcel(Parcel source)
        {
            Gainmap gainmap = new Gainmap(source.readTypedObject(Bitmap.CREATOR));
            gainmap.mRatioMin = source.createFloatArray();
            gainmap.mRatioMax = source.createFloatArray();
            gainmap.mGamma = source.createFloatArray();
            gainmap.mEpsilonSdr = source.createFloatArray();
            gainmap.mEpsilonHdr = source.createFloatArray();
            gainmap.mDisplayRatioForFullHdr = source.readFloat();
            gainmap.mMinDisplayRatioForHdrTransition = source.readFloat();
            gainmap.mDirection = source.readInt();
            return gainmap;
        }

        @Override
        public Gainmap[] newArray(int size)
        {
            return new Gainmap[size];
        }
    };

    private Bitmap mGainmapContents;
    private float[] mRatioMin = {1.0f, 1.0f, 1.0f};
    private float[] mRatioMax = {2.0f, 2.0f, 2.0f};
    private float[] mGamma = {1.0f, 1.0f, 1.0f};
    private float[] mEpsilonSdr = {0.0f, 0.0f, 0.0f};
    private float[] mEpsilonHdr = {0.0f, 0.0f, 0.0f};
    private float mDisplayRatioForFullHdr = 2.0f;
    private float mMinDisplayRatioForHdrTransition = 1.0f;
    private int mDirection = GAINMAP_DIRECTION_SDR_TO_HDR;
    private ColorSpace mAlternativeImagePrimaries;

    public Gainmap(Bitmap gainmapContents)
    {
        setGainmapContents(gainmapContents);
    }

    public Gainmap(Gainmap gainmap, Bitmap gainmapContents)
    {
        this(gainmapContents);

        if (gainmap == null) throw new NullPointerException("gainmap");

        mRatioMin = gainmap.mRatioMin.clone();
        mRatioMax = gainmap.mRatioMax.clone();
        mGamma = gainmap.mGamma.clone();
        mEpsilonSdr = gainmap.mEpsilonSdr.clone();
        mEpsilonHdr = gainmap.mEpsilonHdr.clone();
        mDisplayRatioForFullHdr = gainmap.mDisplayRatioForFullHdr;
        mMinDisplayRatioForHdrTransition = gainmap.mMinDisplayRatioForHdrTransition;
        mDirection = gainmap.mDirection;
        mAlternativeImagePrimaries = gainmap.mAlternativeImagePrimaries;
    }

    public Bitmap getGainmapContents()
    {
        return mGainmapContents;
    }

    public void setGainmapContents(Bitmap bitmap)
    {
        if (bitmap != null && bitmap.isRecycled()) throw new IllegalArgumentException("Bitmap is recycled");

        mGainmapContents = bitmap;
    }

    public void setRatioMin(float r, float g, float b)
    {
        mRatioMin = new float[] {r, g, b};
    }

    public float[] getRatioMin()
    {
        return mRatioMin.clone();
    }

    public void setRatioMax(float r, float g, float b)
    {
        mRatioMax = new float[] {r, g, b};
    }

    public float[] getRatioMax()
    {
        return mRatioMax.clone();
    }

    public void setGamma(float r, float g, float b)
    {
        mGamma = new float[] {r, g, b};
    }

    public float[] getGamma()
    {
        return mGamma.clone();
    }

    public void setEpsilonSdr(float r, float g, float b)
    {
        mEpsilonSdr = new float[] {r, g, b};
    }

    public float[] getEpsilonSdr()
    {
        return mEpsilonSdr.clone();
    }

    public void setEpsilonHdr(float r, float g, float b)
    {
        mEpsilonHdr = new float[] {r, g, b};
    }

    public float[] getEpsilonHdr()
    {
        return mEpsilonHdr.clone();
    }

    public void setDisplayRatioForFullHdr(float max)
    {
        if (!Float.isFinite(max) || max < 1.0f)
            throw new IllegalArgumentException("setDisplayRatioForFullHdr must be >= 1.0f, got = " + max);

        mDisplayRatioForFullHdr = max;
    }

    public float getDisplayRatioForFullHdr()
    {
        return mDisplayRatioForFullHdr;
    }

    public void setMinDisplayRatioForHdrTransition(float min)
    {
        if (!Float.isFinite(min) || min < 1.0f)
            throw new IllegalArgumentException("setMinDisplayRatioForHdrTransition must be >= 1.0f, got = " + min);

        mMinDisplayRatioForHdrTransition = min;
    }

    public float getMinDisplayRatioForHdrTransition()
    {
        return mMinDisplayRatioForHdrTransition;
    }

    public void setGainmapDirection(int direction)
    {
        if (direction != GAINMAP_DIRECTION_SDR_TO_HDR && direction != GAINMAP_DIRECTION_HDR_TO_SDR)
            throw new IllegalArgumentException("Invalid gainmap direction: " + direction);

        mDirection = direction;
    }

    public int getGainmapDirection()
    {
        return mDirection;
    }

    public void setAlternativeImagePrimaries(ColorSpace colorSpace)
    {
        mAlternativeImagePrimaries = colorSpace;
    }

    public ColorSpace getAlternativeImagePrimaries()
    {
        return mAlternativeImagePrimaries;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeTypedObject(mGainmapContents, flags);
        dest.writeFloatArray(mRatioMin);
        dest.writeFloatArray(mRatioMax);
        dest.writeFloatArray(mGamma);
        dest.writeFloatArray(mEpsilonSdr);
        dest.writeFloatArray(mEpsilonHdr);
        dest.writeFloat(mDisplayRatioForFullHdr);
        dest.writeFloat(mMinDisplayRatioForHdrTransition);
        dest.writeInt(mDirection);
    }
}
