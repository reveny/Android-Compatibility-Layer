package android.renderscript;

import java.util.BitSet;

public class FieldPacker
{
    private final byte[] mData;
    private final int mLen;
    private final BitSet mAlignment;

    private int mPos;

    public FieldPacker(int len)
    {
        mPos = 0;
        mLen = len;
        mData = new byte[len];
        mAlignment = new BitSet();
    }

    public FieldPacker(byte[] data)
    {
        mPos = data.length;
        mLen = data.length;
        mData = data;
        mAlignment = new BitSet();
    }

    public void align(int v)
    {
        if (v <= 0 || (v & (v - 1)) != 0)
        {
            throw new RSIllegalArgumentException("argument must be a non-negative non-zero power of 2: " + v);
        }

        while ((mPos & (v - 1)) != 0)
        {
            mAlignment.flip(mPos);
            mData[mPos++] = 0;
        }
    }

    public void subalign(int v)
    {
        if ((v & (v - 1)) != 0)
        {
            throw new RSIllegalArgumentException("argument must be a non-negative non-zero power of 2: " + v);
        }

        while ((mPos & (v - 1)) != 0) mPos--;

        if (mPos > 0)
        {
            while (mAlignment.get(mPos - 1))
            {
                mPos--;
                mAlignment.flip(mPos);
            }
        }
    }

    public void reset()
    {
        mPos = 0;
    }

    public void reset(int i)
    {
        if (i < 0 || i > mLen) throw new RSIllegalArgumentException("out of range argument: " + i);
        mPos = i;
    }

    public void skip(int i)
    {
        int position = mPos + i;
        if (position < 0 || position > mLen) throw new RSIllegalArgumentException("out of range argument: " + i);
        mPos = position;
    }

    public void addI8(byte v)
    {
        mData[mPos++] = v;
    }

    public byte subI8()
    {
        subalign(1);
        return mData[--mPos];
    }

    public void addI16(short v)
    {
        align(2);
        mData[mPos++] = (byte) (v & 0xFF);
        mData[mPos++] = (byte) (v >> 8);
    }

    public short subI16()
    {
        subalign(2);
        int value = (mData[--mPos] & 0xFF) << 8;
        value |= mData[--mPos] & 0xFF;
        return (short) value;
    }

    public void addI32(int v)
    {
        align(4);
        mData[mPos++] = (byte) (v & 0xFF);
        mData[mPos++] = (byte) ((v >> 8) & 0xFF);
        mData[mPos++] = (byte) ((v >> 16) & 0xFF);
        mData[mPos++] = (byte) ((v >> 24) & 0xFF);
    }

    public int subI32()
    {
        subalign(4);
        int value = (mData[--mPos] & 0xFF) << 24;
        value |= (mData[--mPos] & 0xFF) << 16;
        value |= (mData[--mPos] & 0xFF) << 8;
        value |= mData[--mPos] & 0xFF;
        return value;
    }

    public void addI64(long v)
    {
        align(8);
        for (int shift = 0; shift < 64; shift += 8) mData[mPos++] = (byte) ((v >> shift) & 0xFF);
    }

    public long subI64()
    {
        subalign(8);
        long value = 0;
        for (int index = 0; index < 8; index++) value = (value << 8) | (mData[--mPos] & 0xFFL);
        return value;
    }

    public void addU8(short v)
    {
        if (v < 0 || v > 0xFF) throw new IllegalArgumentException("Saving value out of range for type");
        mData[mPos++] = (byte) v;
    }

    public void addU16(int v)
    {
        if (v < 0 || v > 0xFFFF) throw new IllegalArgumentException("Saving value out of range for type");

        align(2);
        mData[mPos++] = (byte) (v & 0xFF);
        mData[mPos++] = (byte) (v >> 8);
    }

    public void addU32(long v)
    {
        if (v < 0 || v > 0xFFFFFFFFL) throw new IllegalArgumentException("Saving value out of range for type");

        align(4);
        mData[mPos++] = (byte) (v & 0xFF);
        mData[mPos++] = (byte) ((v >> 8) & 0xFF);
        mData[mPos++] = (byte) ((v >> 16) & 0xFF);
        mData[mPos++] = (byte) ((v >> 24) & 0xFF);
    }

    public void addU64(long v)
    {
        if (v < 0) throw new IllegalArgumentException("Saving value out of range for type");
        addI64(v);
    }

    public void addF32(float v)
    {
        addI32(Float.floatToRawIntBits(v));
    }

    public float subF32()
    {
        return Float.intBitsToFloat(subI32());
    }

    public void addF64(double v)
    {
        addI64(Double.doubleToRawLongBits(v));
    }

    public double subF64()
    {
        return Double.longBitsToDouble(subI64());
    }

    public void addObj(BaseObj obj)
    {
        addI64(obj == null ? 0 : obj.getID());
        addI64(0);
        addI64(0);
        addI64(0);
    }

    public void addF32(Float2 v)
    {
        addF32(v.x);
        addF32(v.y);
    }

    public void addF32(Float3 v)
    {
        addF32(v.x);
        addF32(v.y);
        addF32(v.z);
    }

    public void addF32(Float4 v)
    {
        addF32(v.x);
        addF32(v.y);
        addF32(v.z);
        addF32(v.w);
    }

    public void addF64(Double2 v)
    {
        addF64(v.x);
        addF64(v.y);
    }

    public void addF64(Double3 v)
    {
        addF64(v.x);
        addF64(v.y);
        addF64(v.z);
    }

    public void addF64(Double4 v)
    {
        addF64(v.x);
        addF64(v.y);
        addF64(v.z);
        addF64(v.w);
    }

    public void addI8(Byte2 v)
    {
        addI8(v.x);
        addI8(v.y);
    }

    public void addI8(Byte3 v)
    {
        addI8(v.x);
        addI8(v.y);
        addI8(v.z);
    }

    public void addI8(Byte4 v)
    {
        addI8(v.x);
        addI8(v.y);
        addI8(v.z);
        addI8(v.w);
    }

    public void addU8(Short2 v)
    {
        addU8(v.x);
        addU8(v.y);
    }

    public void addU8(Short3 v)
    {
        addU8(v.x);
        addU8(v.y);
        addU8(v.z);
    }

    public void addU8(Short4 v)
    {
        addU8(v.x);
        addU8(v.y);
        addU8(v.z);
        addU8(v.w);
    }

    public void addI16(Short2 v)
    {
        addI16(v.x);
        addI16(v.y);
    }

    public void addI16(Short3 v)
    {
        addI16(v.x);
        addI16(v.y);
        addI16(v.z);
    }

    public void addI16(Short4 v)
    {
        addI16(v.x);
        addI16(v.y);
        addI16(v.z);
        addI16(v.w);
    }

    public void addU16(Int2 v)
    {
        addU16(v.x);
        addU16(v.y);
    }

    public void addU16(Int3 v)
    {
        addU16(v.x);
        addU16(v.y);
        addU16(v.z);
    }

    public void addU16(Int4 v)
    {
        addU16(v.x);
        addU16(v.y);
        addU16(v.z);
        addU16(v.w);
    }

    public void addI32(Int2 v)
    {
        addI32(v.x);
        addI32(v.y);
    }

    public void addI32(Int3 v)
    {
        addI32(v.x);
        addI32(v.y);
        addI32(v.z);
    }

    public void addI32(Int4 v)
    {
        addI32(v.x);
        addI32(v.y);
        addI32(v.z);
        addI32(v.w);
    }

    public void addU32(Long2 v)
    {
        addU32(v.x);
        addU32(v.y);
    }

    public void addU32(Long3 v)
    {
        addU32(v.x);
        addU32(v.y);
        addU32(v.z);
    }

    public void addU32(Long4 v)
    {
        addU32(v.x);
        addU32(v.y);
        addU32(v.z);
        addU32(v.w);
    }

    public void addI64(Long2 v)
    {
        addI64(v.x);
        addI64(v.y);
    }

    public void addI64(Long3 v)
    {
        addI64(v.x);
        addI64(v.y);
        addI64(v.z);
    }

    public void addI64(Long4 v)
    {
        addI64(v.x);
        addI64(v.y);
        addI64(v.z);
        addI64(v.w);
    }

    public void addU64(Long2 v)
    {
        addU64(v.x);
        addU64(v.y);
    }

    public void addU64(Long3 v)
    {
        addU64(v.x);
        addU64(v.y);
        addU64(v.z);
    }

    public void addU64(Long4 v)
    {
        addU64(v.x);
        addU64(v.y);
        addU64(v.z);
        addU64(v.w);
    }

    public Float2 subFloat2()
    {
        Float2 value = new Float2();
        value.y = subF32();
        value.x = subF32();
        return value;
    }

    public Float3 subFloat3()
    {
        Float3 value = new Float3();
        value.z = subF32();
        value.y = subF32();
        value.x = subF32();
        return value;
    }

    public Float4 subFloat4()
    {
        Float4 value = new Float4();
        value.w = subF32();
        value.z = subF32();
        value.y = subF32();
        value.x = subF32();
        return value;
    }

    public Double2 subDouble2()
    {
        Double2 value = new Double2();
        value.y = subF64();
        value.x = subF64();
        return value;
    }

    public Double3 subDouble3()
    {
        Double3 value = new Double3();
        value.z = subF64();
        value.y = subF64();
        value.x = subF64();
        return value;
    }

    public Double4 subDouble4()
    {
        Double4 value = new Double4();
        value.w = subF64();
        value.z = subF64();
        value.y = subF64();
        value.x = subF64();
        return value;
    }

    public Byte2 subByte2()
    {
        Byte2 value = new Byte2();
        value.y = subI8();
        value.x = subI8();
        return value;
    }

    public Byte3 subByte3()
    {
        Byte3 value = new Byte3();
        value.z = subI8();
        value.y = subI8();
        value.x = subI8();
        return value;
    }

    public Byte4 subByte4()
    {
        Byte4 value = new Byte4();
        value.w = subI8();
        value.z = subI8();
        value.y = subI8();
        value.x = subI8();
        return value;
    }

    public Short2 subShort2()
    {
        Short2 value = new Short2();
        value.y = subI16();
        value.x = subI16();
        return value;
    }

    public Short3 subShort3()
    {
        Short3 value = new Short3();
        value.z = subI16();
        value.y = subI16();
        value.x = subI16();
        return value;
    }

    public Short4 subShort4()
    {
        Short4 value = new Short4();
        value.w = subI16();
        value.z = subI16();
        value.y = subI16();
        value.x = subI16();
        return value;
    }

    public Int2 subInt2()
    {
        Int2 value = new Int2();
        value.y = subI32();
        value.x = subI32();
        return value;
    }

    public Int3 subInt3()
    {
        Int3 value = new Int3();
        value.z = subI32();
        value.y = subI32();
        value.x = subI32();
        return value;
    }

    public Int4 subInt4()
    {
        Int4 value = new Int4();
        value.w = subI32();
        value.z = subI32();
        value.y = subI32();
        value.x = subI32();
        return value;
    }

    public Long2 subLong2()
    {
        Long2 value = new Long2();
        value.y = subI64();
        value.x = subI64();
        return value;
    }

    public Long3 subLong3()
    {
        Long3 value = new Long3();
        value.z = subI64();
        value.y = subI64();
        value.x = subI64();
        return value;
    }

    public Long4 subLong4()
    {
        Long4 value = new Long4();
        value.w = subI64();
        value.z = subI64();
        value.y = subI64();
        value.x = subI64();
        return value;
    }

    public void addMatrix(Matrix4f mat)
    {
        for (float value : mat.mMat) addF32(value);
    }

    public Matrix4f subMatrix4f()
    {
        Matrix4f value = new Matrix4f();
        for (int index = value.mMat.length - 1; index >= 0; index--) value.mMat[index] = subF32();
        return value;
    }

    public void addMatrix(Matrix3f mat)
    {
        for (float value : mat.mMat) addF32(value);
    }

    public Matrix3f subMatrix3f()
    {
        Matrix3f value = new Matrix3f();
        for (int index = value.mMat.length - 1; index >= 0; index--) value.mMat[index] = subF32();
        return value;
    }

    public void addMatrix(Matrix2f mat)
    {
        for (float value : mat.mMat) addF32(value);
    }

    public Matrix2f subMatrix2f()
    {
        Matrix2f value = new Matrix2f();
        for (int index = value.mMat.length - 1; index >= 0; index--) value.mMat[index] = subF32();
        return value;
    }

    public void addBoolean(boolean v)
    {
        addI8((byte) (v ? 1 : 0));
    }

    public boolean subBoolean()
    {
        return subI8() == 1;
    }

    public final byte[] getData()
    {
        return mData;
    }

    int getPos()
    {
        return mPos;
    }
}
