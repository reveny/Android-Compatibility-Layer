package android.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class RuntimeGles
{
    static final int FRAMEBUFFER_COMPLETE = 0x8CD5;
    static final int ALREADY_SIGNALED = 0x911A;

    private static final int VENDOR = 0x1F00;
    private static final int RENDERER = 0x1F01;
    private static final int VERSION = 0x1F02;
    private static final int EXTENSIONS = 0x1F03;
    private static final int SHADING_LANGUAGE_VERSION = 0x8B8C;

    private static final int VIEWPORT = 0x0BA2;
    private static final int SCISSOR_BOX = 0x0C10;
    private static final int COLOR_CLEAR_VALUE = 0x0C22;
    private static final int ARRAY_BUFFER = 0x8892;
    private static final int ELEMENT_ARRAY_BUFFER = 0x8893;
    private static final int ARRAY_BUFFER_BINDING = 0x8894;
    private static final int ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;
    private static final int FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int READ_FRAMEBUFFER = 0x8CA8;
    private static final int READ_FRAMEBUFFER_BINDING = 0x8CAA;
    private static final int RENDERBUFFER_BINDING = 0x8CA7;
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int TEXTURE_CUBE_MAP = 0x8513;
    private static final int TEXTURE_BINDING_2D = 0x8069;
    private static final int TEXTURE_BINDING_CUBE_MAP = 0x8514;
    private static final int CURRENT_PROGRAM = 0x8B8D;
    private static final int ACTIVE_TEXTURE = 0x84E0;
    private static final int TEXTURE0 = 0x84C0;
    private static final int VERTEX_ARRAY_BINDING = 0x85B5;
    private static final int MAX_SERVER_WAIT_TIMEOUT = 0x9111;

    private static final int SHADER_TYPE = 0x8B4F;
    private static final int COMPILE_STATUS = 0x8B81;
    private static final int LINK_STATUS = 0x8B82;
    private static final int VALIDATE_STATUS = 0x8B83;
    private static final int SHADER_SOURCE_LENGTH = 0x8B88;

    private static final int LOW_FLOAT = 0x8DF0;
    private static final int MEDIUM_FLOAT = 0x8DF1;
    private static final int HIGH_FLOAT = 0x8DF2;

    private static final Map<Integer, int[]> sIntegers = new HashMap<>();
    private static final Map<Integer, float[]> sFloats = new HashMap<>();
    private static final Map<Integer, Integer> sShaderTypes = new HashMap<>();
    private static final Map<Integer, String> sShaderSources = new HashMap<>();
    private static final Map<Integer, Map<String, Integer>> sLocations = new HashMap<>();
    private static final Set<Integer> sEnabled = new HashSet<>();

    private static int sNextName = 1;

    static
    {
        String[] version = runtime.PlatformConfig.GL_ES_VERSION.split("\\.");
        int major = parse(version, 0, 3);
        int minor = parse(version, 1, 0);

        put(0x0D33, 8192);
        put(0x0D3A, 8192, 8192);
        put(0x8869, 16);
        put(0x8DFB, 256);
        put(0x8DFC, 16);
        put(0x8B4D, 48);
        put(0x8B4C, 16);
        put(0x8872, 16);
        put(0x8DFD, 256);
        put(0x851C, 8192);
        put(0x84E8, 8192);
        put(0x0D50, 4);
        put(0x0D52, 8);
        put(0x0D53, 8);
        put(0x0D54, 8);
        put(0x0D55, 8);
        put(0x0D56, 24);
        put(0x0D57, 8);
        put(0x86A2, 0);
        put(0x8DF9, 0);
        put(0x87FE, 0);
        put(0x821B, major);
        put(0x821C, minor);
        put(0x821D, 0);
        put(0x8073, 2048);
        put(0x88FF, 256);
        put(0x8824, 8);
        put(0x8CDF, 8);
        put(0x8D57, 4);
        put(0x80A9, 0);
        put(0x80A8, 0);
        put(0x8A2F, 72);
        put(0x8A30, 65536);
        put(0x8A34, 256);
        put(0x8A2B, 14);
        put(0x8A2D, 14);
        put(0x8A2E, 28);
        put(0x8B4A, 1024);
        put(0x8B49, 1024);
        put(0x9122, 64);
        put(0x9125, 128);
        put(0x80E8, 1048576);
        put(0x80E9, 1048576);
        put(0x84FD, 16);
        put(0x8C8A, 64);
        put(0x8C8B, 4);
        put(0x8C80, 4);
        put(0x8B9A, 0x1401);
        put(0x8B9B, 0x1908);
        put(0x0D05, 4);
        put(0x0CF5, 4);
        put(0x0D32, 8);
        put(0x91BE, 65535, 65535, 65535);
        put(0x91BF, 1024, 1024, 64);
        put(0x90EB, 1024);
        put(VIEWPORT, 0, 0, 0, 0);
        put(SCISSOR_BOX, 0, 0, 0, 0);
        put(ACTIVE_TEXTURE, TEXTURE0);

        sFloats.put(0x846D, new float[] {1.0f, 1024.0f});
        sFloats.put(0x846E, new float[] {1.0f, 8.0f});
        sFloats.put(0x84FF, new float[] {16.0f});
        sFloats.put(0x84FD, new float[] {16.0f});
        sFloats.put(COLOR_CLEAR_VALUE, new float[] {0.0f, 0.0f, 0.0f, 0.0f});
        sFloats.put(0x0B70, new float[] {0.0f, 1.0f});
        sFloats.put(0x0B21, new float[] {1.0f});

        sEnabled.add(0x0BD0);
    }

    private RuntimeGles()
    {
    }

    private static int parse(String[] parts, int index, int fallback)
    {
        if (index >= parts.length) return fallback;

        try
        {
            return Integer.parseInt(parts[index].trim());
        }
        catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static void put(int pname, int... values)
    {
        sIntegers.put(pname, values);
    }

    static synchronized int nextName()
    {
        return sNextName++;
    }

    static synchronized int createShader(int type)
    {
        int name = sNextName++;
        sShaderTypes.put(name, type);
        return name;
    }

    static synchronized void forget(int name)
    {
        sShaderTypes.remove(name);
        sShaderSources.remove(name);
        sLocations.remove(name);
    }

    static synchronized void generateNames(int count, int[] names, int offset)
    {
        checkArray(names, offset, count);
        for (int index = 0; index < count; index++) names[offset + index] = sNextName++;
    }

    static synchronized void generateNames(int count, IntBuffer names)
    {
        if (names == null) throw new IllegalArgumentException("buffer == null");
        if (names.remaining() < count) throw new IllegalArgumentException("remaining() < n < needed");

        int position = names.position();
        for (int index = 0; index < count; index++) names.put(position + index, sNextName++);
    }

    private static void checkArray(Object array, int offset, int count)
    {
        if (array == null) throw new IllegalArgumentException("array == null");
        if (offset < 0) throw new IllegalArgumentException("offset < 0");
        if (java.lang.reflect.Array.getLength(array) - offset < count) throw new IllegalArgumentException("length - offset < n < needed");
    }

    static String getString(int name)
    {
        switch (name)
        {
            case VENDOR:
                return runtime.PlatformConfig.GPU_VENDOR;
            case RENDERER:
                return runtime.PlatformConfig.GPU_RENDERER;
            case VERSION:
                return "OpenGL ES " + runtime.PlatformConfig.GL_ES_VERSION;
            case SHADING_LANGUAGE_VERSION:
                return "OpenGL ES GLSL ES " + shadingLanguageVersion();
            case EXTENSIONS:
                return "";
            default:
                return null;
        }
    }

    static String getString(int name, int index)
    {
        return null;
    }

    private static String shadingLanguageVersion()
    {
        String[] version = runtime.PlatformConfig.GL_ES_VERSION.split("\\.");
        int major = parse(version, 0, 3);
        int minor = parse(version, 1, 0);
        if (major < 3) return "1.00";
        return major + "." + minor + "0";
    }

    static synchronized void getIntegers(int pname, int[] params, int offset)
    {
        if (params == null) throw new IllegalArgumentException("params == null");
        if (offset < 0) throw new IllegalArgumentException("offset < 0");

        int[] values = integers(pname);
        int count = Math.min(values.length, params.length - offset);
        System.arraycopy(values, 0, params, offset, Math.max(count, 0));
    }

    static synchronized void getIntegers(int pname, IntBuffer params)
    {
        if (params == null) throw new IllegalArgumentException("params == null");

        int[] values = integers(pname);
        int position = params.position();
        int count = Math.min(values.length, params.remaining());
        for (int index = 0; index < count; index++) params.put(position + index, values[index]);
    }

    static synchronized void getFloats(int pname, float[] params, int offset)
    {
        if (params == null) throw new IllegalArgumentException("params == null");
        if (offset < 0) throw new IllegalArgumentException("offset < 0");

        float[] values = floats(pname);
        int count = Math.min(values.length, params.length - offset);
        System.arraycopy(values, 0, params, offset, Math.max(count, 0));
    }

    static synchronized void getFloats(int pname, FloatBuffer params)
    {
        if (params == null) throw new IllegalArgumentException("params == null");

        float[] values = floats(pname);
        int position = params.position();
        int count = Math.min(values.length, params.remaining());
        for (int index = 0; index < count; index++) params.put(position + index, values[index]);
    }

    static synchronized void getBooleans(int pname, boolean[] params, int offset)
    {
        if (params == null) throw new IllegalArgumentException("params == null");
        if (offset < 0) throw new IllegalArgumentException("offset < 0");

        if (!sIntegers.containsKey(pname) && !sFloats.containsKey(pname))
        {
            if (offset < params.length) params[offset] = sEnabled.contains(pname);
            return;
        }

        int[] values = integers(pname);
        int count = Math.min(values.length, params.length - offset);
        for (int index = 0; index < count; index++) params[offset + index] = values[index] != 0;
    }

    static synchronized void getLongs(int pname, long[] params, int offset)
    {
        if (params == null) throw new IllegalArgumentException("params == null");
        if (offset < 0) throw new IllegalArgumentException("offset < 0");

        long[] values = longs(pname);
        int count = Math.min(values.length, params.length - offset);
        System.arraycopy(values, 0, params, offset, Math.max(count, 0));
    }

    static synchronized void getLongs(int pname, LongBuffer params)
    {
        if (params == null) throw new IllegalArgumentException("params == null");

        long[] values = longs(pname);
        int position = params.position();
        int count = Math.min(values.length, params.remaining());
        for (int index = 0; index < count; index++) params.put(position + index, values[index]);
    }

    private static int[] integers(int pname)
    {
        int[] values = sIntegers.get(pname);
        if (values != null) return values;

        float[] floats = sFloats.get(pname);
        if (floats != null)
        {
            int[] converted = new int[floats.length];
            for (int index = 0; index < floats.length; index++) converted[index] = Math.round(floats[index]);
            return converted;
        }

        return new int[] {sEnabled.contains(pname) ? 1 : 0};
    }

    private static float[] floats(int pname)
    {
        float[] values = sFloats.get(pname);
        if (values != null) return values;

        int[] integers = integers(pname);
        float[] converted = new float[integers.length];
        for (int index = 0; index < integers.length; index++) converted[index] = integers[index];
        return converted;
    }

    private static long[] longs(int pname)
    {
        if (pname == MAX_SERVER_WAIT_TIMEOUT) return new long[] {Long.MAX_VALUE};

        int[] integers = integers(pname);
        long[] converted = new long[integers.length];
        for (int index = 0; index < integers.length; index++) converted[index] = integers[index];
        return converted;
    }

    static synchronized int getShaderParameter(int shader, int pname)
    {
        switch (pname)
        {
            case SHADER_TYPE:
                Integer type = sShaderTypes.get(shader);
                return type == null ? 0 : type;
            case COMPILE_STATUS:
                return 1;
            case SHADER_SOURCE_LENGTH:
                String source = sShaderSources.get(shader);
                return source == null ? 0 : source.length() + 1;
            default:
                return 0;
        }
    }

    static int getProgramParameter(int program, int pname)
    {
        switch (pname)
        {
            case LINK_STATUS:
            case VALIDATE_STATUS:
                return 1;
            default:
                return 0;
        }
    }

    static synchronized void setShaderSource(int shader, String source)
    {
        sShaderSources.put(shader, source);
    }

    static synchronized String getShaderSource(int shader)
    {
        String source = sShaderSources.get(shader);
        return source == null ? "" : source;
    }

    static synchronized int location(int program, String name)
    {
        if (name == null || name.startsWith("gl_")) return -1;

        Map<String, Integer> locations = sLocations.get(program);
        if (locations == null)
        {
            locations = new HashMap<>();
            sLocations.put(program, locations);
        }

        Integer location = locations.get(name);
        if (location == null)
        {
            location = locations.size();
            locations.put(name, location);
        }

        return location;
    }

    static synchronized boolean isEnabled(int capability)
    {
        return sEnabled.contains(capability);
    }

    static synchronized void setEnabled(int capability, boolean enabled)
    {
        if (enabled) sEnabled.add(capability);
        else sEnabled.remove(capability);
    }

    static synchronized void setViewport(int x, int y, int width, int height)
    {
        put(VIEWPORT, x, y, width, height);
    }

    static synchronized void setScissor(int x, int y, int width, int height)
    {
        put(SCISSOR_BOX, x, y, width, height);
    }

    static synchronized void setClearColor(float red, float green, float blue, float alpha)
    {
        sFloats.put(COLOR_CLEAR_VALUE, new float[] {red, green, blue, alpha});
    }

    static synchronized void setClearColor(int red, int green, int blue, int alpha)
    {
        setClearColor(red / 65536.0f, green / 65536.0f, blue / 65536.0f, alpha / 65536.0f);
    }

    static synchronized void bindBuffer(int target, int buffer)
    {
        if (target == ARRAY_BUFFER) put(ARRAY_BUFFER_BINDING, buffer);
        else if (target == ELEMENT_ARRAY_BUFFER) put(ELEMENT_ARRAY_BUFFER_BINDING, buffer);
    }

    static synchronized void bindFramebuffer(int target, int framebuffer)
    {
        if (target != READ_FRAMEBUFFER) put(FRAMEBUFFER_BINDING, framebuffer);
        put(READ_FRAMEBUFFER_BINDING, framebuffer);
    }

    static synchronized void bindRenderbuffer(int target, int renderbuffer)
    {
        put(RENDERBUFFER_BINDING, renderbuffer);
    }

    static synchronized void bindTexture(int target, int texture)
    {
        if (target == TEXTURE_2D) put(TEXTURE_BINDING_2D, texture);
        else if (target == TEXTURE_CUBE_MAP) put(TEXTURE_BINDING_CUBE_MAP, texture);
    }

    static synchronized void useProgram(int program)
    {
        put(CURRENT_PROGRAM, program);
    }

    static synchronized void activeTexture(int texture)
    {
        put(ACTIVE_TEXTURE, texture);
    }

    static synchronized void bindVertexArray(int array)
    {
        put(VERTEX_ARRAY_BINDING, array);
    }

    static Buffer mapBuffer(int length)
    {
        return ByteBuffer.allocateDirect(Math.max(length, 0)).order(ByteOrder.nativeOrder());
    }

    static void getShaderPrecisionFormat(int precisionType, int[] range, int rangeOffset, int[] precision, int precisionOffset)
    {
        boolean floating = precisionType == LOW_FLOAT || precisionType == MEDIUM_FLOAT || precisionType == HIGH_FLOAT;
        range[rangeOffset] = floating ? 127 : 31;
        range[rangeOffset + 1] = floating ? 127 : 30;
        precision[precisionOffset] = floating ? 23 : 0;
    }

    static void getShaderPrecisionFormat(int precisionType, IntBuffer range, IntBuffer precision)
    {
        int[] rangeValues = new int[2];
        int[] precisionValues = new int[1];
        getShaderPrecisionFormat(precisionType, rangeValues, 0, precisionValues, 0);
        range.put(range.position(), rangeValues[0]);
        range.put(range.position() + 1, rangeValues[1]);
        precision.put(precision.position(), precisionValues[0]);
    }
}
