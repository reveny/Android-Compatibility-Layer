package android.renderscript;

import java.nio.ByteBuffer;

public final class ScriptIntrinsicBlur extends ScriptIntrinsic
{
    private static final int KERNEL_SLOT = 0;
    private static final int RADIUS_SLOT = 0;
    private static final int INPUT_SLOT = 1;

    private final Element mElement;

    private float mRadius = 5.0f;
    private Allocation mInput;

    private ScriptIntrinsicBlur(RenderScript rs, Element element)
    {
        super(rs);
        mElement = element;
    }

    public static ScriptIntrinsicBlur create(RenderScript rs, Element e)
    {
        if (!e.isCompatible(Element.U8_4(rs)) && !e.isCompatible(Element.U8(rs)))
        {
            throw new RSIllegalArgumentException("Unsupported element type.");
        }

        ScriptIntrinsicBlur blur = new ScriptIntrinsicBlur(rs, e);
        blur.setRadius(5.0f);
        return blur;
    }

    public void setInput(Allocation ain)
    {
        if (ain.getType().getY() == 0) throw new RSIllegalArgumentException("Input set to a 1D Allocation");

        mInput = ain;
        setVar(INPUT_SLOT, ain);
    }

    public void setRadius(float radius)
    {
        if (radius <= 0 || radius > 25) throw new RSIllegalArgumentException("Radius out of range (0 < r <= 25).");

        mRadius = radius;
        setVar(RADIUS_SLOT, radius);
    }

    public void forEach(Allocation aout)
    {
        forEach(aout, null);
    }

    public void forEach(Allocation aout, Script.LaunchOptions opt)
    {
        if (aout.getType().getY() == 0) throw new RSIllegalArgumentException("Output is a 1D Allocation");
        checkValid();
        if (mInput == null) throw new RSIllegalArgumentException("Input is required to be set before forEach.");

        int channels = mInput.elementSize();
        if (channels != 1 && channels != 4) throw new RSIllegalArgumentException("Unsupported element type.");
        if (aout.elementSize() != channels) throw new RSIllegalArgumentException("Input and output element types must match.");

        int inputWidth = mInput.getType().getX();
        int inputHeight = mInput.getType().getY();
        int outputWidth = aout.getType().getX();
        int outputHeight = aout.getType().getY();

        int startX = 0;
        int endX = outputWidth;
        int startY = 0;
        int endY = outputHeight;
        if (opt != null)
        {
            if (opt.getXEnd() > 0)
            {
                startX = opt.getXStart();
                endX = Math.min(opt.getXEnd(), outputWidth);
            }
            if (opt.getYEnd() > 0)
            {
                startY = opt.getYStart();
                endY = Math.min(opt.getYEnd(), outputHeight);
            }
        }
        if (startX >= endX || startY >= endY) return;

        float[] weights = weights(mRadius);
        int reach = weights.length / 2;
        ByteBuffer source = mInput.data();

        int firstRow = Math.max(0, startY - reach);
        int lastRow = Math.min(inputHeight - 1, endY - 1 + reach);
        int rows = lastRow - firstRow + 1;
        int columns = endX - startX;
        float[] horizontal = new float[rows * columns * channels];

        for (int row = 0; row < rows; row++)
        {
            int inputRow = firstRow + row;
            for (int column = 0; column < columns; column++)
            {
                int x = startX + column;
                int target = (row * columns + column) * channels;
                for (int tap = -reach; tap <= reach; tap++)
                {
                    int sampleX = Math.max(0, Math.min(inputWidth - 1, x + tap));
                    float weight = weights[tap + reach];
                    int sample = (inputRow * inputWidth + sampleX) * channels;
                    for (int channel = 0; channel < channels; channel++)
                    {
                        horizontal[target + channel] += weight * (source.get(sample + channel) & 0xFF);
                    }
                }
            }
        }

        ByteBuffer target = aout.data();
        float[] accumulator = new float[channels];
        for (int y = startY; y < endY; y++)
        {
            for (int column = 0; column < columns; column++)
            {
                for (int channel = 0; channel < channels; channel++) accumulator[channel] = 0;

                for (int tap = -reach; tap <= reach; tap++)
                {
                    int sampleY = Math.max(0, Math.min(inputHeight - 1, y + tap));
                    int row = Math.max(0, Math.min(rows - 1, sampleY - firstRow));
                    float weight = weights[tap + reach];
                    int sample = (row * columns + column) * channels;
                    for (int channel = 0; channel < channels; channel++) accumulator[channel] += weight * horizontal[sample + channel];
                }

                int output = (y * outputWidth + startX + column) * channels;
                for (int channel = 0; channel < channels; channel++)
                {
                    int value = Math.round(accumulator[channel]);
                    target.put(output + channel, (byte) Math.max(0, Math.min(255, value)));
                }
            }
        }
    }

    private static float[] weights(float radius)
    {
        float sigma = 0.4f * radius + 0.6f;
        int reach = (int) Math.ceil(radius);
        float[] weights = new float[reach * 2 + 1];
        double exponentScale = -1.0 / (2.0 * sigma * sigma);

        float sum = 0;
        for (int offset = -reach; offset <= reach; offset++)
        {
            float weight = (float) Math.exp(offset * offset * exponentScale);
            weights[offset + reach] = weight;
            sum += weight;
        }
        for (int index = 0; index < weights.length; index++) weights[index] /= sum;
        return weights;
    }

    public Script.KernelID getKernelID()
    {
        return createKernelID(KERNEL_SLOT, 2, null, mElement);
    }

    public Script.FieldID getFieldID_Input()
    {
        return createFieldID(INPUT_SLOT, null);
    }
}
