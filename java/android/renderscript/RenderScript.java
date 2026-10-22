package android.renderscript;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RenderScript
{
    public static final int CREATE_FLAG_NONE = 0x0000;
    public static final int CREATE_FLAG_LOW_LATENCY = 0x0002;
    public static final int CREATE_FLAG_LOW_POWER = 0x0004;

    private static final int CREATE_FLAG_WAIT_FOR_ATTACH = 0x0008;
    private static final int CREATE_FLAG_OPT_LEVEL_0 = 0x0010;
    private static final int CREATE_FLAG_ANY = CREATE_FLAG_LOW_LATENCY | CREATE_FLAG_LOW_POWER | CREATE_FLAG_WAIT_FOR_ATTACH
            | CREATE_FLAG_OPT_LEVEL_0;

    private static final ArrayList<RenderScript> sContexts = new ArrayList<>();

    private static long sNextIdentifier = 1;

    private final Context mApplicationContext;
    private final ContextType mContextType;
    private final int mCreateFlags;
    private final Map<String, Element> mElements = new HashMap<>();

    private RSMessageHandler mMessageCallback;
    private RSErrorHandler mErrorCallback;
    private Priority mPriority = Priority.NORMAL;
    private boolean mDestroyed;

    public enum ContextType
    {
        NORMAL,
        DEBUG,
        PROFILE
    }

    public enum Priority
    {
        LOW,
        NORMAL
    }

    public static class RSMessageHandler implements Runnable
    {
        protected int[] mData;
        protected int mID;
        protected int mLength;

        public RSMessageHandler()
        {
        }

        @Override
        public void run()
        {
        }
    }

    public static class RSErrorHandler implements Runnable
    {
        protected String mErrorMessage;
        protected int mErrorNum;

        public RSErrorHandler()
        {
        }

        @Override
        public void run()
        {
        }
    }

    RenderScript(Context context, ContextType contextType, int createFlags)
    {
        Context applicationContext = context == null ? null : context.getApplicationContext();
        mApplicationContext = applicationContext != null ? applicationContext : context;
        mContextType = contextType;
        mCreateFlags = createFlags;
    }

    public static long getMinorVersion()
    {
        return 0;
    }

    public void setMessageHandler(RSMessageHandler messageHandler)
    {
        mMessageCallback = messageHandler;
    }

    public RSMessageHandler getMessageHandler()
    {
        return mMessageCallback;
    }

    public void sendMessage(int id, int[] data)
    {
        validate();
    }

    public void setErrorHandler(RSErrorHandler errorHandler)
    {
        mErrorCallback = errorHandler;
    }

    public RSErrorHandler getErrorHandler()
    {
        return mErrorCallback;
    }

    public void setPriority(Priority priority)
    {
        validate();
        if (priority == null) throw new RSIllegalArgumentException("priority == null");
        mPriority = priority;
    }

    public final Context getApplicationContext()
    {
        return mApplicationContext;
    }

    public static RenderScript create(Context context)
    {
        return create(context, ContextType.NORMAL);
    }

    public static RenderScript create(Context context, ContextType contextType)
    {
        return create(context, contextType, CREATE_FLAG_NONE);
    }

    public static RenderScript create(Context context, ContextType contextType, int flags)
    {
        if ((flags & ~CREATE_FLAG_ANY) != 0) throw new RSIllegalArgumentException("Invalid flags passed.");

        RenderScript renderScript = new RenderScript(context, contextType, flags);
        synchronized (sContexts)
        {
            sContexts.add(renderScript);
        }
        return renderScript;
    }

    public static void releaseAllContexts()
    {
        ArrayList<RenderScript> contexts;
        synchronized (sContexts)
        {
            contexts = new ArrayList<>(sContexts);
            sContexts.clear();
        }

        for (RenderScript renderScript : contexts) renderScript.destroy();
    }

    public static RenderScript createMultiContext(Context context, ContextType contextType, int flags, int api)
    {
        return create(context, contextType, flags);
    }

    public void contextDump()
    {
        validate();
    }

    public void finish()
    {
        validate();
    }

    @Override
    protected void finalize() throws Throwable
    {
        mDestroyed = true;
        super.finalize();
    }

    public void destroy()
    {
        if (mDestroyed) return;

        validate();
        mDestroyed = true;
        synchronized (sContexts)
        {
            sContexts.remove(this);
        }
    }

    void validate()
    {
        if (mDestroyed) throw new RSInvalidStateException("Calling RS with no Context active.");
    }

    static synchronized long nextIdentifier()
    {
        return sNextIdentifier++;
    }

    synchronized Element cachedElement(String name)
    {
        return mElements.get(name);
    }

    synchronized Element cacheElement(String name, Element element)
    {
        Element existing = mElements.get(name);
        if (existing != null) return existing;

        mElements.put(name, element);
        return element;
    }

    ContextType getContextType()
    {
        return mContextType;
    }

    int getCreateFlags()
    {
        return mCreateFlags;
    }

    Priority getPriority()
    {
        return mPriority;
    }
}
