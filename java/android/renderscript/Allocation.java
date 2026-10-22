package android.renderscript;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Half;
import android.view.Surface;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Allocation extends BaseObj
{
    public static final int USAGE_SCRIPT = 0x0001;
    public static final int USAGE_GRAPHICS_TEXTURE = 0x0002;
    public static final int USAGE_GRAPHICS_VERTEX = 0x0004;
    public static final int USAGE_GRAPHICS_CONSTANTS = 0x0008;
    public static final int USAGE_GRAPHICS_RENDER_TARGET = 0x0010;
    public static final int USAGE_IO_INPUT = 0x0020;
    public static final int USAGE_IO_OUTPUT = 0x0040;
    public static final int USAGE_SHARED = 0x0080;

    private static final int USAGE_ALL = USAGE_SCRIPT | USAGE_GRAPHICS_TEXTURE | USAGE_GRAPHICS_VERTEX | USAGE_GRAPHICS_CONSTANTS
            | USAGE_GRAPHICS_RENDER_TARGET | USAGE_IO_INPUT | USAGE_IO_OUTPUT | USAGE_SHARED;

    private Type mType;
    private ByteBuffer mData;
    private final int mUsage;
    private final MipmapControl mMipmapControl;
    private boolean mAutoPadding;
    private long mTimeStamp = -1;
    private Surface mSurface;
    private OnBufferAvailableListener mBufferNotifier;

    public enum MipmapControl
    {
        MIPMAP_NONE(0),
        MIPMAP_FULL(1),
        MIPMAP_ON_SYNC_TO_TEXTURE(2);

        final int mID;

        MipmapControl(int id)
        {
            mID = id;
        }
    }

    public interface OnBufferAvailableListener
    {
        void onBufferAvailable(Allocation a);
    }

    Allocation(RenderScript rs, Type type, MipmapControl mipmapControl, int usage)
    {
        super(rs);

        if ((usage & ~USAGE_ALL) != 0) throw new RSIllegalArgumentException("Unknown usage specified.");
        if ((usage & USAGE_IO_INPUT) != 0 && (usage & ~(USAGE_IO_INPUT | USAGE_GRAPHICS_TEXTURE | USAGE_SCRIPT)) != 0)
        {
            throw new RSIllegalArgumentException("Invalid usage combination.");
        }

        mType = type;
        mUsage = usage;
        mMipmapControl = mipmapControl;
        mData = ByteBuffer.allocateDirect(computeBytesSize(type)).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static int computeBytesSize(Type type)
    {
        int size = type.getCount() * type.getElement().getBytesSize();
        if (type.getYuv() != 0) return (int) Math.ceil(size * 1.5);
        return size;
    }

    ByteBuffer data()
    {
        return mData;
    }

    int elementSize()
    {
        return mType.getElement().getBytesSize();
    }

    public Element getElement()
    {
        return mType.getElement();
    }

    public int getUsage()
    {
        return mUsage;
    }

    public void setAutoPadding(boolean useAutoPadding)
    {
        mAutoPadding = useAutoPadding;
    }

    public int getBytesSize()
    {
        return computeBytesSize(mType);
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public Type getType()
    {
        return mType;
    }

    public void syncAll(int srcLocation)
    {
        switch (srcLocation)
        {
            case USAGE_GRAPHICS_TEXTURE:
            case USAGE_SCRIPT:
            case USAGE_GRAPHICS_CONSTANTS:
            case USAGE_GRAPHICS_VERTEX:
            case USAGE_SHARED:
                break;
            default:
                throw new RSIllegalArgumentException("Source must be exactly one usage type.");
        }
        checkValid();
    }

    public void ioSend()
    {
        if ((mUsage & USAGE_IO_OUTPUT) == 0)
        {
            throw new RSIllegalArgumentException("Can only send buffer if IO_OUTPUT usage specified.");
        }
        checkValid();
    }

    public void ioReceive()
    {
        if ((mUsage & USAGE_IO_INPUT) == 0)
        {
            throw new RSIllegalArgumentException("Can only receive if IO_INPUT usage specified.");
        }
        checkValid();
        mTimeStamp = System.nanoTime();
    }

    public void copyFrom(BaseObj[] d)
    {
        checkValid();
        Element.DataType dataType = mType.getElement().mType;
        if (dataType.mID < Element.DataType.RS_ELEMENT.mID)
        {
            throw new RSIllegalArgumentException("copyFrom BaseObj[] requires an object element type.");
        }
        if (d.length != mType.getCount())
        {
            throw new RSIllegalArgumentException("Array size mismatch, allocation sizeX = " + mType.getCount() + ", array length = "
                    + d.length);
        }

        int size = elementSize();
        for (int index = 0; index < d.length; index++)
        {
            int offset = index * size;
            for (int byteIndex = 0; byteIndex < size; byteIndex++) mData.put(offset + byteIndex, (byte) 0);
            mData.putLong(offset, d[index] == null ? 0 : d[index].getID());
        }
    }

    public void copyFromUnchecked(Object array)
    {
        copyWholeFrom(array, false);
    }

    public void copyFromUnchecked(int[] d)
    {
        copyWholeFrom(d, false);
    }

    public void copyFromUnchecked(short[] d)
    {
        copyWholeFrom(d, false);
    }

    public void copyFromUnchecked(byte[] d)
    {
        copyWholeFrom(d, false);
    }

    public void copyFromUnchecked(float[] d)
    {
        copyWholeFrom(d, false);
    }

    public void copyFrom(Object array)
    {
        copyWholeFrom(array, true);
    }

    public void copyFrom(int[] d)
    {
        copyWholeFrom(d, true);
    }

    public void copyFrom(short[] d)
    {
        copyWholeFrom(d, true);
    }

    public void copyFrom(byte[] d)
    {
        copyWholeFrom(d, true);
    }

    public void copyFrom(float[] d)
    {
        copyWholeFrom(d, true);
    }

    public void copyFrom(Bitmap b)
    {
        checkValid();
        if (b.getConfig() == null)
        {
            Bitmap converted = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
            copyFrom(converted);
            return;
        }

        validateBitmapSize(b);
        byte[] bytes = RuntimeBitmapBridge.read(b, 0, 0, b.getWidth(), b.getHeight(), elementSize());
        writeBytes(0, bytes, 0, bytes.length);
    }

    public void copyFrom(Allocation a)
    {
        checkValid();
        if (!a.getType().getElement().equals(mType.getElement())
                && !a.getType().getElement().isCompatible(mType.getElement()))
        {
            throw new RSIllegalArgumentException("Types of allocations must match.");
        }
        if (a.getType().getX() != mType.getX() || a.getType().getY() != mType.getY() || a.getType().getZ() != mType.getZ())
        {
            throw new RSIllegalArgumentException("Types of allocations must match.");
        }

        int length = Math.min(a.getBytesSize(), getBytesSize());
        copyBuffer(a.mData, 0, mData, 0, length);
    }

    public void setFromFieldPacker(int xoff, FieldPacker fp)
    {
        checkValid();
        int size = elementSize();
        byte[] data = fp.getData();
        int dataSize = fp.getPos();

        int count = dataSize / size;
        if (size * count != dataSize)
        {
            throw new RSIllegalArgumentException("Field packer length " + dataSize + " not divisible by element size " + size + ".");
        }
        copy1DRangeFromUnchecked(xoff, count, data);
    }

    public void setFromFieldPacker(int xoff, int componentNumber, FieldPacker fp)
    {
        setFromFieldPacker(xoff, 0, 0, componentNumber, fp);
    }

    public void setFromFieldPacker(int xoff, int yoff, int zoff, int componentNumber, FieldPacker fp)
    {
        checkValid();
        Element element = mType.getElement();
        if (componentNumber >= element.getSubElementCount())
        {
            throw new RSIllegalArgumentException("Component_number " + componentNumber + " out of range.");
        }
        if (xoff < 0) throw new RSIllegalArgumentException("Offset x must be >= 0.");
        if (yoff < 0) throw new RSIllegalArgumentException("Offset y must be >= 0.");
        if (zoff < 0) throw new RSIllegalArgumentException("Offset z must be >= 0.");

        byte[] data = fp.getData();
        int dataSize = fp.getPos();
        int componentSize = element.getSubElement(componentNumber).getBytesSize() * element.getSubElementArraySize(componentNumber);
        if (dataSize != componentSize)
        {
            throw new RSIllegalArgumentException("Field packer sizelength " + dataSize + " does not match component size "
                    + componentSize + ".");
        }

        int index = (zoff * Math.max(mType.getY(), 1) + yoff) * Math.max(mType.getX(), 1) + xoff;
        int offset = index * element.getBytesSize() + element.getSubElementOffsetBytes(componentNumber);
        writeBytes(offset, data, 0, dataSize);
    }

    public void generateMipmaps()
    {
        checkValid();
    }

    public void copy1DRangeFromUnchecked(int off, int count, Object array)
    {
        copy1DIn(off, count, array, false);
    }

    public void copy1DRangeFromUnchecked(int off, int count, int[] d)
    {
        copy1DIn(off, count, d, false);
    }

    public void copy1DRangeFromUnchecked(int off, int count, short[] d)
    {
        copy1DIn(off, count, d, false);
    }

    public void copy1DRangeFromUnchecked(int off, int count, byte[] d)
    {
        copy1DIn(off, count, d, false);
    }

    public void copy1DRangeFromUnchecked(int off, int count, float[] d)
    {
        copy1DIn(off, count, d, false);
    }

    public void copy1DRangeFrom(int off, int count, Object array)
    {
        copy1DIn(off, count, array, true);
    }

    public void copy1DRangeFrom(int off, int count, int[] d)
    {
        copy1DIn(off, count, d, true);
    }

    public void copy1DRangeFrom(int off, int count, short[] d)
    {
        copy1DIn(off, count, d, true);
    }

    public void copy1DRangeFrom(int off, int count, byte[] d)
    {
        copy1DIn(off, count, d, true);
    }

    public void copy1DRangeFrom(int off, int count, float[] d)
    {
        copy1DIn(off, count, d, true);
    }

    public void copy1DRangeFrom(int off, int count, Allocation data, int dataOff)
    {
        checkValid();
        validate1DRange(off, count);
        data.validate1DRange(dataOff, count);

        int size = elementSize();
        copyBuffer(data.mData, dataOff * size, mData, off * size, count * size);
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, Object array)
    {
        copy2DIn(xoff, yoff, w, h, array, true);
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, byte[] data)
    {
        copy2DIn(xoff, yoff, w, h, data, true);
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, short[] data)
    {
        copy2DIn(xoff, yoff, w, h, data, true);
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, int[] data)
    {
        copy2DIn(xoff, yoff, w, h, data, true);
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, float[] data)
    {
        copy2DIn(xoff, yoff, w, h, data, true);
    }

    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, Allocation data, int dataXoff, int dataYoff)
    {
        checkValid();
        validate2DRange(xoff, yoff, w, h);
        data.validate2DRange(dataXoff, dataYoff, w, h);

        int size = elementSize();
        int width = Math.max(mType.getX(), 1);
        int sourceWidth = Math.max(data.mType.getX(), 1);
        for (int row = 0; row < h; row++)
        {
            int source = ((dataYoff + row) * sourceWidth + dataXoff) * size;
            int target = ((yoff + row) * width + xoff) * size;
            copyBuffer(data.mData, source, mData, target, w * size);
        }
    }

    public void copy2DRangeFrom(int xoff, int yoff, Bitmap data)
    {
        checkValid();
        int w = data.getWidth();
        int h = data.getHeight();
        validate2DRange(xoff, yoff, w, h);

        byte[] bytes = RuntimeBitmapBridge.read(data, 0, 0, w, h, elementSize());
        copy2DIn(xoff, yoff, w, h, bytes, false);
    }

    public void copy3DRangeFrom(int xoff, int yoff, int zoff, int w, int h, int d, Object array)
    {
        checkValid();
        validate3DRange(xoff, yoff, zoff, w, h, d);
        validateArrayType(array, true);

        int size = elementSize();
        int primitiveSize = primitiveSize(array);
        int width = Math.max(mType.getX(), 1);
        int height = Math.max(mType.getY(), 1);
        checkArrayLength(array, w * h * d * size);
        for (int slice = 0; slice < d; slice++)
        {
            for (int row = 0; row < h; row++)
            {
                int target = (((zoff + slice) * height + yoff + row) * width + xoff) * size;
                int source = ((slice * h + row) * w * size) / primitiveSize;
                writeArray(target, array, source, w * size);
            }
        }
    }

    public void copy3DRangeFrom(int xoff, int yoff, int zoff, int w, int h, int d, Allocation data, int dataXoff, int dataYoff,
            int dataZoff)
    {
        checkValid();
        validate3DRange(xoff, yoff, zoff, w, h, d);
        data.validate3DRange(dataXoff, dataYoff, dataZoff, w, h, d);

        int size = elementSize();
        int width = Math.max(mType.getX(), 1);
        int height = Math.max(mType.getY(), 1);
        int sourceWidth = Math.max(data.mType.getX(), 1);
        int sourceHeight = Math.max(data.mType.getY(), 1);
        for (int slice = 0; slice < d; slice++)
        {
            for (int row = 0; row < h; row++)
            {
                int source = (((dataZoff + slice) * sourceHeight + dataYoff + row) * sourceWidth + dataXoff) * size;
                int target = (((zoff + slice) * height + yoff + row) * width + xoff) * size;
                copyBuffer(data.mData, source, mData, target, w * size);
            }
        }
    }

    public void copyTo(Bitmap b)
    {
        checkValid();
        validateBitmapSize(b);

        int length = b.getWidth() * b.getHeight() * elementSize();
        byte[] bytes = new byte[length];
        readBytes(0, bytes, 0, length);
        RuntimeBitmapBridge.write(b, 0, 0, b.getWidth(), b.getHeight(), elementSize(), bytes);
    }

    public void copyTo(Object array)
    {
        copyWholeTo(array, true);
    }

    public void copyTo(byte[] d)
    {
        copyWholeTo(d, true);
    }

    public void copyTo(short[] d)
    {
        copyWholeTo(d, true);
    }

    public void copyTo(int[] d)
    {
        copyWholeTo(d, true);
    }

    public void copyTo(float[] d)
    {
        copyWholeTo(d, true);
    }

    public synchronized void resize(int dimX)
    {
        if (mType.getY() > 0 || mType.getZ() > 0 || mType.hasFaces() || mType.hasMipmaps())
        {
            throw new RSInvalidStateException("Resize only support for 1D allocations at this time.");
        }

        Type type = new Type(mRS, mType.getElement(), dimX, 0, 0, false, false, 0);
        ByteBuffer data = ByteBuffer.allocateDirect(computeBytesSize(type)).order(ByteOrder.LITTLE_ENDIAN);
        copyBuffer(mData, 0, data, 0, Math.min(data.capacity(), mData.capacity()));
        mType = type;
        mData = data;
    }

    public void copy1DRangeToUnchecked(int off, int count, Object array)
    {
        copy1DOut(off, count, array, false);
    }

    public void copy1DRangeToUnchecked(int off, int count, int[] d)
    {
        copy1DOut(off, count, d, false);
    }

    public void copy1DRangeToUnchecked(int off, int count, short[] d)
    {
        copy1DOut(off, count, d, false);
    }

    public void copy1DRangeToUnchecked(int off, int count, byte[] d)
    {
        copy1DOut(off, count, d, false);
    }

    public void copy1DRangeToUnchecked(int off, int count, float[] d)
    {
        copy1DOut(off, count, d, false);
    }

    public void copy1DRangeTo(int off, int count, Object array)
    {
        copy1DOut(off, count, array, true);
    }

    public void copy1DRangeTo(int off, int count, int[] d)
    {
        copy1DOut(off, count, d, true);
    }

    public void copy1DRangeTo(int off, int count, short[] d)
    {
        copy1DOut(off, count, d, true);
    }

    public void copy1DRangeTo(int off, int count, byte[] d)
    {
        copy1DOut(off, count, d, true);
    }

    public void copy1DRangeTo(int off, int count, float[] d)
    {
        copy1DOut(off, count, d, true);
    }

    public void copy2DRangeTo(int xoff, int yoff, int w, int h, Object array)
    {
        copy2DOut(xoff, yoff, w, h, array);
    }

    public void copy2DRangeTo(int xoff, int yoff, int w, int h, byte[] data)
    {
        copy2DOut(xoff, yoff, w, h, data);
    }

    public void copy2DRangeTo(int xoff, int yoff, int w, int h, short[] data)
    {
        copy2DOut(xoff, yoff, w, h, data);
    }

    public void copy2DRangeTo(int xoff, int yoff, int w, int h, int[] data)
    {
        copy2DOut(xoff, yoff, w, h, data);
    }

    public void copy2DRangeTo(int xoff, int yoff, int w, int h, float[] data)
    {
        copy2DOut(xoff, yoff, w, h, data);
    }

    public void copy3DRangeTo(int xoff, int yoff, int zoff, int w, int h, int d, Object array)
    {
        checkValid();
        validate3DRange(xoff, yoff, zoff, w, h, d);
        validateArrayType(array, true);

        int size = elementSize();
        int primitiveSize = primitiveSize(array);
        int width = Math.max(mType.getX(), 1);
        int height = Math.max(mType.getY(), 1);
        checkArrayLength(array, w * h * d * size);
        for (int slice = 0; slice < d; slice++)
        {
            for (int row = 0; row < h; row++)
            {
                int source = (((zoff + slice) * height + yoff + row) * width + xoff) * size;
                int target = ((slice * h + row) * w * size) / primitiveSize;
                readArray(source, array, target, w * size);
            }
        }
    }

    public static Allocation createTyped(RenderScript rs, Type type, MipmapControl mips, int usage)
    {
        rs.validate();
        type.checkValid();
        return new Allocation(rs, type, mips, usage);
    }

    public static Allocation createTyped(RenderScript rs, Type type, int usage)
    {
        return createTyped(rs, type, MipmapControl.MIPMAP_NONE, usage);
    }

    public static Allocation createTyped(RenderScript rs, Type type)
    {
        return createTyped(rs, type, MipmapControl.MIPMAP_NONE, USAGE_SCRIPT);
    }

    public static Allocation createSized(RenderScript rs, Element e, int count, int usage)
    {
        rs.validate();
        Type type = new Type.Builder(rs, e).setX(count).create();
        return new Allocation(rs, type, MipmapControl.MIPMAP_NONE, usage);
    }

    public static Allocation createSized(RenderScript rs, Element e, int count)
    {
        return createSized(rs, e, count, USAGE_SCRIPT);
    }

    static Element elementFromBitmap(RenderScript rs, Bitmap b)
    {
        Bitmap.Config config = b.getConfig();
        if (config == Bitmap.Config.ALPHA_8) return Element.A_8(rs);
        if (config == Bitmap.Config.ARGB_4444) return Element.RGBA_4444(rs);
        if (config == Bitmap.Config.ARGB_8888) return Element.RGBA_8888(rs);
        if (config == Bitmap.Config.RGB_565) return Element.RGB_565(rs);
        if (config == Bitmap.Config.RGBA_F16) return Element.F16_4(rs);
        throw new RSInvalidStateException("Bad bitmap type: " + config);
    }

    static Type typeFromBitmap(RenderScript rs, Bitmap b, MipmapControl mips)
    {
        Type.Builder builder = new Type.Builder(rs, elementFromBitmap(rs, b));
        builder.setX(b.getWidth());
        builder.setY(b.getHeight());
        builder.setMipmaps(mips == MipmapControl.MIPMAP_FULL);
        return builder.create();
    }

    public static Allocation createFromBitmap(RenderScript rs, Bitmap b, MipmapControl mips, int usage)
    {
        rs.validate();
        if (b.getConfig() == null)
        {
            if ((usage & USAGE_SHARED) != 0)
            {
                throw new RSIllegalArgumentException("USAGE_SHARED cannot be used with a Bitmap that has a null config.");
            }

            Bitmap converted = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
            return createFromBitmap(rs, converted, mips, usage);
        }

        Allocation allocation = new Allocation(rs, typeFromBitmap(rs, b, mips), mips, usage);
        allocation.copyFrom(b);
        return allocation;
    }

    public ByteBuffer getByteBuffer()
    {
        if (mType.getZ() > 0) throw new RSInvalidStateException("Cannot get ByteBuffer of a 3D Allocation");

        return mData.duplicate().order(ByteOrder.nativeOrder());
    }

    public static Allocation[] createAllocations(RenderScript rs, Type t, int usage, int numAlloc)
    {
        if (numAlloc < 1 || numAlloc > 16) throw new RSIllegalArgumentException("Exceeds the max number of Allocations allowed: 16");
        if ((usage & USAGE_IO_INPUT) == 0) throw new RSIllegalArgumentException("Allocation must be USAGE_IO_INPUT");

        Allocation[] allocations = new Allocation[numAlloc];
        for (int index = 0; index < numAlloc; index++) allocations[index] = createTyped(rs, t, usage);
        return allocations;
    }

    public long getStride()
    {
        return (long) Math.max(mType.getX(), 1) * elementSize();
    }

    public long getTimeStamp()
    {
        return mTimeStamp;
    }

    public Surface getSurface()
    {
        if ((mUsage & USAGE_IO_INPUT) == 0) throw new RSInvalidStateException("Allocation is not a surface texture.");

        if (mSurface == null) mSurface = new Surface();
        return mSurface;
    }

    public void setSurface(Surface sur)
    {
        checkValid();
        if ((mUsage & USAGE_IO_OUTPUT) == 0) throw new RSInvalidStateException("Allocation is not USAGE_IO_OUTPUT.");

        mSurface = sur;
    }

    public static Allocation createFromBitmap(RenderScript rs, Bitmap b)
    {
        return createFromBitmap(rs, b, MipmapControl.MIPMAP_NONE, USAGE_SHARED | USAGE_SCRIPT | USAGE_GRAPHICS_TEXTURE);
    }

    public static Allocation createCubemapFromBitmap(RenderScript rs, Bitmap b, MipmapControl mips, int usage)
    {
        rs.validate();

        int height = b.getHeight();
        int width = b.getWidth();
        if (width % 6 != 0) throw new RSIllegalArgumentException("Cubemap height must be multiple of 6");
        if (width / 6 != height) throw new RSIllegalArgumentException("Only square cube map faces supported");
        if ((height & (height - 1)) != 0) throw new RSIllegalArgumentException("Only power of 2 cube faces supported");

        Type type = new Type.Builder(rs, elementFromBitmap(rs, b)).setX(height).setY(height).setFaces(true)
                .setMipmaps(mips == MipmapControl.MIPMAP_FULL).create();
        Allocation allocation = new Allocation(rs, type, mips, usage);
        int faceBytes = height * height * allocation.elementSize();
        for (int face = 0; face < 6; face++)
        {
            byte[] bytes = RuntimeBitmapBridge.read(b, face * height, 0, height, height, allocation.elementSize());
            allocation.writeBytes(face * faceBytes, bytes, 0, bytes.length);
        }
        return allocation;
    }

    public static Allocation createCubemapFromBitmap(RenderScript rs, Bitmap b)
    {
        return createCubemapFromBitmap(rs, b, MipmapControl.MIPMAP_NONE, USAGE_GRAPHICS_TEXTURE);
    }

    public static Allocation createCubemapFromCubeFaces(RenderScript rs, Bitmap xpos, Bitmap xneg, Bitmap ypos, Bitmap yneg, Bitmap zpos,
            Bitmap zneg, MipmapControl mips, int usage)
    {
        int height = xpos.getHeight();
        if (xpos.getWidth() != height || xneg.getWidth() != height || xneg.getHeight() != height || ypos.getWidth() != height
                || ypos.getHeight() != height || yneg.getWidth() != height || yneg.getHeight() != height
                || zpos.getWidth() != height || zpos.getHeight() != height || zneg.getWidth() != height
                || zneg.getHeight() != height)
        {
            throw new RSIllegalArgumentException("Only square cube map faces supported");
        }
        if ((height & (height - 1)) != 0) throw new RSIllegalArgumentException("Only power of 2 cube faces supported");

        Type type = new Type.Builder(rs, elementFromBitmap(rs, xpos)).setX(height).setY(height).setFaces(true)
                .setMipmaps(mips == MipmapControl.MIPMAP_FULL).create();
        Allocation allocation = new Allocation(rs, type, mips, usage);
        Bitmap[] faces = {xpos, xneg, ypos, yneg, zpos, zneg};
        int faceBytes = height * height * allocation.elementSize();
        for (int face = 0; face < faces.length; face++)
        {
            byte[] bytes = RuntimeBitmapBridge.read(faces[face], 0, 0, height, height, allocation.elementSize());
            allocation.writeBytes(face * faceBytes, bytes, 0, bytes.length);
        }
        return allocation;
    }

    public static Allocation createCubemapFromCubeFaces(RenderScript rs, Bitmap xpos, Bitmap xneg, Bitmap ypos, Bitmap yneg, Bitmap zpos,
            Bitmap zneg)
    {
        return createCubemapFromCubeFaces(rs, xpos, xneg, ypos, yneg, zpos, zneg, MipmapControl.MIPMAP_NONE, USAGE_GRAPHICS_TEXTURE);
    }

    public static Allocation createFromBitmapResource(RenderScript rs, Resources res, int id, MipmapControl mips, int usage)
    {
        rs.validate();
        if ((usage & (USAGE_SHARED | USAGE_IO_INPUT | USAGE_IO_OUTPUT)) != 0)
        {
            throw new RSIllegalArgumentException("Unsupported usage specified.");
        }

        Bitmap bitmap = BitmapFactory.decodeResource(res, id);
        Allocation allocation = createFromBitmap(rs, bitmap, mips, usage);
        bitmap.recycle();
        return allocation;
    }

    public static Allocation createFromBitmapResource(RenderScript rs, Resources res, int id)
    {
        return createFromBitmapResource(rs, res, id, MipmapControl.MIPMAP_NONE, USAGE_GRAPHICS_TEXTURE);
    }

    public static Allocation createFromString(RenderScript rs, String str, int usage)
    {
        rs.validate();
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        Allocation allocation = createSized(rs, Element.U8(rs), bytes.length, usage);
        allocation.copyFrom(bytes);
        return allocation;
    }

    public void setOnBufferAvailableListener(OnBufferAvailableListener callback)
    {
        mBufferNotifier = callback;
    }

    @Override
    public void destroy()
    {
        mBufferNotifier = null;
        super.destroy();
    }

    private void validateBitmapSize(Bitmap b)
    {
        if (mType.getX() != b.getWidth() || mType.getY() != b.getHeight())
        {
            throw new RSIllegalArgumentException("Cannot update allocation from bitmap, sizes mismatch");
        }
    }

    private void copyWholeFrom(Object array, boolean checked)
    {
        checkValid();
        validateArrayType(array, checked);

        int length = Math.min(getBytesSize(), Array.getLength(array) * primitiveSize(array));
        if (Array.getLength(array) * primitiveSize(array) < mType.getCount() * elementSize())
        {
            throw new RSIllegalArgumentException("Array too small for allocation type.");
        }
        writeArray(0, array, 0, length);
    }

    private void copyWholeTo(Object array, boolean checked)
    {
        checkValid();
        validateArrayType(array, checked);

        int length = Math.min(getBytesSize(), Array.getLength(array) * primitiveSize(array));
        if (Array.getLength(array) * primitiveSize(array) < mType.getCount() * elementSize())
        {
            throw new RSIllegalArgumentException("Size of output array cannot be smaller than size of allocation.");
        }
        readArray(0, array, 0, length);
    }

    private void copy1DIn(int off, int count, Object array, boolean checked)
    {
        checkValid();
        validateArrayType(array, checked);
        validate1DRange(off, count);

        int size = elementSize();
        checkArrayLength(array, count * size);
        writeArray(off * size, array, 0, count * size);
    }

    private void copy1DOut(int off, int count, Object array, boolean checked)
    {
        checkValid();
        validateArrayType(array, checked);
        validate1DRange(off, count);

        int size = elementSize();
        checkArrayLength(array, count * size);
        readArray(off * size, array, 0, count * size);
    }

    private void copy2DIn(int xoff, int yoff, int w, int h, Object array, boolean checked)
    {
        checkValid();
        validateArrayType(array, checked);
        validate2DRange(xoff, yoff, w, h);

        int size = elementSize();
        int primitiveSize = primitiveSize(array);
        int width = Math.max(mType.getX(), 1);
        checkArrayLength(array, w * h * size);
        for (int row = 0; row < h; row++)
        {
            int target = ((yoff + row) * width + xoff) * size;
            writeArray(target, array, (row * w * size) / primitiveSize, w * size);
        }
    }

    private void copy2DOut(int xoff, int yoff, int w, int h, Object array)
    {
        checkValid();
        validateArrayType(array, true);
        validate2DRange(xoff, yoff, w, h);

        int size = elementSize();
        int primitiveSize = primitiveSize(array);
        int width = Math.max(mType.getX(), 1);
        checkArrayLength(array, w * h * size);
        for (int row = 0; row < h; row++)
        {
            int source = ((yoff + row) * width + xoff) * size;
            readArray(source, array, (row * w * size) / primitiveSize, w * size);
        }
    }

    private void validate1DRange(int off, int count)
    {
        if (off < 0) throw new RSIllegalArgumentException("Offset must be >= 0.");
        if (count < 1) throw new RSIllegalArgumentException("Count must be >= 1.");
        if (off + count > mType.getCount())
        {
            throw new RSIllegalArgumentException("Overflow, Available count " + mType.getCount() + ", got " + count + " at offset "
                    + off + ".");
        }
    }

    private void validate2DRange(int xoff, int yoff, int w, int h)
    {
        if (xoff < 0 || yoff < 0) throw new RSIllegalArgumentException("Offset cannot be negative.");
        if (h < 0 || w < 0) throw new RSIllegalArgumentException("Height or width cannot be negative.");
        if (xoff + w > Math.max(mType.getX(), 1) || yoff + h > Math.max(mType.getY(), 1))
        {
            throw new RSIllegalArgumentException("Updated region larger than allocation.");
        }
    }

    private void validate3DRange(int xoff, int yoff, int zoff, int w, int h, int d)
    {
        if (xoff < 0 || yoff < 0 || zoff < 0) throw new RSIllegalArgumentException("Offset cannot be negative.");
        if (h < 0 || w < 0 || d < 0) throw new RSIllegalArgumentException("Height or width cannot be negative.");
        if (xoff + w > Math.max(mType.getX(), 1) || yoff + h > Math.max(mType.getY(), 1) || zoff + d > Math.max(mType.getZ(), 1))
        {
            throw new RSIllegalArgumentException("Updated region larger than allocation.");
        }
    }

    private static void checkArrayLength(Object array, int bytes)
    {
        if (Array.getLength(array) * primitiveSize(array) < bytes)
        {
            throw new RSIllegalArgumentException("Array too small for allocation type.");
        }
    }

    private static int primitiveSize(Object array)
    {
        Class<?> component = array.getClass().getComponentType();
        if (component == Byte.TYPE || component == Boolean.TYPE) return 1;
        if (component == Short.TYPE || component == Character.TYPE) return 2;
        if (component == Integer.TYPE || component == Float.TYPE) return 4;
        return 8;
    }

    private void validateArrayType(Object array, boolean checked)
    {
        if (array == null) throw new RSIllegalArgumentException("Array cannot be null");

        Class<?> component = array.getClass().getComponentType();
        if (component == null || !component.isPrimitive())
        {
            throw new RSIllegalArgumentException("Object passed is not an array of primitives.");
        }
        if (!checked) return;

        Element.DataType type = mType.getElement().mType;
        if (component == Long.TYPE)
        {
            if (type != Element.DataType.SIGNED_64 && type != Element.DataType.UNSIGNED_64)
            {
                throw new RSIllegalArgumentException("64 bit integer source does not match allocation type " + type);
            }
        }
        else if (component == Integer.TYPE)
        {
            if (type != Element.DataType.SIGNED_32 && type != Element.DataType.UNSIGNED_32 && type != Element.DataType.SIGNED_64
                    && type != Element.DataType.UNSIGNED_64)
            {
                throw new RSIllegalArgumentException("32 bit integer source does not match allocation type " + type);
            }
        }
        else if (component == Short.TYPE)
        {
            if (type != Element.DataType.SIGNED_16 && type != Element.DataType.UNSIGNED_16 && type != Element.DataType.FLOAT_16)
            {
                throw new RSIllegalArgumentException("16 bit integer source does not match allocation type " + type);
            }
        }
        else if (component == Byte.TYPE)
        {
            if (type != Element.DataType.SIGNED_8 && type != Element.DataType.UNSIGNED_8)
            {
                throw new RSIllegalArgumentException("8 bit integer source does not match allocation type " + type);
            }
        }
        else if (component == Float.TYPE)
        {
            if (type != Element.DataType.FLOAT_32)
            {
                throw new RSIllegalArgumentException("32 bit float source does not match allocation type " + type);
            }
        }
        else if (component == Double.TYPE)
        {
            if (type != Element.DataType.FLOAT_64)
            {
                throw new RSIllegalArgumentException("64 bit float source does not match allocation type " + type);
            }
        }
        else
        {
            throw new RSIllegalArgumentException("Parameter of type " + component.getSimpleName() + "[] is not compatible.");
        }
    }

    private void writeArray(int byteOffset, Object array, int arrayIndex, int byteCount)
    {
        int size = primitiveSize(array);
        int count = byteCount / size;
        ByteBuffer data = mData;

        if (array instanceof byte[])
        {
            byte[] values = (byte[]) array;
            for (int index = 0; index < count; index++) data.put(byteOffset + index, values[arrayIndex + index]);
        }
        else if (array instanceof short[])
        {
            short[] values = (short[]) array;
            for (int index = 0; index < count; index++) data.putShort(byteOffset + index * 2, values[arrayIndex + index]);
        }
        else if (array instanceof int[])
        {
            int[] values = (int[]) array;
            for (int index = 0; index < count; index++) data.putInt(byteOffset + index * 4, values[arrayIndex + index]);
        }
        else if (array instanceof float[])
        {
            float[] values = (float[]) array;
            for (int index = 0; index < count; index++) data.putFloat(byteOffset + index * 4, values[arrayIndex + index]);
        }
        else if (array instanceof long[])
        {
            long[] values = (long[]) array;
            for (int index = 0; index < count; index++) data.putLong(byteOffset + index * 8, values[arrayIndex + index]);
        }
        else if (array instanceof double[])
        {
            double[] values = (double[]) array;
            for (int index = 0; index < count; index++) data.putDouble(byteOffset + index * 8, values[arrayIndex + index]);
        }
        else if (array instanceof char[])
        {
            char[] values = (char[]) array;
            for (int index = 0; index < count; index++) data.putChar(byteOffset + index * 2, values[arrayIndex + index]);
        }
        else if (array instanceof boolean[])
        {
            boolean[] values = (boolean[]) array;
            for (int index = 0; index < count; index++) data.put(byteOffset + index, (byte) (values[arrayIndex + index] ? 1 : 0));
        }
    }

    private void readArray(int byteOffset, Object array, int arrayIndex, int byteCount)
    {
        int size = primitiveSize(array);
        int count = byteCount / size;
        ByteBuffer data = mData;

        if (array instanceof byte[])
        {
            byte[] values = (byte[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.get(byteOffset + index);
        }
        else if (array instanceof short[])
        {
            short[] values = (short[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.getShort(byteOffset + index * 2);
        }
        else if (array instanceof int[])
        {
            int[] values = (int[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.getInt(byteOffset + index * 4);
        }
        else if (array instanceof float[])
        {
            float[] values = (float[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.getFloat(byteOffset + index * 4);
        }
        else if (array instanceof long[])
        {
            long[] values = (long[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.getLong(byteOffset + index * 8);
        }
        else if (array instanceof double[])
        {
            double[] values = (double[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.getDouble(byteOffset + index * 8);
        }
        else if (array instanceof char[])
        {
            char[] values = (char[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.getChar(byteOffset + index * 2);
        }
        else if (array instanceof boolean[])
        {
            boolean[] values = (boolean[]) array;
            for (int index = 0; index < count; index++) values[arrayIndex + index] = data.get(byteOffset + index) != 0;
        }
    }

    void writeBytes(int byteOffset, byte[] source, int sourceOffset, int length)
    {
        for (int index = 0; index < length; index++) mData.put(byteOffset + index, source[sourceOffset + index]);
    }

    void readBytes(int byteOffset, byte[] target, int targetOffset, int length)
    {
        for (int index = 0; index < length; index++) target[targetOffset + index] = mData.get(byteOffset + index);
    }

    private static void copyBuffer(ByteBuffer source, int sourceOffset, ByteBuffer target, int targetOffset, int length)
    {
        if (source == target && sourceOffset < targetOffset)
        {
            for (int index = length - 1; index >= 0; index--) target.put(targetOffset + index, source.get(sourceOffset + index));
            return;
        }

        for (int index = 0; index < length; index++) target.put(targetOffset + index, source.get(sourceOffset + index));
    }

    static final class RuntimeBitmapBridge
    {
        private RuntimeBitmapBridge()
        {
        }

        static byte[] read(Bitmap bitmap, int x, int y, int width, int height, int elementSize)
        {
            int[] colors = new int[width * height];
            bitmap.getPixels(colors, 0, width, x, y, width, height);

            Bitmap.Config config = bitmap.getConfig();
            boolean premultiplied = bitmap.isPremultiplied();
            byte[] bytes = new byte[width * height * elementSize];

            for (int index = 0; index < colors.length; index++)
            {
                int color = colors[index];
                int alpha = color >>> 24;
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;
                if (premultiplied && alpha != 0xFF)
                {
                    red = (red * alpha + 127) / 255;
                    green = (green * alpha + 127) / 255;
                    blue = (blue * alpha + 127) / 255;
                }

                int offset = index * elementSize;
                if (config == Bitmap.Config.ALPHA_8 || elementSize == 1)
                {
                    bytes[offset] = (byte) alpha;
                }
                else if (config == Bitmap.Config.RGB_565)
                {
                    int packed = ((red >> 3) << 11) | ((green >> 2) << 5) | (blue >> 3);
                    bytes[offset] = (byte) packed;
                    bytes[offset + 1] = (byte) (packed >> 8);
                }
                else if (config == Bitmap.Config.ARGB_4444)
                {
                    int packed = ((red >> 4) << 12) | ((green >> 4) << 8) | ((blue >> 4) << 4) | (alpha >> 4);
                    bytes[offset] = (byte) packed;
                    bytes[offset + 1] = (byte) (packed >> 8);
                }
                else if (config == Bitmap.Config.RGBA_F16 && elementSize == 8)
                {
                    putHalf(bytes, offset, red / 255.0f);
                    putHalf(bytes, offset + 2, green / 255.0f);
                    putHalf(bytes, offset + 4, blue / 255.0f);
                    putHalf(bytes, offset + 6, alpha / 255.0f);
                }
                else
                {
                    bytes[offset] = (byte) red;
                    if (elementSize > 1) bytes[offset + 1] = (byte) green;
                    if (elementSize > 2) bytes[offset + 2] = (byte) blue;
                    if (elementSize > 3) bytes[offset + 3] = (byte) alpha;
                }
            }

            return bytes;
        }

        static void write(Bitmap bitmap, int x, int y, int width, int height, int elementSize, byte[] bytes)
        {
            Bitmap.Config config = bitmap.getConfig();
            boolean premultiplied = bitmap.isPremultiplied();
            int[] colors = new int[width * height];

            for (int index = 0; index < colors.length; index++)
            {
                int offset = index * elementSize;
                int alpha;
                int red;
                int green;
                int blue;

                if (config == Bitmap.Config.ALPHA_8 || elementSize == 1)
                {
                    alpha = bytes[offset] & 0xFF;
                    red = 0;
                    green = 0;
                    blue = 0;
                }
                else if (config == Bitmap.Config.RGB_565)
                {
                    int packed = (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
                    red = ((packed >> 11) & 0x1F) * 255 / 31;
                    green = ((packed >> 5) & 0x3F) * 255 / 63;
                    blue = (packed & 0x1F) * 255 / 31;
                    alpha = 0xFF;
                }
                else if (config == Bitmap.Config.ARGB_4444)
                {
                    int packed = (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
                    red = ((packed >> 12) & 0xF) * 17;
                    green = ((packed >> 8) & 0xF) * 17;
                    blue = ((packed >> 4) & 0xF) * 17;
                    alpha = (packed & 0xF) * 17;
                }
                else if (config == Bitmap.Config.RGBA_F16 && elementSize == 8)
                {
                    red = channel(getHalf(bytes, offset));
                    green = channel(getHalf(bytes, offset + 2));
                    blue = channel(getHalf(bytes, offset + 4));
                    alpha = channel(getHalf(bytes, offset + 6));
                }
                else
                {
                    red = bytes[offset] & 0xFF;
                    green = elementSize > 1 ? bytes[offset + 1] & 0xFF : 0;
                    blue = elementSize > 2 ? bytes[offset + 2] & 0xFF : 0;
                    alpha = elementSize > 3 ? bytes[offset + 3] & 0xFF : 0xFF;
                }

                if (premultiplied && alpha != 0 && alpha != 0xFF)
                {
                    red = Math.min(255, (red * 255 + alpha / 2) / alpha);
                    green = Math.min(255, (green * 255 + alpha / 2) / alpha);
                    blue = Math.min(255, (blue * 255 + alpha / 2) / alpha);
                }
                else if (premultiplied && alpha == 0)
                {
                    red = 0;
                    green = 0;
                    blue = 0;
                }

                colors[index] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }

            bitmap.setPixels(colors, 0, width, x, y, width, height);
        }

        private static int channel(float value)
        {
            return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
        }

        private static void putHalf(byte[] bytes, int offset, float value)
        {
            short half = Half.toHalf(value);
            bytes[offset] = (byte) half;
            bytes[offset + 1] = (byte) (half >> 8);
        }

        private static float getHalf(byte[] bytes, int offset)
        {
            short half = (short) ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8));
            return Half.toFloat(half);
        }
    }
}
