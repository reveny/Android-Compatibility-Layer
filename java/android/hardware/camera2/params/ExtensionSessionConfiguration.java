package android.hardware.camera2.params;

import android.graphics.ColorSpace;
import android.hardware.camera2.CameraExtensionSession;

import java.util.List;
import java.util.concurrent.Executor;

public final class ExtensionSessionConfiguration
{
    private final int mExtensionType;
    private final List<OutputConfiguration> mOutputs;
    private final Executor mExecutor;
    private final CameraExtensionSession.StateCallback mCallback;
    private OutputConfiguration mPostviewOutput;
    private ColorSpace.Named mColorSpace;

    public ExtensionSessionConfiguration(int extension, List<OutputConfiguration> outputs, Executor executor,
            CameraExtensionSession.StateCallback listener)
    {
        mExtensionType = extension;
        mOutputs = outputs;
        mExecutor = executor;
        mCallback = listener;
    }

    public int getExtension()
    {
        return mExtensionType;
    }

    public void setPostviewOutputConfiguration(OutputConfiguration postviewOutput)
    {
        mPostviewOutput = postviewOutput;
    }

    public OutputConfiguration getPostviewOutputConfiguration()
    {
        return mPostviewOutput;
    }

    public List<OutputConfiguration> getOutputConfigurations()
    {
        return mOutputs;
    }

    public CameraExtensionSession.StateCallback getStateCallback()
    {
        return mCallback;
    }

    public Executor getExecutor()
    {
        return mExecutor;
    }

    public void setColorSpace(ColorSpace.Named colorSpace)
    {
        mColorSpace = colorSpace;
    }

    public void clearColorSpace()
    {
        mColorSpace = null;
    }

    public ColorSpace getColorSpace()
    {
        return mColorSpace == null ? null : ColorSpace.get(mColorSpace);
    }
}
