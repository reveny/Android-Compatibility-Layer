package android.webkit;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public abstract class TracingController
{
    public TracingController()
    {
    }

    public static TracingController getInstance()
    {
        return LocalTracingController.INSTANCE;
    }

    public abstract void start(TracingConfig tracingConfig);

    public abstract boolean stop(OutputStream outputStream, Executor executor);

    public abstract boolean isTracing();

    private static final class LocalTracingController extends TracingController
    {
        static final LocalTracingController INSTANCE = new LocalTracingController();

        private boolean mTracing;

        @Override
        public synchronized void start(TracingConfig tracingConfig)
        {
            if (tracingConfig == null) throw new IllegalArgumentException("tracingConfig cannot be null");
            if (mTracing) throw new IllegalStateException("cannot start tracing: tracing is already enabled");
            mTracing = true;
        }

        @Override
        public boolean stop(final OutputStream outputStream, Executor executor)
        {
            synchronized (this)
            {
                if (!mTracing) return false;
                mTracing = false;
            }
            if (outputStream == null) return true;
            Runnable writer = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        outputStream.write("{\"traceEvents\":[]}".getBytes(StandardCharsets.UTF_8));
                        outputStream.close();
                    }
                    catch (IOException ignored)
                    {
                    }
                }
            };
            if (executor != null)
            {
                executor.execute(writer);
            }
            else
            {
                writer.run();
            }
            return true;
        }

        @Override
        public synchronized boolean isTracing()
        {
            return mTracing;
        }
    }
}
