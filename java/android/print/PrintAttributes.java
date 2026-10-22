package android.print;

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class PrintAttributes implements Parcelable
{
    public static final int COLOR_MODE_MONOCHROME = 1;
    public static final int COLOR_MODE_COLOR = 2;

    public static final int DUPLEX_MODE_NONE = 1;
    public static final int DUPLEX_MODE_LONG_EDGE = 2;
    public static final int DUPLEX_MODE_SHORT_EDGE = 4;

    public static final Parcelable.Creator<PrintAttributes> CREATOR = new Parcelable.Creator<PrintAttributes>()
    {
        @Override
        public PrintAttributes createFromParcel(Parcel source)
        {
            PrintAttributes attributes = new PrintAttributes();
            attributes.mMediaSize = source.readInt() != 0 ? MediaSize.createFromParcel(source) : null;
            attributes.mResolution = source.readInt() != 0 ? Resolution.createFromParcel(source) : null;
            attributes.mMinMargins = source.readInt() != 0 ? Margins.createFromParcel(source) : null;
            attributes.mColorMode = source.readInt();
            attributes.mDuplexMode = source.readInt();
            return attributes;
        }

        @Override
        public PrintAttributes[] newArray(int size)
        {
            return new PrintAttributes[size];
        }
    };

    private MediaSize mMediaSize;
    private Resolution mResolution;
    private Margins mMinMargins;
    private int mColorMode;
    private int mDuplexMode;

    PrintAttributes()
    {
    }

    public MediaSize getMediaSize()
    {
        return mMediaSize;
    }

    public Resolution getResolution()
    {
        return mResolution;
    }

    public Margins getMinMargins()
    {
        return mMinMargins;
    }

    public int getColorMode()
    {
        return mColorMode;
    }

    public int getDuplexMode()
    {
        return mDuplexMode;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mMediaSize != null ? 1 : 0);
        if (mMediaSize != null) mMediaSize.writeToParcel(dest);
        dest.writeInt(mResolution != null ? 1 : 0);
        if (mResolution != null) mResolution.writeToParcel(dest);
        dest.writeInt(mMinMargins != null ? 1 : 0);
        if (mMinMargins != null) mMinMargins.writeToParcel(dest);
        dest.writeInt(mColorMode);
        dest.writeInt(mDuplexMode);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mMediaSize, mResolution, mMinMargins, mColorMode, mDuplexMode);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PrintAttributes)) return false;

        PrintAttributes attributes = (PrintAttributes)other;
        return mColorMode == attributes.mColorMode && mDuplexMode == attributes.mDuplexMode
            && Objects.equals(mMediaSize, attributes.mMediaSize) && Objects.equals(mResolution, attributes.mResolution)
            && Objects.equals(mMinMargins, attributes.mMinMargins);
    }

    @Override
    public String toString()
    {
        return "PrintAttributes{mediaSize: " + mMediaSize + ", resolution: " + mResolution + ", minMargins: " + mMinMargins
            + ", colorMode: " + colorModeToString(mColorMode) + ", duplexMode: " + duplexModeToString(mDuplexMode) + "}";
    }

    private static String colorModeToString(int colorMode)
    {
        switch (colorMode)
        {
            case COLOR_MODE_MONOCHROME: return "COLOR_MODE_MONOCHROME";
            case COLOR_MODE_COLOR: return "COLOR_MODE_COLOR";
            default: return "COLOR_MODE_UNKNOWN";
        }
    }

    private static String duplexModeToString(int duplexMode)
    {
        switch (duplexMode)
        {
            case DUPLEX_MODE_NONE: return "DUPLEX_MODE_NONE";
            case DUPLEX_MODE_LONG_EDGE: return "DUPLEX_MODE_LONG_EDGE";
            case DUPLEX_MODE_SHORT_EDGE: return "DUPLEX_MODE_SHORT_EDGE";
            default: return "DUPLEX_MODE_UNKNOWN";
        }
    }

    public static final class MediaSize
    {
        public static final MediaSize UNKNOWN_PORTRAIT = new MediaSize("UNKNOWN_PORTRAIT", "Unknown portrait", 1, Integer.MAX_VALUE);
        public static final MediaSize UNKNOWN_LANDSCAPE = new MediaSize("UNKNOWN_LANDSCAPE", "Unknown landscape", Integer.MAX_VALUE, 1);

        public static final MediaSize ISO_A0 = new MediaSize("ISO_A0", "A0", 33110, 46810);
        public static final MediaSize ISO_A1 = new MediaSize("ISO_A1", "A1", 23390, 33110);
        public static final MediaSize ISO_A2 = new MediaSize("ISO_A2", "A2", 16540, 23390);
        public static final MediaSize ISO_A3 = new MediaSize("ISO_A3", "A3", 11690, 16540);
        public static final MediaSize ISO_A4 = new MediaSize("ISO_A4", "A4", 8270, 11690);
        public static final MediaSize ISO_A5 = new MediaSize("ISO_A5", "A5", 5830, 8270);
        public static final MediaSize ISO_A6 = new MediaSize("ISO_A6", "A6", 4130, 5830);
        public static final MediaSize ISO_A7 = new MediaSize("ISO_A7", "A7", 2910, 4130);
        public static final MediaSize ISO_A8 = new MediaSize("ISO_A8", "A8", 2050, 2910);
        public static final MediaSize ISO_A9 = new MediaSize("ISO_A9", "A9", 1460, 2050);
        public static final MediaSize ISO_A10 = new MediaSize("ISO_A10", "A10", 1020, 1460);

        public static final MediaSize ISO_B0 = new MediaSize("ISO_B0", "B0", 39370, 55670);
        public static final MediaSize ISO_B1 = new MediaSize("ISO_B1", "B1", 27830, 39370);
        public static final MediaSize ISO_B2 = new MediaSize("ISO_B2", "B2", 19690, 27830);
        public static final MediaSize ISO_B3 = new MediaSize("ISO_B3", "B3", 13900, 19690);
        public static final MediaSize ISO_B4 = new MediaSize("ISO_B4", "B4", 9840, 13900);
        public static final MediaSize ISO_B5 = new MediaSize("ISO_B5", "B5", 6930, 9840);
        public static final MediaSize ISO_B6 = new MediaSize("ISO_B6", "B6", 4920, 6930);
        public static final MediaSize ISO_B7 = new MediaSize("ISO_B7", "B7", 3460, 4920);
        public static final MediaSize ISO_B8 = new MediaSize("ISO_B8", "B8", 2440, 3460);
        public static final MediaSize ISO_B9 = new MediaSize("ISO_B9", "B9", 1730, 2440);
        public static final MediaSize ISO_B10 = new MediaSize("ISO_B10", "B10", 1220, 1730);

        public static final MediaSize ISO_C0 = new MediaSize("ISO_C0", "C0", 36100, 51060);
        public static final MediaSize ISO_C1 = new MediaSize("ISO_C1", "C1", 25510, 36100);
        public static final MediaSize ISO_C2 = new MediaSize("ISO_C2", "C2", 18030, 25510);
        public static final MediaSize ISO_C3 = new MediaSize("ISO_C3", "C3", 12760, 18030);
        public static final MediaSize ISO_C4 = new MediaSize("ISO_C4", "C4", 9020, 12760);
        public static final MediaSize ISO_C5 = new MediaSize("ISO_C5", "C5", 6380, 9020);
        public static final MediaSize ISO_C6 = new MediaSize("ISO_C6", "C6", 4490, 6380);
        public static final MediaSize ISO_C7 = new MediaSize("ISO_C7", "C7", 3190, 4490);
        public static final MediaSize ISO_C8 = new MediaSize("ISO_C8", "C8", 2240, 3190);
        public static final MediaSize ISO_C9 = new MediaSize("ISO_C9", "C9", 1570, 2240);
        public static final MediaSize ISO_C10 = new MediaSize("ISO_C10", "C10", 1100, 1570);

        public static final MediaSize NA_LETTER = new MediaSize("NA_LETTER", "Letter", 8500, 11000);
        public static final MediaSize NA_GOVT_LETTER = new MediaSize("NA_GOVT_LETTER", "Government Letter", 8000, 10500);
        public static final MediaSize NA_LEGAL = new MediaSize("NA_LEGAL", "Legal", 8500, 14000);
        public static final MediaSize NA_JUNIOR_LEGAL = new MediaSize("NA_JUNIOR_LEGAL", "Junior Legal", 8000, 5000);
        public static final MediaSize NA_LEDGER = new MediaSize("NA_LEDGER", "Ledger", 17000, 11000);
        public static final MediaSize NA_TABLOID = new MediaSize("NA_TABLOID", "Tabloid", 11000, 17000);
        public static final MediaSize NA_INDEX_3X5 = new MediaSize("NA_INDEX_3X5", "Index Card 3x5", 3000, 5000);
        public static final MediaSize NA_INDEX_4X6 = new MediaSize("NA_INDEX_4X6", "Index Card 4x6", 4000, 6000);
        public static final MediaSize NA_INDEX_5X8 = new MediaSize("NA_INDEX_5X8", "Index Card 5x8", 5000, 8000);
        public static final MediaSize NA_MONARCH = new MediaSize("NA_MONARCH", "Monarch", 7250, 10500);
        public static final MediaSize NA_QUARTO = new MediaSize("NA_QUARTO", "Quarto", 8000, 10000);
        public static final MediaSize NA_FOOLSCAP = new MediaSize("NA_FOOLSCAP", "Foolscap", 8000, 13000);
        public static final MediaSize ANSI_C = new MediaSize("ANSI_C", "ANSI C", 17000, 22000);
        public static final MediaSize ANSI_D = new MediaSize("ANSI_D", "ANSI D", 22000, 34000);
        public static final MediaSize ANSI_E = new MediaSize("ANSI_E", "ANSI E", 34000, 44000);
        public static final MediaSize ANSI_F = new MediaSize("ANSI_F", "ANSI F", 28000, 40000);
        public static final MediaSize NA_ARCH_A = new MediaSize("NA_ARCH_A", "Arch A", 9000, 12000);
        public static final MediaSize NA_ARCH_B = new MediaSize("NA_ARCH_B", "Arch B", 12000, 18000);
        public static final MediaSize NA_ARCH_C = new MediaSize("NA_ARCH_C", "Arch C", 18000, 24000);
        public static final MediaSize NA_ARCH_D = new MediaSize("NA_ARCH_D", "Arch D", 24000, 36000);
        public static final MediaSize NA_ARCH_E = new MediaSize("NA_ARCH_E", "Arch E", 36000, 48000);
        public static final MediaSize NA_ARCH_E1 = new MediaSize("NA_ARCH_E1", "Arch E1", 30000, 42000);
        public static final MediaSize NA_SUPER_B = new MediaSize("NA_SUPER_B", "Super B", 13000, 19000);

        public static final MediaSize ROC_8K = new MediaSize("ROC_8K", "8K", 10629, 15354);
        public static final MediaSize ROC_16K = new MediaSize("ROC_16K", "16K", 7677, 10629);

        public static final MediaSize PRC_1 = new MediaSize("PRC_1", "PRC 1", 4015, 6496);
        public static final MediaSize PRC_2 = new MediaSize("PRC_2", "PRC 2", 4015, 6929);
        public static final MediaSize PRC_3 = new MediaSize("PRC_3", "PRC 3", 4921, 6929);
        public static final MediaSize PRC_4 = new MediaSize("PRC_4", "PRC 4", 4330, 8189);
        public static final MediaSize PRC_5 = new MediaSize("PRC_5", "PRC 5", 4330, 8661);
        public static final MediaSize PRC_6 = new MediaSize("PRC_6", "PRC 6", 4724, 12599);
        public static final MediaSize PRC_7 = new MediaSize("PRC_7", "PRC 7", 6299, 9055);
        public static final MediaSize PRC_8 = new MediaSize("PRC_8", "PRC 8", 4724, 12165);
        public static final MediaSize PRC_9 = new MediaSize("PRC_9", "PRC 9", 9016, 12756);
        public static final MediaSize PRC_10 = new MediaSize("PRC_10", "PRC 10", 12756, 18032);
        public static final MediaSize PRC_16K = new MediaSize("PRC_16K", "PRC 16K", 5749, 8465);

        public static final MediaSize OM_PA_KAI = new MediaSize("OM_PA_KAI", "Pa Kai", 10512, 15315);
        public static final MediaSize OM_DAI_PA_KAI = new MediaSize("OM_DAI_PA_KAI", "Dai Pa Kai", 10827, 15551);
        public static final MediaSize OM_JUURO_KU_KAI = new MediaSize("OM_JUURO_KU_KAI", "Jurro Ku Kai", 7796, 10827);

        public static final MediaSize JIS_B10 = new MediaSize("JIS_B10", "JIS B10", 1259, 1772);
        public static final MediaSize JIS_B9 = new MediaSize("JIS_B9", "JIS B9", 1772, 2520);
        public static final MediaSize JIS_B8 = new MediaSize("JIS_B8", "JIS B8", 2520, 3583);
        public static final MediaSize JIS_B7 = new MediaSize("JIS_B7", "JIS B7", 3583, 5049);
        public static final MediaSize JIS_B6 = new MediaSize("JIS_B6", "JIS B6", 5049, 7165);
        public static final MediaSize JIS_B5 = new MediaSize("JIS_B5", "JIS B5", 7165, 10118);
        public static final MediaSize JIS_B4 = new MediaSize("JIS_B4", "JIS B4", 10118, 14331);
        public static final MediaSize JIS_B3 = new MediaSize("JIS_B3", "JIS B3", 14331, 20276);
        public static final MediaSize JIS_B2 = new MediaSize("JIS_B2", "JIS B2", 20276, 28661);
        public static final MediaSize JIS_B1 = new MediaSize("JIS_B1", "JIS B1", 28661, 40551);
        public static final MediaSize JIS_B0 = new MediaSize("JIS_B0", "JIS B0", 40551, 57323);
        public static final MediaSize JIS_EXEC = new MediaSize("JIS_EXEC", "JIS Exec", 8504, 12992);

        public static final MediaSize JPN_CHOU4 = new MediaSize("JPN_CHOU4", "Chou4", 3543, 8071);
        public static final MediaSize JPN_CHOU3 = new MediaSize("JPN_CHOU3", "Chou3", 4724, 9252);
        public static final MediaSize JPN_CHOU2 = new MediaSize("JPN_CHOU2", "Chou2", 4374, 5748);
        public static final MediaSize JPN_HAGAKI = new MediaSize("JPN_HAGAKI", "Hagaki", 3937, 5827);
        public static final MediaSize JPN_OUFUKU = new MediaSize("JPN_OUFUKU", "Oufuku", 5827, 7874);
        public static final MediaSize JPN_KAHU = new MediaSize("JPN_KAHU", "Kahu", 9449, 12681);
        public static final MediaSize JPN_KAKU2 = new MediaSize("JPN_KAKU2", "Kaku2", 9449, 13071);
        public static final MediaSize JPN_YOU4 = new MediaSize("JPN_YOU4", "You4", 4134, 9252);
        public static final MediaSize JPN_OE_PHOTO_L = new MediaSize("JPN_OE_PHOTO_L", "L", 3500, 4750);

        private final String mId;
        private final String mLabel;
        private final int mWidthMils;
        private final int mHeightMils;

        public MediaSize(String id, String label, int widthMils, int heightMils)
        {
            if (id == null || id.isEmpty()) throw new IllegalArgumentException("id cannot be empty.");
            if (label == null || label.isEmpty()) throw new IllegalArgumentException("label cannot be empty.");
            if (widthMils <= 0) throw new IllegalArgumentException("widthMils cannot be less than or equal to zero.");
            if (heightMils <= 0) throw new IllegalArgumentException("heightMils cannot be less than or equal to zero.");

            mId = id;
            mLabel = label;
            mWidthMils = widthMils;
            mHeightMils = heightMils;
        }

        public String getId()
        {
            return mId;
        }

        public String getLabel(PackageManager packageManager)
        {
            return mLabel;
        }

        public int getWidthMils()
        {
            return mWidthMils;
        }

        public int getHeightMils()
        {
            return mHeightMils;
        }

        public boolean isPortrait()
        {
            return mHeightMils >= mWidthMils;
        }

        public MediaSize asPortrait()
        {
            if (isPortrait()) return this;
            return new MediaSize(mId, mLabel, Math.min(mWidthMils, mHeightMils), Math.max(mWidthMils, mHeightMils));
        }

        public MediaSize asLandscape()
        {
            if (!isPortrait()) return this;
            return new MediaSize(mId, mLabel, Math.max(mWidthMils, mHeightMils), Math.min(mWidthMils, mHeightMils));
        }

        void writeToParcel(Parcel dest)
        {
            dest.writeString(mId);
            dest.writeString(mLabel);
            dest.writeInt(mWidthMils);
            dest.writeInt(mHeightMils);
        }

        static MediaSize createFromParcel(Parcel source)
        {
            String id = source.readString();
            String label = source.readString();
            int width = source.readInt();
            int height = source.readInt();
            return new MediaSize(id, label, width, height);
        }

        @Override
        public int hashCode()
        {
            return 31 * (31 + mWidthMils) + mHeightMils;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof MediaSize)) return false;

            MediaSize size = (MediaSize)other;
            return mWidthMils == size.mWidthMils && mHeightMils == size.mHeightMils;
        }

        @Override
        public String toString()
        {
            return "MediaSize{id: " + mId + ", label: " + mLabel + ", heightMils: " + mHeightMils + ", widthMils: " + mWidthMils + "}";
        }
    }

    public static final class Resolution
    {
        private final String mId;
        private final String mLabel;
        private final int mHorizontalDpi;
        private final int mVerticalDpi;

        public Resolution(String id, String label, int horizontalDpi, int verticalDpi)
        {
            if (id == null || id.isEmpty()) throw new IllegalArgumentException("id cannot be empty.");
            if (label == null || label.isEmpty()) throw new IllegalArgumentException("label cannot be empty.");
            if (horizontalDpi <= 0) throw new IllegalArgumentException("horizontalDpi cannot be less than or equal to zero.");
            if (verticalDpi <= 0) throw new IllegalArgumentException("verticalDpi cannot be less than or equal to zero.");

            mId = id;
            mLabel = label;
            mHorizontalDpi = horizontalDpi;
            mVerticalDpi = verticalDpi;
        }

        public String getId()
        {
            return mId;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public int getHorizontalDpi()
        {
            return mHorizontalDpi;
        }

        public int getVerticalDpi()
        {
            return mVerticalDpi;
        }

        void writeToParcel(Parcel dest)
        {
            dest.writeString(mId);
            dest.writeString(mLabel);
            dest.writeInt(mHorizontalDpi);
            dest.writeInt(mVerticalDpi);
        }

        static Resolution createFromParcel(Parcel source)
        {
            String id = source.readString();
            String label = source.readString();
            int horizontalDpi = source.readInt();
            int verticalDpi = source.readInt();
            return new Resolution(id, label, horizontalDpi, verticalDpi);
        }

        @Override
        public int hashCode()
        {
            return 31 * (31 + mHorizontalDpi) + mVerticalDpi;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof Resolution)) return false;

            Resolution resolution = (Resolution)other;
            return mHorizontalDpi == resolution.mHorizontalDpi && mVerticalDpi == resolution.mVerticalDpi;
        }

        @Override
        public String toString()
        {
            return "Resolution{id: " + mId + ", label: " + mLabel + ", horizontalDpi: " + mHorizontalDpi + ", verticalDpi: "
                + mVerticalDpi + "}";
        }
    }

    public static final class Margins
    {
        public static final Margins NO_MARGINS = new Margins(0, 0, 0, 0);

        private final int mLeftMils;
        private final int mTopMils;
        private final int mRightMils;
        private final int mBottomMils;

        public Margins(int leftMils, int topMils, int rightMils, int bottomMils)
        {
            mLeftMils = leftMils;
            mTopMils = topMils;
            mRightMils = rightMils;
            mBottomMils = bottomMils;
        }

        public int getLeftMils()
        {
            return mLeftMils;
        }

        public int getTopMils()
        {
            return mTopMils;
        }

        public int getRightMils()
        {
            return mRightMils;
        }

        public int getBottomMils()
        {
            return mBottomMils;
        }

        void writeToParcel(Parcel dest)
        {
            dest.writeInt(mLeftMils);
            dest.writeInt(mTopMils);
            dest.writeInt(mRightMils);
            dest.writeInt(mBottomMils);
        }

        static Margins createFromParcel(Parcel source)
        {
            int left = source.readInt();
            int top = source.readInt();
            int right = source.readInt();
            int bottom = source.readInt();
            return new Margins(left, top, right, bottom);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mLeftMils, mTopMils, mRightMils, mBottomMils);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof Margins)) return false;

            Margins margins = (Margins)other;
            return mLeftMils == margins.mLeftMils && mTopMils == margins.mTopMils && mRightMils == margins.mRightMils
                && mBottomMils == margins.mBottomMils;
        }

        @Override
        public String toString()
        {
            return "Margins{leftMils: " + mLeftMils + ", topMils: " + mTopMils + ", rightMils: " + mRightMils + ", bottomMils: "
                + mBottomMils + "}";
        }
    }

    public static final class Builder
    {
        private final PrintAttributes mAttributes = new PrintAttributes();

        public Builder()
        {
        }

        public Builder setMediaSize(MediaSize mediaSize)
        {
            mAttributes.mMediaSize = mediaSize;
            return this;
        }

        public Builder setResolution(Resolution resolution)
        {
            mAttributes.mResolution = resolution;
            return this;
        }

        public Builder setMinMargins(Margins margins)
        {
            mAttributes.mMinMargins = margins;
            return this;
        }

        public Builder setColorMode(int colorMode)
        {
            if (colorMode != COLOR_MODE_MONOCHROME && colorMode != COLOR_MODE_COLOR)
            {
                throw new IllegalArgumentException("invalid color mode: " + colorMode);
            }
            mAttributes.mColorMode = colorMode;
            return this;
        }

        public Builder setDuplexMode(int duplexMode)
        {
            if (duplexMode != DUPLEX_MODE_NONE && duplexMode != DUPLEX_MODE_LONG_EDGE && duplexMode != DUPLEX_MODE_SHORT_EDGE)
            {
                throw new IllegalArgumentException("invalid duplex mode: " + duplexMode);
            }
            mAttributes.mDuplexMode = duplexMode;
            return this;
        }

        public PrintAttributes build()
        {
            PrintAttributes attributes = new PrintAttributes();
            attributes.mMediaSize = mAttributes.mMediaSize;
            attributes.mResolution = mAttributes.mResolution;
            attributes.mMinMargins = mAttributes.mMinMargins;
            attributes.mColorMode = mAttributes.mColorMode;
            attributes.mDuplexMode = mAttributes.mDuplexMode;
            return attributes;
        }
    }
}
