package android.graphics;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

public abstract class ColorSpace
{
    public static final float[] ILLUMINANT_A = {0.44757f, 0.40745f};
    public static final float[] ILLUMINANT_B = {0.34570f, 0.35850f};
    public static final float[] ILLUMINANT_C = {0.31006f, 0.31616f};
    public static final float[] ILLUMINANT_D50 = {0.34567f, 0.35850f};
    public static final float[] ILLUMINANT_D55 = {0.33242f, 0.34743f};
    public static final float[] ILLUMINANT_D60 = {0.32168f, 0.33767f};
    public static final float[] ILLUMINANT_D65 = {0.31271f, 0.32902f};
    public static final float[] ILLUMINANT_D75 = {0.29902f, 0.31485f};
    public static final float[] ILLUMINANT_E = {0.33333f, 0.33333f};

    public static final int MIN_ID = -1;
    public static final int MAX_ID = 63;

    private static final float[] SRGB_PRIMARIES = {0.640f, 0.330f, 0.300f, 0.600f, 0.150f, 0.060f};
    private static final float[] NTSC_1953_PRIMARIES = {0.67f, 0.33f, 0.21f, 0.71f, 0.14f, 0.08f};
    private static final float[] BT2020_PRIMARIES = {0.708f, 0.292f, 0.170f, 0.797f, 0.131f, 0.046f};
    private static final float[] DCI_P3_PRIMARIES = {0.680f, 0.320f, 0.265f, 0.690f, 0.150f, 0.060f};
    private static final float[] ILLUMINANT_D50_XYZ = {0.964212f, 1.0f, 0.825188f};

    private static final double TYPE_PQ = -2.0;
    private static final double TYPE_HLG = -3.0;

    private static final Rgb.TransferParameters SRGB_TRANSFER_PARAMETERS =
        new Rgb.TransferParameters(1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045, 2.4);
    private static final Rgb.TransferParameters BT709_TRANSFER_PARAMETERS =
        new Rgb.TransferParameters(1 / 1.099, 0.099 / 1.099, 1 / 4.5, 0.081, 1 / 0.45);
    private static final Rgb.TransferParameters BT2020_TRANSFER_PARAMETERS =
        new Rgb.TransferParameters(1 / 1.0993, 0.0993 / 1.0993, 1 / 4.5, 0.08145, 1 / 0.45);
    private static final Rgb.TransferParameters BT2020_HLG_TRANSFER_PARAMETERS =
        new Rgb.TransferParameters(2.0, 2.0, 1 / 0.17883277, 0.28466892, 0.55991073, -0.685490157, TYPE_HLG);
    private static final Rgb.TransferParameters BT2020_PQ_TRANSFER_PARAMETERS =
        new Rgb.TransferParameters(-1.555223, 1.860454, 32 / 2523.0, 2413 / 128.0, -2392 / 128.0, 8192 / 1305.0, TYPE_PQ);

    private static final int DATASPACE_UNKNOWN = 0;
    private static final int DATASPACE_SRGB = 142671872;
    private static final int DATASPACE_SRGB_LINEAR = 138477568;
    private static final int DATASPACE_SCRGB = 411107328;
    private static final int DATASPACE_SCRGB_LINEAR = 406913024;
    private static final int DATASPACE_BT709 = 281083904;
    private static final int DATASPACE_BT2020 = 147193856;
    private static final int DATASPACE_DCI_P3 = 155844608;
    private static final int DATASPACE_DISPLAY_P3 = 143261696;
    private static final int DATASPACE_ADOBE_RGB = 151715840;
    private static final int DATASPACE_BT2020_HLG = 168165376;
    private static final int DATASPACE_BT2020_PQ = 163971072;

    private static final ColorSpace[] sSpaces = new ColorSpace[Named.values().length];

    private final String mName;
    private final Model mModel;
    private final int mId;

    ColorSpace(String name, Model model, int id)
    {
        if (name == null || name.length() < 1)
            throw new IllegalArgumentException("The name of a color space cannot be null and must contain at least 1 character");
        if (model == null) throw new IllegalArgumentException("A color space must have a model");
        if (id < MIN_ID || id > MAX_ID) throw new IllegalArgumentException("The id must be between " + MIN_ID + " and " + MAX_ID);

        mName = name;
        mModel = model;
        mId = id;
    }

    public String getName()
    {
        return mName;
    }

    public int getId()
    {
        return mId;
    }

    public Model getModel()
    {
        return mModel;
    }

    public int getComponentCount()
    {
        return mModel.getComponentCount();
    }

    public int getDataSpace()
    {
        if (mId < 0 || mId >= Named.values().length) return DATASPACE_UNKNOWN;

        switch (Named.values()[mId])
        {
            case SRGB:
                return DATASPACE_SRGB;
            case LINEAR_SRGB:
                return DATASPACE_SRGB_LINEAR;
            case EXTENDED_SRGB:
                return DATASPACE_SCRGB;
            case LINEAR_EXTENDED_SRGB:
                return DATASPACE_SCRGB_LINEAR;
            case BT709:
                return DATASPACE_BT709;
            case BT2020:
                return DATASPACE_BT2020;
            case DCI_P3:
                return DATASPACE_DCI_P3;
            case DISPLAY_P3:
                return DATASPACE_DISPLAY_P3;
            case ADOBE_RGB:
                return DATASPACE_ADOBE_RGB;
            case BT2020_HLG:
                return DATASPACE_BT2020_HLG;
            case BT2020_PQ:
                return DATASPACE_BT2020_PQ;
            default:
                return DATASPACE_UNKNOWN;
        }
    }

    public static ColorSpace getFromDataSpace(int dataSpace)
    {
        for (Named named : Named.values())
        {
            ColorSpace space = get(named);
            if (space.getDataSpace() == dataSpace && dataSpace != DATASPACE_UNKNOWN) return space;
        }

        return null;
    }

    public boolean isSrgb()
    {
        return false;
    }

    public abstract boolean isWideGamut();

    public abstract float getMinValue(int component);

    public abstract float getMaxValue(int component);

    public float[] toXyz(float r, float g, float b)
    {
        return toXyz(new float[] {r, g, b});
    }

    public abstract float[] toXyz(float[] value);

    public float[] fromXyz(float x, float y, float z)
    {
        float[] xyz = new float[mModel.getComponentCount()];
        xyz[0] = x;
        xyz[1] = y;
        xyz[2] = z;
        return fromXyz(xyz);
    }

    public abstract float[] fromXyz(float[] value);

    @Override
    public String toString()
    {
        return mName + " (id=" + mId + ", model=" + mModel + ")";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ColorSpace space = (ColorSpace)other;
        return mId == space.mId && mName.equals(space.mName) && mModel == space.mModel;
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * mName.hashCode() + mModel.hashCode()) + mId;
    }

    public static Connector connect(ColorSpace source, ColorSpace destination)
    {
        return connect(source, destination, RenderIntent.PERCEPTUAL);
    }

    public static Connector connect(ColorSpace source, ColorSpace destination, RenderIntent intent)
    {
        if (source == null || destination == null || intent == null) throw new NullPointerException();
        if (source.equals(destination)) return Connector.identity(source);

        return new Connector(source, destination, intent);
    }

    public static Connector connect(ColorSpace source)
    {
        return connect(source, RenderIntent.PERCEPTUAL);
    }

    public static Connector connect(ColorSpace source, RenderIntent intent)
    {
        if (source.isSrgb()) return Connector.identity(source);

        return connect(source, get(Named.SRGB), intent);
    }

    public static ColorSpace adapt(ColorSpace colorSpace, float[] whitePoint)
    {
        return adapt(colorSpace, whitePoint, Adaptation.BRADFORD);
    }

    public static ColorSpace adapt(ColorSpace colorSpace, float[] whitePoint, Adaptation adaptation)
    {
        if (colorSpace == null || whitePoint == null || adaptation == null) throw new NullPointerException();
        if (colorSpace.getModel() != Model.RGB) return colorSpace;

        Rgb rgb = (Rgb)colorSpace;
        if (compare(rgb.mWhitePoint, xyWhitePoint(whitePoint))) return colorSpace;

        float[] transform = adaptationMatrix(adaptation.mTransform, xyYToXyz(rgb.mWhitePoint),
                                                whitePoint.length == 3 ? Arrays.copyOf(whitePoint, 3) : xyYToXyz(whitePoint));
        float[] adapted = mul3x3(transform, rgb.mTransform);

        return new Rgb(rgb, adapted, whitePoint);
    }

    public static float[] cctToXyz(int cct)
    {
        if (cct < 1) throw new IllegalArgumentException("Temperature must be greater than 0");

        double temperature = cct;
        double inverse = 1.0 / temperature;
        double inverse2 = inverse * inverse;
        double inverse3 = inverse2 * inverse;

        double x;
        if (temperature <= 4000.0)
            x = -0.2661239e9 * inverse3 - 0.2343589e6 * inverse2 + 0.8776956e3 * inverse + 0.179910;
        else
            x = -3.0258469e9 * inverse3 + 2.1070379e6 * inverse2 + 0.2226347e3 * inverse + 0.240390;

        double x2 = x * x;
        double x3 = x2 * x;

        double y;
        if (temperature <= 2222.0)
            y = -1.1063814 * x3 - 1.34811020 * x2 + 2.18555832 * x - 0.20219683;
        else if (temperature <= 4000.0)
            y = -0.9549476 * x3 - 1.37418593 * x2 + 2.09137015 * x - 0.16748867;
        else
            y = 3.0817580 * x3 - 5.87338670 * x2 + 3.75112997 * x - 0.37001483;

        return new float[] {(float)(x / y), 1.0f, (float)((1.0 - x - y) / y)};
    }

    public static float[] chromaticAdaptation(Adaptation adaptation, float[] srcWhitePoint, float[] dstWhitePoint)
    {
        if (adaptation == null || srcWhitePoint == null || dstWhitePoint == null) throw new NullPointerException();
        if ((srcWhitePoint.length != 2 && srcWhitePoint.length != 3) || (dstWhitePoint.length != 2 && dstWhitePoint.length != 3))
            throw new IllegalArgumentException("A white point array must have 2 or 3 floats");

        float[] source = srcWhitePoint.length == 3 ? Arrays.copyOf(srcWhitePoint, 3) : xyYToXyz(srcWhitePoint);
        float[] destination = dstWhitePoint.length == 3 ? Arrays.copyOf(dstWhitePoint, 3) : xyYToXyz(dstWhitePoint);

        if (compare(source, destination)) return new float[] {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};

        return adaptationMatrix(adaptation.mTransform, source, destination);
    }

    public static ColorSpace match(float[] toXYZD50, Rgb.TransferParameters function)
    {
        if (toXYZD50 == null || function == null) throw new NullPointerException();

        for (Named named : Named.values())
        {
            ColorSpace space = get(named);
            if (space.getModel() != Model.RGB) continue;

            Rgb rgb = (Rgb)adapt(space, ILLUMINANT_D50_XYZ);
            if (compare(toXYZD50, rgb.mTransform) && function.equals(rgb.mTransferParameters)) return space;
        }

        return null;
    }

    public static ColorSpace get(Named name)
    {
        if (name == null) throw new NullPointerException("name");

        int index = name.ordinal();

        synchronized (sSpaces)
        {
            if (sSpaces[index] == null) sSpaces[index] = create(name);

            return sSpaces[index];
        }
    }

    private static ColorSpace create(Named name)
    {
        int id = name.ordinal();

        switch (name)
        {
            case SRGB:
                return new Rgb("sRGB IEC61966-2.1", SRGB_PRIMARIES, ILLUMINANT_D65, null, SRGB_TRANSFER_PARAMETERS, id);
            case LINEAR_SRGB:
                return new Rgb("sRGB IEC61966-2.1 (Linear)", SRGB_PRIMARIES, ILLUMINANT_D65, 1.0, 0.0f, 1.0f, id);
            case EXTENDED_SRGB:
                return new Rgb("scRGB-nl IEC 61966-2-2:2003", SRGB_PRIMARIES, ILLUMINANT_D65, null,
                               x -> absoluteResponse(x, SRGB_TRANSFER_PARAMETERS, false),
                               x -> absoluteResponse(x, SRGB_TRANSFER_PARAMETERS, true), -0.799f, 2.399f,
                               SRGB_TRANSFER_PARAMETERS, id);
            case LINEAR_EXTENDED_SRGB:
                return new Rgb("scRGB IEC 61966-2-2:2003", SRGB_PRIMARIES, ILLUMINANT_D65, 1.0, -0.5f, 7.499f, id);
            case BT709:
                return new Rgb("Rec. ITU-R BT.709-5", SRGB_PRIMARIES, ILLUMINANT_D65, null, BT709_TRANSFER_PARAMETERS, id);
            case BT2020:
                return new Rgb("Rec. ITU-R BT.2020-1", BT2020_PRIMARIES, ILLUMINANT_D65, null, BT2020_TRANSFER_PARAMETERS, id);
            case DCI_P3:
                return new Rgb("SMPTE RP 431-2-2007 DCI (P3)", DCI_P3_PRIMARIES, new float[] {0.314f, 0.351f}, 2.6, 0.0f, 1.0f, id);
            case DISPLAY_P3:
                return new Rgb("Display P3", DCI_P3_PRIMARIES, ILLUMINANT_D65, null, SRGB_TRANSFER_PARAMETERS, id);
            case NTSC_1953:
                return new Rgb("NTSC (1953)", NTSC_1953_PRIMARIES, ILLUMINANT_C, null, BT709_TRANSFER_PARAMETERS, id);
            case SMPTE_C:
                return new Rgb("SMPTE-C RGB", new float[] {0.630f, 0.340f, 0.310f, 0.595f, 0.155f, 0.070f}, ILLUMINANT_D65, null,
                               BT709_TRANSFER_PARAMETERS, id);
            case ADOBE_RGB:
                return new Rgb("Adobe RGB (1998)", new float[] {0.64f, 0.33f, 0.21f, 0.71f, 0.15f, 0.06f}, ILLUMINANT_D65, 2.2,
                               0.0f, 1.0f, id);
            case PRO_PHOTO_RGB:
                return new Rgb("ROMM RGB ISO 22028-2:2013", new float[] {0.7347f, 0.2653f, 0.1596f, 0.8404f, 0.0366f, 0.0001f},
                               ILLUMINANT_D50, null, new Rgb.TransferParameters(1.0, 0.0, 1 / 16.0, 0.031248, 1.8), id);
            case ACES:
                return new Rgb("SMPTE ST 2065-1:2012 ACES", new float[] {0.73470f, 0.26530f, 0.0f, 1.0f, 0.00010f, -0.0770f},
                               ILLUMINANT_D60, 1.0, -65504.0f, 65504.0f, id);
            case ACESCG:
                return new Rgb("Academy S-2014-004 ACEScg", new float[] {0.713f, 0.293f, 0.165f, 0.830f, 0.128f, 0.044f},
                               ILLUMINANT_D60, 1.0, -65504.0f, 65504.0f, id);
            case CIE_XYZ:
                return new Xyz("Generic XYZ", id);
            case CIE_LAB:
                return new Lab("Generic L*a*b*", id);
            case BT2020_HLG:
                return new Rgb("Hybrid Log Gamma encoding", BT2020_PRIMARIES, ILLUMINANT_D65, null, ColorSpace::hybridLogGammaOetf,
                               ColorSpace::hybridLogGammaEotf, 0.0f, 1.0f, BT2020_HLG_TRANSFER_PARAMETERS, id);
            case BT2020_PQ:
                return new Rgb("Perceptual Quantizer encoding", BT2020_PRIMARIES, ILLUMINANT_D65, null,
                               ColorSpace::perceptualQuantizerOetf, ColorSpace::perceptualQuantizerEotf, 0.0f, 1.0f,
                               BT2020_PQ_TRANSFER_PARAMETERS, id);
            case OK_LAB:
                return new OkLab("Oklab", id);
            default:
                throw new IllegalArgumentException("unknown color space " + name);
        }
    }

    private static double absoluteResponse(double x, Rgb.TransferParameters parameters, boolean decode)
    {
        double magnitude = Math.abs(x);
        double value = decode ? parameters.decode(magnitude) : parameters.encode(magnitude);
        return Math.copySign(value, x);
    }

    private static double hybridLogGammaEotf(double x)
    {
        double a = 0.17883277;
        double b = 0.28466892;
        double c = 0.55991073;

        if (x <= 0.5) return x * x / 3.0;
        return (Math.exp((x - c) / a) + b) / 12.0;
    }

    private static double hybridLogGammaOetf(double x)
    {
        double a = 0.17883277;
        double b = 0.28466892;
        double c = 0.55991073;

        if (x <= 0.0) return 0.0;
        if (x <= 1.0 / 12.0) return Math.sqrt(3.0 * x);
        return a * Math.log(12.0 * x - b) + c;
    }

    private static double perceptualQuantizerEotf(double x)
    {
        double m1 = 2610.0 / 16384.0;
        double m2 = 2523.0 / 4096.0 * 128.0;
        double c1 = 3424.0 / 4096.0;
        double c2 = 2413.0 / 4096.0 * 32.0;
        double c3 = 2392.0 / 4096.0 * 32.0;

        double power = Math.pow(Math.max(x, 0.0), 1.0 / m2);
        return Math.pow(Math.max(power - c1, 0.0) / (c2 - c3 * power), 1.0 / m1);
    }

    private static double perceptualQuantizerOetf(double x)
    {
        double m1 = 2610.0 / 16384.0;
        double m2 = 2523.0 / 4096.0 * 128.0;
        double c1 = 3424.0 / 4096.0;
        double c2 = 2413.0 / 4096.0 * 32.0;
        double c3 = 2392.0 / 4096.0 * 32.0;

        double power = Math.pow(Math.max(x, 0.0), m1);
        return Math.pow((c1 + c2 * power) / (1.0 + c3 * power), m2);
    }

    private static boolean compare(float[] first, float[] second)
    {
        if (first == second) return true;
        if (first.length != second.length) return false;

        for (int index = 0; index < first.length; index++)
        {
            if (Float.compare(first[index], second[index]) != 0 && Math.abs(first[index] - second[index]) > 1e-3f) return false;
        }

        return true;
    }

    static float[] xyYToXyz(float[] xy)
    {
        return new float[] {xy[0] / xy[1], 1.0f, (1 - xy[0] - xy[1]) / xy[1]};
    }

    static float[] xyWhitePoint(float[] whitePoint)
    {
        if (whitePoint.length == 2) return Arrays.copyOf(whitePoint, 2);

        float sum = whitePoint[0] + whitePoint[1] + whitePoint[2];
        return new float[] {whitePoint[0] / sum, whitePoint[1] / sum};
    }

    static float[] xyPrimaries(float[] primaries)
    {
        if (primaries.length == 6) return Arrays.copyOf(primaries, 6);

        float[] xy = new float[6];

        for (int index = 0; index < 3; index++)
        {
            float sum = primaries[index * 3] + primaries[index * 3 + 1] + primaries[index * 3 + 2];
            xy[index * 2] = primaries[index * 3] / sum;
            xy[index * 2 + 1] = primaries[index * 3 + 1] / sum;
        }

        return xy;
    }

    static float[] inverse3x3(float[] m)
    {
        float a = m[0];
        float b = m[3];
        float c = m[6];
        float d = m[1];
        float e = m[4];
        float f = m[7];
        float g = m[2];
        float h = m[5];
        float i = m[8];

        float xA = e * i - f * h;
        float xB = f * g - d * i;
        float xC = d * h - e * g;

        float determinant = a * xA + b * xB + c * xC;

        float[] inverted = new float[m.length];
        inverted[0] = xA / determinant;
        inverted[1] = xB / determinant;
        inverted[2] = xC / determinant;
        inverted[3] = (c * h - b * i) / determinant;
        inverted[4] = (a * i - c * g) / determinant;
        inverted[5] = (b * g - a * h) / determinant;
        inverted[6] = (b * f - c * e) / determinant;
        inverted[7] = (c * d - a * f) / determinant;
        inverted[8] = (a * e - b * d) / determinant;
        return inverted;
    }

    static float[] mul3x3(float[] lhs, float[] rhs)
    {
        float[] r = new float[9];
        r[0] = lhs[0] * rhs[0] + lhs[3] * rhs[1] + lhs[6] * rhs[2];
        r[1] = lhs[1] * rhs[0] + lhs[4] * rhs[1] + lhs[7] * rhs[2];
        r[2] = lhs[2] * rhs[0] + lhs[5] * rhs[1] + lhs[8] * rhs[2];
        r[3] = lhs[0] * rhs[3] + lhs[3] * rhs[4] + lhs[6] * rhs[5];
        r[4] = lhs[1] * rhs[3] + lhs[4] * rhs[4] + lhs[7] * rhs[5];
        r[5] = lhs[2] * rhs[3] + lhs[5] * rhs[4] + lhs[8] * rhs[5];
        r[6] = lhs[0] * rhs[6] + lhs[3] * rhs[7] + lhs[6] * rhs[8];
        r[7] = lhs[1] * rhs[6] + lhs[4] * rhs[7] + lhs[7] * rhs[8];
        r[8] = lhs[2] * rhs[6] + lhs[5] * rhs[7] + lhs[8] * rhs[8];
        return r;
    }

    static float[] mul3x3Float3(float[] lhs, float[] rhs)
    {
        float r0 = rhs[0];
        float r1 = rhs[1];
        float r2 = rhs[2];
        rhs[0] = lhs[0] * r0 + lhs[3] * r1 + lhs[6] * r2;
        rhs[1] = lhs[1] * r0 + lhs[4] * r1 + lhs[7] * r2;
        rhs[2] = lhs[2] * r0 + lhs[5] * r1 + lhs[8] * r2;
        return rhs;
    }

    static float[] mul3x3Diag(float[] lhs, float[] rhs)
    {
        return new float[] {lhs[0] * rhs[0], lhs[1] * rhs[1], lhs[2] * rhs[2], lhs[0] * rhs[3], lhs[1] * rhs[4],
                            lhs[2] * rhs[5], lhs[0] * rhs[6], lhs[1] * rhs[7], lhs[2] * rhs[8]};
    }

    private static float[] adaptationMatrix(float[] matrix, float[] srcWhitePoint, float[] dstWhitePoint)
    {
        float[] sourceLms = mul3x3Float3(matrix, Arrays.copyOf(srcWhitePoint, 3));
        float[] destinationLms = mul3x3Float3(matrix, Arrays.copyOf(dstWhitePoint, 3));
        float[] diagonal = {destinationLms[0] / sourceLms[0], destinationLms[1] / sourceLms[1], destinationLms[2] / sourceLms[2]};
        return mul3x3(inverse3x3(matrix), mul3x3Diag(diagonal, matrix));
    }

    private static float area(float[] primaries)
    {
        float rx = primaries[0];
        float ry = primaries[1];
        float gx = primaries[2];
        float gy = primaries[3];
        float bx = primaries[4];
        float by = primaries[5];
        float determinant = rx * gy + ry * bx + gx * by - gy * bx - ry * gx - rx * by;
        float result = 0.5f * determinant;
        return result < 0.0f ? -result : result;
    }

    private static float cross(float ax, float ay, float bx, float by)
    {
        return ax * by - ay * bx;
    }

    private static boolean contains(float[] outer, float[] inner)
    {
        float[] r = {outer[0] - inner[0], outer[1] - inner[1], outer[2] - inner[2], outer[3] - inner[3], outer[4] - inner[4],
                     outer[5] - inner[5]};

        if (cross(r[0], r[1], inner[0] - inner[4], inner[1] - inner[5]) < 0) return false;
        if (cross(inner[0] - inner[2], inner[1] - inner[3], r[0], r[1]) < 0) return false;
        if (cross(r[2], r[3], inner[2] - inner[0], inner[3] - inner[1]) < 0) return false;
        if (cross(inner[2] - inner[4], inner[3] - inner[5], r[2], r[3]) < 0) return false;
        if (cross(r[4], r[5], inner[4] - inner[2], inner[5] - inner[3]) < 0) return false;
        if (cross(inner[4] - inner[0], inner[5] - inner[1], r[4], r[5]) < 0) return false;

        return true;
    }

    private static float[] computeXyzMatrix(float[] primaries, float[] whitePoint)
    {
        float rx = primaries[0];
        float ry = primaries[1];
        float gx = primaries[2];
        float gy = primaries[3];
        float bx = primaries[4];
        float by = primaries[5];
        float wx = whitePoint[0];
        float wy = whitePoint[1];

        float oneRxRy = (1 - rx) / ry;
        float oneGxGy = (1 - gx) / gy;
        float oneBxBy = (1 - bx) / by;
        float oneWxWy = (1 - wx) / wy;

        float rxRy = rx / ry;
        float gxGy = gx / gy;
        float bxBy = bx / by;
        float wxWy = wx / wy;

        float blueY = ((oneWxWy - oneRxRy) * (gxGy - rxRy) - (wxWy - rxRy) * (oneGxGy - oneRxRy))
                      / ((oneBxBy - oneRxRy) * (gxGy - rxRy) - (bxBy - rxRy) * (oneGxGy - oneRxRy));
        float greenY = (wxWy - rxRy - blueY * (bxBy - rxRy)) / (gxGy - rxRy);
        float redY = 1 - greenY - blueY;

        float redYRy = redY / ry;
        float greenYGy = greenY / gy;
        float blueYBy = blueY / by;

        return new float[] {redYRy * rx, redY, redYRy * (1 - rx - ry), greenYGy * gx, greenY, greenYGy * (1 - gx - gy),
                            blueYBy * bx, blueY, blueYBy * (1 - bx - by)};
    }

    private static float[] computePrimaries(float[] toXyz)
    {
        float[] r = mul3x3Float3(toXyz, new float[] {1.0f, 0.0f, 0.0f});
        float[] g = mul3x3Float3(toXyz, new float[] {0.0f, 1.0f, 0.0f});
        float[] b = mul3x3Float3(toXyz, new float[] {0.0f, 0.0f, 1.0f});

        float rSum = r[0] + r[1] + r[2];
        float gSum = g[0] + g[1] + g[2];
        float bSum = b[0] + b[1] + b[2];

        return new float[] {r[0] / rSum, r[1] / rSum, g[0] / gSum, g[1] / gSum, b[0] / bSum, b[1] / bSum};
    }

    private static float[] computeWhitePoint(float[] toXyz)
    {
        float[] w = mul3x3Float3(toXyz, new float[] {1.0f, 1.0f, 1.0f});
        float sum = w[0] + w[1] + w[2];
        return new float[] {w[0] / sum, w[1] / sum};
    }

    public enum Named
    {
        SRGB,
        LINEAR_SRGB,
        EXTENDED_SRGB,
        LINEAR_EXTENDED_SRGB,
        BT709,
        BT2020,
        DCI_P3,
        DISPLAY_P3,
        NTSC_1953,
        SMPTE_C,
        ADOBE_RGB,
        PRO_PHOTO_RGB,
        ACES,
        ACESCG,
        CIE_XYZ,
        CIE_LAB,
        BT2020_HLG,
        BT2020_PQ,
        OK_LAB
    }

    public enum RenderIntent
    {
        PERCEPTUAL,
        RELATIVE,
        SATURATION,
        ABSOLUTE
    }

    public enum Adaptation
    {
        BRADFORD(new float[] {0.8951f, -0.7502f, 0.0389f, 0.2664f, 1.7135f, -0.0685f, -0.1614f, 0.0367f, 1.0296f}),
        VON_KRIES(new float[] {0.40024f, -0.22630f, 0.00000f, 0.70760f, 1.16532f, 0.00000f, -0.08081f, 0.04570f, 0.91822f}),
        CIECAT02(new float[] {0.7328f, -0.7036f, 0.0030f, 0.4296f, 1.6975f, 0.0136f, -0.1624f, 0.0061f, 0.9834f});

        final float[] mTransform;

        Adaptation(float[] transform)
        {
            mTransform = transform;
        }
    }

    public enum Model
    {
        RGB(3),
        XYZ(3),
        LAB(3),
        CMYK(4);

        private final int mComponents;

        Model(int components)
        {
            mComponents = components;
        }

        public int getComponentCount()
        {
            return mComponents;
        }
    }

    private static final class Xyz extends ColorSpace
    {
        Xyz(String name, int id)
        {
            super(name, Model.XYZ, id);
        }

        @Override
        public boolean isWideGamut()
        {
            return true;
        }

        @Override
        public float getMinValue(int component)
        {
            return -2.0f;
        }

        @Override
        public float getMaxValue(int component)
        {
            return 2.0f;
        }

        @Override
        public float[] toXyz(float[] value)
        {
            value[0] = clamp(value[0]);
            value[1] = clamp(value[1]);
            value[2] = clamp(value[2]);
            return value;
        }

        @Override
        public float[] fromXyz(float[] value)
        {
            value[0] = clamp(value[0]);
            value[1] = clamp(value[1]);
            value[2] = clamp(value[2]);
            return value;
        }

        private static float clamp(float x)
        {
            return x < -2.0f ? -2.0f : x > 2.0f ? 2.0f : x;
        }
    }

    private static final class Lab extends ColorSpace
    {
        private static final float A = 216.0f / 24389.0f;
        private static final float B = 841.0f / 108.0f;
        private static final float C = 4.0f / 29.0f;
        private static final float D = 6.0f / 29.0f;

        Lab(String name, int id)
        {
            super(name, Model.LAB, id);
        }

        @Override
        public boolean isWideGamut()
        {
            return true;
        }

        @Override
        public float getMinValue(int component)
        {
            return component == 0 ? 0.0f : -128.0f;
        }

        @Override
        public float getMaxValue(int component)
        {
            return component == 0 ? 100.0f : 128.0f;
        }

        @Override
        public float[] toXyz(float[] value)
        {
            float lightness = clamp(value[0], 0.0f, 100.0f);
            float a = clamp(value[1], -128.0f, 128.0f);
            float b = clamp(value[2], -128.0f, 128.0f);

            float fy = (lightness + 16.0f) / 116.0f;
            float fx = fy + (a * 0.002f);
            float fz = fy - (b * 0.005f);
            float x = fx > D ? fx * fx * fx : (1.0f / B) * (fx - C);
            float y = fy > D ? fy * fy * fy : (1.0f / B) * (fy - C);
            float z = fz > D ? fz * fz * fz : (1.0f / B) * (fz - C);

            value[0] = x * ILLUMINANT_D50_XYZ[0];
            value[1] = y * ILLUMINANT_D50_XYZ[1];
            value[2] = z * ILLUMINANT_D50_XYZ[2];
            return value;
        }

        @Override
        public float[] fromXyz(float[] value)
        {
            float x = value[0] / ILLUMINANT_D50_XYZ[0];
            float y = value[1] / ILLUMINANT_D50_XYZ[1];
            float z = value[2] / ILLUMINANT_D50_XYZ[2];

            float fx = x > A ? (float)Math.pow(x, 1.0 / 3.0) : B * x + C;
            float fy = y > A ? (float)Math.pow(y, 1.0 / 3.0) : B * y + C;
            float fz = z > A ? (float)Math.pow(z, 1.0 / 3.0) : B * z + C;

            value[0] = clamp(116.0f * fy - 16.0f, 0.0f, 100.0f);
            value[1] = clamp(500.0f * (fx - fy), -128.0f, 128.0f);
            value[2] = clamp(200.0f * (fy - fz), -128.0f, 128.0f);
            return value;
        }

        private static float clamp(float x, float min, float max)
        {
            return x < min ? min : x > max ? max : x;
        }
    }

    private static final class OkLab extends ColorSpace
    {
        private static final float[] M1 = {0.8189330101f, 0.0329845436f, 0.0482003018f, 0.3618667424f, 0.9293118715f,
                                           0.2643662691f, -0.1288597137f, 0.0361456387f, 0.6338517070f};
        private static final float[] M2 = {0.2104542553f, 1.9779984951f, 0.0259040371f, 0.7936177850f, -2.4285922050f,
                                           0.7827717662f, -0.0040720468f, 0.4505937099f, -0.8086757660f};
        private static final float[] D50_TO_D65 =
            adaptationMatrix(Adaptation.BRADFORD.mTransform, ILLUMINANT_D50_XYZ, xyYToXyz(ILLUMINANT_D65));
        private static final float[] TO_LMS = mul3x3(M1, D50_TO_D65);
        private static final float[] FROM_LMS = inverse3x3(TO_LMS);
        private static final float[] M2_INVERSE = inverse3x3(M2);

        OkLab(String name, int id)
        {
            super(name, Model.LAB, id);
        }

        @Override
        public boolean isWideGamut()
        {
            return true;
        }

        @Override
        public float getMinValue(int component)
        {
            return component == 0 ? 0.0f : -0.5f;
        }

        @Override
        public float getMaxValue(int component)
        {
            return component == 0 ? 1.0f : 0.5f;
        }

        @Override
        public float[] toXyz(float[] value)
        {
            mul3x3Float3(M2_INVERSE, value);
            value[0] = value[0] * value[0] * value[0];
            value[1] = value[1] * value[1] * value[1];
            value[2] = value[2] * value[2] * value[2];
            return mul3x3Float3(FROM_LMS, value);
        }

        @Override
        public float[] fromXyz(float[] value)
        {
            mul3x3Float3(TO_LMS, value);
            value[0] = (float)Math.cbrt(value[0]);
            value[1] = (float)Math.cbrt(value[1]);
            value[2] = (float)Math.cbrt(value[2]);
            return mul3x3Float3(M2, value);
        }
    }

    public static class Rgb extends ColorSpace
    {
        private final float[] mWhitePoint;
        private final float[] mPrimaries;
        private final float mMin;
        private final float mMax;
        private final float[] mTransform;
        private final float[] mInverseTransform;
        private final DoubleUnaryOperator mOetf;
        private final DoubleUnaryOperator mEotf;
        private final DoubleUnaryOperator mClampedOetf;
        private final DoubleUnaryOperator mClampedEotf;
        private final boolean mIsWideGamut;
        private final boolean mIsSrgb;
        private final TransferParameters mTransferParameters;

        public static class TransferParameters
        {
            public final double a;
            public final double b;
            public final double c;
            public final double d;
            public final double e;
            public final double f;
            public final double g;

            public TransferParameters(double a, double b, double c, double d, double g)
            {
                this(a, b, c, d, 0.0, 0.0, g);
            }

            public TransferParameters(double a, double b, double c, double d, double e, double f, double g)
            {
                if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c) || Double.isNaN(d) || Double.isNaN(e) || Double.isNaN(f)
                    || Double.isNaN(g))
                {
                    throw new IllegalArgumentException("Parameters cannot be NaN");
                }

                if (g != TYPE_PQ && g != TYPE_HLG)
                {
                    if (!(d >= 0.0 && d <= 1.0f + Math.ulp(1.0f)))
                        throw new IllegalArgumentException("Parameter d must be in the range [0..1], was " + d);
                    if (d == 0.0 && (a == 0.0 || g == 0.0))
                        throw new IllegalArgumentException("Parameter a or g is zero, the transfer function is constant");
                    if (d >= 1.0 && c == 0.0) throw new IllegalArgumentException("Parameter c is zero, the transfer function is constant");
                    if ((a == 0.0 || g == 0.0) && c == 0.0)
                        throw new IllegalArgumentException("Parameter a or g is zero, and c is zero, the transfer function is constant");
                    if (c < 0.0) throw new IllegalArgumentException("The transfer function must be increasing");
                    if (a < 0.0 || g < 0.0) throw new IllegalArgumentException("The transfer function must be positive or increasing");
                }

                this.a = a;
                this.b = b;
                this.c = c;
                this.d = d;
                this.e = e;
                this.f = f;
                this.g = g;
            }

            double decode(double x)
            {
                if (g == TYPE_PQ) return perceptualQuantizerEotf(x);
                if (g == TYPE_HLG) return hybridLogGammaEotf(x);
                return x >= d ? Math.pow(a * x + b, g) + e : c * x + f;
            }

            double encode(double x)
            {
                if (g == TYPE_PQ) return perceptualQuantizerOetf(x);
                if (g == TYPE_HLG) return hybridLogGammaOetf(x);
                return x >= d * c ? (Math.pow(Math.max(x - e, 0.0), 1.0 / g) - b) / a : (x - f) / c;
            }

            @Override
            public boolean equals(Object other)
            {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;

                TransferParameters that = (TransferParameters)other;
                return Double.compare(that.a, a) == 0 && Double.compare(that.b, b) == 0 && Double.compare(that.c, c) == 0
                       && Double.compare(that.d, d) == 0 && Double.compare(that.e, e) == 0 && Double.compare(that.f, f) == 0
                       && Double.compare(that.g, g) == 0;
            }

            @Override
            public int hashCode()
            {
                return Arrays.hashCode(new double[] {a, b, c, d, e, f, g});
            }
        }

        public Rgb(String name, float[] toXYZ, DoubleUnaryOperator oetf, DoubleUnaryOperator eotf)
        {
            this(name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), null, oetf, eotf, 0.0f, 1.0f, null, MIN_ID);
        }

        public Rgb(String name, float[] primaries, float[] whitePoint, DoubleUnaryOperator oetf, DoubleUnaryOperator eotf, float min,
                   float max)
        {
            this(name, primaries, whitePoint, null, oetf, eotf, min, max, null, MIN_ID);
        }

        public Rgb(String name, float[] toXYZ, TransferParameters function)
        {
            this(name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), function, MIN_ID);
        }

        public Rgb(String name, float[] primaries, float[] whitePoint, TransferParameters function)
        {
            this(name, primaries, whitePoint, null, function, MIN_ID);
        }

        public Rgb(String name, float[] toXYZ, double gamma)
        {
            this(name, computePrimaries(toXYZ), computeWhitePoint(toXYZ), gamma, 0.0f, 1.0f, MIN_ID);
        }

        public Rgb(String name, float[] primaries, float[] whitePoint, double gamma)
        {
            this(name, primaries, whitePoint, gamma, 0.0f, 1.0f, MIN_ID);
        }

        private Rgb(String name, float[] primaries, float[] whitePoint, TransferParameters function, int id)
        {
            this(name, primaries, whitePoint, null, function, id);
        }

        private Rgb(String name, float[] primaries, float[] whitePoint, float[] transform, TransferParameters function, int id)
        {
            this(name, primaries, whitePoint, transform, function::encode, function::decode, 0.0f, 1.0f, function, id);
        }

        private Rgb(String name, float[] primaries, float[] whitePoint, double gamma, float min, float max, int id)
        {
            this(name, primaries, whitePoint, null,
                 gamma == 1.0 ? DoubleUnaryOperator.identity() : x -> Math.pow(x < 0.0 ? 0.0 : x, 1 / gamma),
                 gamma == 1.0 ? DoubleUnaryOperator.identity() : x -> Math.pow(x < 0.0 ? 0.0 : x, gamma), min, max,
                 new TransferParameters(1.0, 0.0, 0.0, 0.0, gamma), id);
        }

        private Rgb(String name, float[] primaries, float[] whitePoint, float[] transform, DoubleUnaryOperator oetf,
                    DoubleUnaryOperator eotf, float min, float max, TransferParameters transferParameters, int id)
        {
            super(name, Model.RGB, id);

            if (primaries == null || (primaries.length != 6 && primaries.length != 9))
            {
                throw new IllegalArgumentException("The color space's primaries must be defined as an array of 6 floats in xyY or 9 "
                                                   + "floats in XYZ");
            }

            if (whitePoint == null || (whitePoint.length != 2 && whitePoint.length != 3))
            {
                throw new IllegalArgumentException("The color space's white point must be defined as an array of 2 floats in xyY or 3 "
                                                   + "float in XYZ");
            }

            if (oetf == null || eotf == null) throw new IllegalArgumentException("The transfer functions of a color space cannot be null");
            if (min >= max)
                throw new IllegalArgumentException("Invalid range: min=" + min + ", max=" + max + "; min must be strictly < max");

            mWhitePoint = xyWhitePoint(whitePoint);
            mPrimaries = xyPrimaries(primaries);
            mTransform = transform == null ? computeXyzMatrix(mPrimaries, mWhitePoint) : transform;
            mInverseTransform = inverse3x3(mTransform);
            mOetf = oetf;
            mEotf = eotf;
            mMin = min;
            mMax = max;
            mTransferParameters = transferParameters;

            final float clampMin = min;
            final float clampMax = max;
            mClampedOetf = x -> clampRange(oetf.applyAsDouble(x), clampMin, clampMax);
            mClampedEotf = x -> eotf.applyAsDouble(clampRange(x, clampMin, clampMax));

            mIsWideGamut = (area(mPrimaries) / area(NTSC_1953_PRIMARIES) > 0.9f && contains(mPrimaries, SRGB_PRIMARIES))
                           || (min < 0.0f && max > 1.0f);
            mIsSrgb = id == 0 || (compare(mPrimaries, SRGB_PRIMARIES) && compare(mWhitePoint, ILLUMINANT_D65) && min == 0.0f
                                  && max == 1.0f && SRGB_TRANSFER_PARAMETERS.equals(transferParameters));
        }

        private Rgb(Rgb colorSpace, float[] transform, float[] whitePoint)
        {
            this(colorSpace.getName(), colorSpace.mPrimaries, whitePoint, transform, colorSpace.mOetf, colorSpace.mEotf,
                 colorSpace.mMin, colorSpace.mMax, colorSpace.mTransferParameters, MIN_ID);
        }

        private static double clampRange(double x, float min, float max)
        {
            return x < min ? min : x > max ? max : x;
        }

        public float[] getWhitePoint(float[] whitePoint)
        {
            whitePoint[0] = mWhitePoint[0];
            whitePoint[1] = mWhitePoint[1];
            return whitePoint;
        }

        public float[] getWhitePoint()
        {
            return Arrays.copyOf(mWhitePoint, mWhitePoint.length);
        }

        public float[] getPrimaries(float[] primaries)
        {
            System.arraycopy(mPrimaries, 0, primaries, 0, mPrimaries.length);
            return primaries;
        }

        public float[] getPrimaries()
        {
            return Arrays.copyOf(mPrimaries, mPrimaries.length);
        }

        public float[] getTransform(float[] transform)
        {
            System.arraycopy(mTransform, 0, transform, 0, mTransform.length);
            return transform;
        }

        public float[] getTransform()
        {
            return Arrays.copyOf(mTransform, mTransform.length);
        }

        public float[] getInverseTransform(float[] inverseTransform)
        {
            System.arraycopy(mInverseTransform, 0, inverseTransform, 0, mInverseTransform.length);
            return inverseTransform;
        }

        public float[] getInverseTransform()
        {
            return Arrays.copyOf(mInverseTransform, mInverseTransform.length);
        }

        public DoubleUnaryOperator getOetf()
        {
            return mClampedOetf;
        }

        public DoubleUnaryOperator getEotf()
        {
            return mClampedEotf;
        }

        public TransferParameters getTransferParameters()
        {
            return mTransferParameters;
        }

        @Override
        public boolean isSrgb()
        {
            return mIsSrgb;
        }

        @Override
        public boolean isWideGamut()
        {
            return mIsWideGamut;
        }

        @Override
        public float getMinValue(int component)
        {
            return mMin;
        }

        @Override
        public float getMaxValue(int component)
        {
            return mMax;
        }

        public float[] toLinear(float r, float g, float b)
        {
            return toLinear(new float[] {r, g, b});
        }

        public float[] toLinear(float[] value)
        {
            value[0] = (float)mClampedEotf.applyAsDouble(value[0]);
            value[1] = (float)mClampedEotf.applyAsDouble(value[1]);
            value[2] = (float)mClampedEotf.applyAsDouble(value[2]);
            return value;
        }

        public float[] fromLinear(float r, float g, float b)
        {
            return fromLinear(new float[] {r, g, b});
        }

        public float[] fromLinear(float[] value)
        {
            value[0] = (float)mClampedOetf.applyAsDouble(value[0]);
            value[1] = (float)mClampedOetf.applyAsDouble(value[1]);
            value[2] = (float)mClampedOetf.applyAsDouble(value[2]);
            return value;
        }

        @Override
        public float[] toXyz(float[] value)
        {
            value[0] = (float)mClampedEotf.applyAsDouble(value[0]);
            value[1] = (float)mClampedEotf.applyAsDouble(value[1]);
            value[2] = (float)mClampedEotf.applyAsDouble(value[2]);
            return mul3x3Float3(mTransform, value);
        }

        @Override
        public float[] fromXyz(float[] value)
        {
            mul3x3Float3(mInverseTransform, value);
            value[0] = (float)mClampedOetf.applyAsDouble(value[0]);
            value[1] = (float)mClampedOetf.applyAsDouble(value[1]);
            value[2] = (float)mClampedOetf.applyAsDouble(value[2]);
            return value;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (!super.equals(other)) return false;

            Rgb rgb = (Rgb)other;
            if (Float.compare(rgb.mMin, mMin) != 0 || Float.compare(rgb.mMax, mMax) != 0) return false;
            if (!Arrays.equals(mWhitePoint, rgb.mWhitePoint) || !Arrays.equals(mPrimaries, rgb.mPrimaries)) return false;
            if (mTransferParameters != null) return mTransferParameters.equals(rgb.mTransferParameters);
            if (rgb.mTransferParameters != null) return false;

            return mOetf.equals(rgb.mOetf) && mEotf.equals(rgb.mEotf);
        }

        @Override
        public int hashCode()
        {
            int result = super.hashCode();
            result = 31 * result + Arrays.hashCode(mWhitePoint);
            result = 31 * result + Arrays.hashCode(mPrimaries);
            result = 31 * result + Float.floatToIntBits(mMin);
            result = 31 * result + Float.floatToIntBits(mMax);
            result = 31 * result + (mTransferParameters != null ? mTransferParameters.hashCode() : 0);
            return result;
        }
    }

    public static class Connector
    {
        private final ColorSpace mSource;
        private final ColorSpace mDestination;
        private final ColorSpace mTransformSource;
        private final ColorSpace mTransformDestination;
        private final RenderIntent mIntent;
        private final float[] mTransform;

        Connector(ColorSpace source, ColorSpace destination, RenderIntent intent)
        {
            this(source, destination, source.getModel() == Model.RGB ? adapt(source, ILLUMINANT_D50_XYZ) : source,
                 destination.getModel() == Model.RGB ? adapt(destination, ILLUMINANT_D50_XYZ) : destination, intent,
                 computeTransform(source, destination, intent));
        }

        private Connector(ColorSpace source, ColorSpace destination, ColorSpace transformSource, ColorSpace transformDestination,
                          RenderIntent intent, float[] transform)
        {
            mSource = source;
            mDestination = destination;
            mTransformSource = transformSource;
            mTransformDestination = transformDestination;
            mIntent = intent;
            mTransform = transform;
        }

        private static float[] computeTransform(ColorSpace source, ColorSpace destination, RenderIntent intent)
        {
            if (intent != RenderIntent.ABSOLUTE) return null;

            boolean sourceRgb = source.getModel() == Model.RGB;
            boolean destinationRgb = destination.getModel() == Model.RGB;
            if (sourceRgb && destinationRgb) return null;
            if (!sourceRgb && !destinationRgb) return null;

            Rgb rgb = (Rgb)(sourceRgb ? source : destination);
            float[] sourceXyz = sourceRgb ? xyYToXyz(rgb.mWhitePoint) : ILLUMINANT_D50_XYZ;
            float[] destinationXyz = destinationRgb ? xyYToXyz(rgb.mWhitePoint) : ILLUMINANT_D50_XYZ;

            return new float[] {sourceXyz[0] / destinationXyz[0], sourceXyz[1] / destinationXyz[1], sourceXyz[2] / destinationXyz[2]};
        }

        static Connector identity(ColorSpace source)
        {
            return new Connector(source, source, source, source, RenderIntent.RELATIVE, null)
            {
                @Override
                public float[] transform(float[] value)
                {
                    return value;
                }
            };
        }

        public ColorSpace getSource()
        {
            return mSource;
        }

        public ColorSpace getDestination()
        {
            return mDestination;
        }

        public RenderIntent getRenderIntent()
        {
            return mIntent;
        }

        public float[] transform(float r, float g, float b)
        {
            return transform(new float[] {r, g, b});
        }

        public float[] transform(float[] value)
        {
            float[] xyz = mTransformSource.toXyz(value);

            if (mTransform != null)
            {
                xyz[0] *= mTransform[0];
                xyz[1] *= mTransform[1];
                xyz[2] *= mTransform[2];
            }

            return mTransformDestination.fromXyz(xyz);
        }
    }
}
