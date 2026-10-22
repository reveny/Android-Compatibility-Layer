package android.opengl;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGL10;

final class RuntimeEglBridge
{
    static final EGL10 EGL = (EGL10) javax.microedition.khronos.egl.EGLContext.getEGL();

    private static final Map<Long, Object> sObjects = new HashMap<>();
    private static final Map<Object, Long> sHandles = new IdentityHashMap<>();

    private static long sNextHandle = 1;

    private RuntimeEglBridge()
    {
    }

    static synchronized long handle(Object object)
    {
        if (object == null) return 0;

        Long handle = sHandles.get(object);
        if (handle == null)
        {
            handle = sNextHandle++;
            sHandles.put(object, handle);
            sObjects.put(handle, object);
        }
        return handle;
    }

    static synchronized long newHandle()
    {
        return sNextHandle++;
    }

    static synchronized void forget(EGLObjectHandle handle)
    {
        if (handle == null) return;

        Object object = sObjects.remove(handle.getNativeHandle());
        if (object != null) sHandles.remove(object);
    }

    static synchronized Object resolve(EGLObjectHandle handle)
    {
        if (handle == null) return null;
        return sObjects.get(handle.getNativeHandle());
    }

    static javax.microedition.khronos.egl.EGLDisplay display(EGLDisplay display)
    {
        return (javax.microedition.khronos.egl.EGLDisplay) resolve(display);
    }

    static javax.microedition.khronos.egl.EGLConfig config(EGLConfig config)
    {
        return (javax.microedition.khronos.egl.EGLConfig) resolve(config);
    }

    static javax.microedition.khronos.egl.EGLContext context(EGLContext context)
    {
        return (javax.microedition.khronos.egl.EGLContext) resolve(context);
    }

    static javax.microedition.khronos.egl.EGLSurface surface(EGLSurface surface)
    {
        return (javax.microedition.khronos.egl.EGLSurface) resolve(surface);
    }

    static EGLDisplay wrap(javax.microedition.khronos.egl.EGLDisplay display)
    {
        return display == null ? EGL14.EGL_NO_DISPLAY : new EGLDisplay(handle(display));
    }

    static EGLConfig wrap(javax.microedition.khronos.egl.EGLConfig config)
    {
        return config == null ? null : new EGLConfig(handle(config));
    }

    static EGLContext wrap(javax.microedition.khronos.egl.EGLContext context)
    {
        return context == null ? EGL14.EGL_NO_CONTEXT : new EGLContext(handle(context));
    }

    static EGLSurface wrap(javax.microedition.khronos.egl.EGLSurface surface)
    {
        return surface == null ? EGL14.EGL_NO_SURFACE : new EGLSurface(handle(surface));
    }

    static javax.microedition.khronos.egl.EGLConfig[] configArray(int size)
    {
        return new javax.microedition.khronos.egl.EGLConfig[size];
    }

    static int[] attributes(int[] list, int offset)
    {
        if (list == null) return null;
        if (offset < 0) throw new IllegalArgumentException("offset < 0");
        if (list.length - offset < 1) throw new IllegalArgumentException("length - offset < 1");

        int length = list.length - offset;
        int[] copy = new int[length];
        System.arraycopy(list, offset, copy, 0, length);
        return copy;
    }

    static void checkOutput(Object array, int offset, int needed, String name)
    {
        if (array == null) throw new IllegalArgumentException(name + " == null");
        if (offset < 0) throw new IllegalArgumentException(name + "Offset < 0");
        if (java.lang.reflect.Array.getLength(array) - offset < needed)
        {
            throw new IllegalArgumentException("length - " + name + "Offset < " + needed + " < needed");
        }
    }
}
