package android.media.midi;

import java.io.Closeable;
import java.io.IOException;

public final class MidiDevice implements Closeable
{
    private final MidiDeviceInfo mDeviceInfo;
    private final MidiOutputPort[] mOutputPorts;
    private boolean mClosed;

    MidiDevice(MidiDeviceInfo deviceInfo)
    {
        mDeviceInfo = deviceInfo;
        mOutputPorts = new MidiOutputPort[deviceInfo.getOutputPortCount()];

        for (int index = 0; index < mOutputPorts.length; index++)
        {
            mOutputPorts[index] = new MidiOutputPort(index);
        }
    }

    @Override
    public void close() throws IOException
    {
        synchronized (this)
        {
            if (mClosed) return;

            mClosed = true;
        }

        for (MidiOutputPort port : mOutputPorts)
        {
            port.close();
        }
    }

    public MidiConnection connectPorts(MidiInputPort inputPort, int outputPortNumber)
    {
        if (outputPortNumber < 0 || outputPortNumber >= mDeviceInfo.getOutputPortCount())
        {
            throw new IllegalArgumentException("outputPortNumber out of range");
        }

        synchronized (this)
        {
            if (mClosed) return null;
        }

        MidiOutputPort outputPort = mOutputPorts[outputPortNumber];
        outputPort.connect(inputPort);
        return new MidiConnection(outputPort, inputPort);
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            close();
        }
        finally
        {
            super.finalize();
        }
    }

    public MidiDeviceInfo getInfo()
    {
        return mDeviceInfo;
    }

    public MidiInputPort openInputPort(int portNumber)
    {
        synchronized (this)
        {
            if (mClosed) return null;
        }

        if (portNumber < 0 || portNumber >= mDeviceInfo.getInputPortCount()) return null;

        return new MidiInputPort(portNumber, null);
    }

    public MidiOutputPort openOutputPort(int portNumber)
    {
        synchronized (this)
        {
            if (mClosed) return null;
        }

        if (portNumber < 0 || portNumber >= mOutputPorts.length) return null;

        return mOutputPorts[portNumber];
    }

    @Override
    public String toString()
    {
        return "MidiDevice: " + mDeviceInfo.toString();
    }

    public class MidiConnection implements Closeable
    {
        private final MidiOutputPort mOutputPort;
        private final MidiInputPort mInputPort;
        private boolean mConnectionClosed;

        MidiConnection(MidiOutputPort outputPort, MidiInputPort inputPort)
        {
            mOutputPort = outputPort;
            mInputPort = inputPort;
        }

        @Override
        public void close() throws IOException
        {
            synchronized (this)
            {
                if (mConnectionClosed) return;

                mConnectionClosed = true;
            }

            mOutputPort.disconnect(mInputPort);
        }

        @Override
        protected void finalize() throws Throwable
        {
            try
            {
                close();
            }
            finally
            {
                super.finalize();
            }
        }
    }
}
