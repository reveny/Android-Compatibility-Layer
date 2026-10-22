package android.renderscript;

import android.graphics.ImageFormat;

public class Type extends BaseObj
{
    final Element mElement;
    final int mDimX;
    final int mDimY;
    final int mDimZ;
    final boolean mDimMipmaps;
    final boolean mDimFaces;
    final int mDimYuv;
    final int mElementCount;

    public enum CubemapFace
    {
        POSITIVE_X(0),
        NEGATIVE_X(1),
        POSITIVE_Y(2),
        NEGATIVE_Y(3),
        POSITIVE_Z(4),
        NEGATIVE_Z(5),
        @Deprecated
        POSITVE_X(0),
        @Deprecated
        POSITVE_Y(2),
        @Deprecated
        POSITVE_Z(4);

        final int mID;

        CubemapFace(int id)
        {
            mID = id;
        }
    }

    Type(RenderScript rs, Element element, int dimX, int dimY, int dimZ, boolean mipmaps, boolean faces, int yuv)
    {
        super(rs);

        mElement = element;
        mDimX = dimX;
        mDimY = dimY;
        mDimZ = dimZ;
        mDimMipmaps = mipmaps;
        mDimFaces = faces;
        mDimYuv = yuv;
        mElementCount = calculateElementCount();
    }

    private int calculateElementCount()
    {
        int x = Math.max(mDimX, 1);
        int y = Math.max(mDimY, 1);
        int z = Math.max(mDimZ, 1);
        int faces = mDimFaces ? 6 : 1;

        int count = x * y * z * faces;
        while (mDimMipmaps && (x > 1 || y > 1 || z > 1))
        {
            if (x > 1) x >>= 1;
            if (y > 1) y >>= 1;
            if (z > 1) z >>= 1;
            count += x * y * z * faces;
        }
        return count;
    }

    public Element getElement()
    {
        return mElement;
    }

    public int getX()
    {
        return mDimX;
    }

    public int getY()
    {
        return mDimY;
    }

    public int getZ()
    {
        return mDimZ;
    }

    public int getYuv()
    {
        return mDimYuv;
    }

    public boolean hasMipmaps()
    {
        return mDimMipmaps;
    }

    public boolean hasFaces()
    {
        return mDimFaces;
    }

    public int getCount()
    {
        return mElementCount;
    }

    public static Type createX(RenderScript rs, Element element, int dimX)
    {
        if (dimX < 1) throw new RSInvalidStateException("Dimension must be >= 1.");
        return new Type(rs, element, dimX, 0, 0, false, false, 0);
    }

    public static Type createXY(RenderScript rs, Element element, int dimX, int dimY)
    {
        if (dimX < 1 || dimY < 1) throw new RSInvalidStateException("Dimension must be >= 1.");
        return new Type(rs, element, dimX, dimY, 0, false, false, 0);
    }

    public static Type createXYZ(RenderScript rs, Element element, int dimX, int dimY, int dimZ)
    {
        if (dimX < 1 || dimY < 1 || dimZ < 1) throw new RSInvalidStateException("Dimension must be >= 1.");
        return new Type(rs, element, dimX, dimY, dimZ, false, false, 0);
    }

    public static class Builder
    {
        private final RenderScript mRS;
        private final Element mElement;

        private int mDimX = 1;
        private int mDimY;
        private int mDimZ;
        private boolean mDimMipmaps;
        private boolean mDimFaces;
        private int mYuv;

        public Builder(RenderScript rs, Element element)
        {
            element.checkValid();
            mRS = rs;
            mElement = element;
        }

        public Builder setX(int value)
        {
            if (value < 1) throw new RSIllegalArgumentException("Values of less than 1 for Dimension X are not valid.");
            mDimX = value;
            return this;
        }

        public Builder setY(int value)
        {
            if (value < 1) throw new RSIllegalArgumentException("Values of less than 1 for Dimension Y are not valid.");
            mDimY = value;
            return this;
        }

        public Builder setZ(int value)
        {
            if (value < 1) throw new RSIllegalArgumentException("Values of less than 1 for Dimension Z are not valid.");
            mDimZ = value;
            return this;
        }

        public Builder setMipmaps(boolean value)
        {
            mDimMipmaps = value;
            return this;
        }

        public Builder setFaces(boolean value)
        {
            mDimFaces = value;
            return this;
        }

        public Builder setYuvFormat(int yuvFormat)
        {
            switch (yuvFormat)
            {
                case ImageFormat.NV21:
                case ImageFormat.YV12:
                case ImageFormat.YUV_420_888:
                    break;
                default:
                    throw new RSIllegalArgumentException("Only ImageFormat.NV21, .YV12, and .YUV_420_888 are supported..");
            }

            mYuv = yuvFormat;
            return this;
        }

        public Type create()
        {
            if (mDimZ > 0)
            {
                if (mDimX < 1 || mDimY < 1) throw new RSInvalidStateException("Both X and Y dimension required when Z is present.");
                if (mDimFaces) throw new RSInvalidStateException("Cube maps not supported with 3D types.");
            }
            if (mDimY > 0 && mDimX < 1) throw new RSInvalidStateException("X dimension required when Y is present.");
            if (mDimFaces && mDimY < 1) throw new RSInvalidStateException("Cube maps require 2D Types.");
            if (mYuv != 0 && (mDimZ != 0 || mDimFaces || mDimMipmaps))
            {
                throw new RSInvalidStateException("YUV only supports basic 2D.");
            }

            return new Type(mRS, mElement, mDimX, mDimY, mDimZ, mDimMipmaps, mDimFaces, mYuv);
        }
    }
}
