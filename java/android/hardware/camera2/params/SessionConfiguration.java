package android.hardware.camera2.params;

import android.graphics.ColorSpace;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public final class SessionConfiguration implements Parcelable
{
    public static final int SESSION_REGULAR = 0;
    public static final int SESSION_HIGH_SPEED = 1;

    public static final Parcelable.Creator<SessionConfiguration> CREATOR = new Parcelable.Creator<SessionConfiguration>()
    {
        @Override
        public SessionConfiguration createFromParcel(Parcel source)
        {
            int sessionType = source.readInt();
            int count = source.readInt();
            List<OutputConfiguration> outputs = new ArrayList<>();
            for (int index = 0; index < count; ++index)
            {
                outputs.add(OutputConfiguration.CREATOR.createFromParcel(source));
            }
            SessionConfiguration configuration = new SessionConfiguration(sessionType, outputs);
            int colorSpace = source.readInt();
            if (colorSpace >= 0) configuration.setColorSpace(ColorSpace.Named.values()[colorSpace]);
            return configuration;
        }

        @Override
        public SessionConfiguration[] newArray(int size)
        {
            return new SessionConfiguration[size];
        }
    };

    private final int mSessionType;
    private final List<OutputConfiguration> mOutputConfigurations;
    private CameraCaptureSession.StateCallback mStateCallback;
    private Executor mExecutor;
    private InputConfiguration mInputConfig;
    private CaptureRequest mSessionParameters;
    private ColorSpace.Named mColorSpace;

    public SessionConfiguration(int sessionType, List<OutputConfiguration> outputs)
    {
        mSessionType = sessionType;
        mOutputConfigurations = outputs;
    }

    public SessionConfiguration(int sessionType, List<OutputConfiguration> outputs, Executor executor,
            CameraCaptureSession.StateCallback callback)
    {
        mSessionType = sessionType;
        mOutputConfigurations = outputs;
        mStateCallback = callback;
        mExecutor = executor;
    }

    public int getSessionType()
    {
        return mSessionType;
    }

    public List<OutputConfiguration> getOutputConfigurations()
    {
        return mOutputConfigurations;
    }

    public CameraCaptureSession.StateCallback getStateCallback()
    {
        return mStateCallback;
    }

    public Executor getExecutor()
    {
        return mExecutor;
    }

    public void setStateCallback(Executor executor, CameraCaptureSession.StateCallback callback)
    {
        mExecutor = executor;
        mStateCallback = callback;
    }

    public void setInputConfiguration(InputConfiguration input)
    {
        if (mSessionType == SESSION_HIGH_SPEED)
        {
            throw new UnsupportedOperationException("Method not supported for high speed session types");
        }
        mInputConfig = input;
    }

    public InputConfiguration getInputConfiguration()
    {
        return mInputConfig;
    }

    public void setSessionParameters(CaptureRequest params)
    {
        mSessionParameters = params;
    }

    public CaptureRequest getSessionParameters()
    {
        return mSessionParameters;
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

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        if (dest == null) throw new IllegalArgumentException("dest must not be null");
        dest.writeInt(mSessionType);
        dest.writeInt(mOutputConfigurations == null ? 0 : mOutputConfigurations.size());
        if (mOutputConfigurations != null)
        {
            for (OutputConfiguration output : mOutputConfigurations)
            {
                output.writeToParcel(dest, flags);
            }
        }
        dest.writeInt(mColorSpace == null ? -1 : mColorSpace.ordinal());
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof SessionConfiguration)) return false;

        SessionConfiguration configuration = (SessionConfiguration)other;
        return mSessionType == configuration.mSessionType && mColorSpace == configuration.mColorSpace
                && Objects.equals(mInputConfig, configuration.mInputConfig)
                && Objects.equals(mOutputConfigurations, configuration.mOutputConfigurations);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mOutputConfigurations, mInputConfig, mSessionType, mColorSpace);
    }
}
