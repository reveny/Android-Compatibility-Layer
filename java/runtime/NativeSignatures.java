package runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public final class NativeSignatures
{
    private NativeSignatures()
    {
    }

    public static String[] describe(String className)
    {
        Class<?> subject;

        String binaryName = className.replace('/', '.');

        try
        {
            subject = Class.forName(binaryName, false, NativeSignatures.class.getClassLoader());
        }
        catch (Throwable missing)
        {
            try
            {
                subject = Class.forName(binaryName, false, Thread.currentThread().getContextClassLoader());
            }
            catch (Throwable stillMissing)
            {
                return new String[0];
            }
        }

        ArrayList<String> described = new ArrayList<>();

        for (Method method : subject.getDeclaredMethods())
        {
            if (!Modifier.isNative(method.getModifiers())) continue;

            described.add(method.getName());
            described.add(signatureOf(method));
        }

        return described.toArray(new String[0]);
    }

    private static String signatureOf(Method method)
    {
        StringBuilder built = new StringBuilder("(");

        for (Class<?> parameter : method.getParameterTypes()) appendDescriptor(built, parameter);

        built.append(')');
        appendDescriptor(built, method.getReturnType());
        return built.toString();
    }

    private static void appendDescriptor(StringBuilder out, Class<?> type)
    {
        if (type.isArray())
        {
            out.append('[');
            appendDescriptor(out, type.getComponentType());
            return;
        }

        if (!type.isPrimitive())
        {
            out.append('L').append(type.getName().replace('.', '/')).append(';');
            return;
        }

        if (type == void.class) out.append('V');
        else if (type == boolean.class) out.append('Z');
        else if (type == byte.class) out.append('B');
        else if (type == char.class) out.append('C');
        else if (type == short.class) out.append('S');
        else if (type == int.class) out.append('I');
        else if (type == long.class) out.append('J');
        else if (type == float.class) out.append('F');
        else out.append('D');
    }
}
