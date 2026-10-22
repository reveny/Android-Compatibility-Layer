package android.view;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class ViewDebug
{
    @Deprecated
    public static final boolean TRACE_HIERARCHY = false;

    @Deprecated
    public static final boolean TRACE_RECYCLER = false;

    public ViewDebug()
    {
    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExportedProperty
    {
        boolean resolveId() default false;

        IntToString[] mapping() default {};

        IntToString[] indexMapping() default {};

        FlagToString[] flagMapping() default {};

        boolean deepExport() default false;

        String prefix() default "";

        String category() default "";

        boolean formatToHexString() default false;

        boolean hasAdjacentMapping() default false;
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IntToString
    {
        int from();

        String to();
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FlagToString
    {
        int mask();

        int equals();

        String name();

        boolean outputIf() default true;
    }

    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CapturedViewProperty
    {
        boolean retrieveReturn() default false;
    }

    @Deprecated
    public enum HierarchyTraceType
    {
        INVALIDATE,
        INVALIDATE_CHILD,
        INVALIDATE_CHILD_IN_PARENT,
        REQUEST_LAYOUT,
        ON_LAYOUT,
        ON_MEASURE,
        DRAW,
        BUILD_CACHE
    }

    @Deprecated
    public enum RecyclerTraceType
    {
        NEW_VIEW,
        BIND_VIEW,
        RECYCLE_FROM_ACTIVE_HEAP,
        RECYCLE_FROM_SCRAP_HEAP,
        MOVE_TO_SCRAP_HEAP,
        MOVE_FROM_ACTIVE_TO_SCRAP_HEAP
    }

    @Deprecated
    public static void trace(View view, RecyclerTraceType type, int... parameters)
    {
    }

    @Deprecated
    public static void startRecyclerTracing(String prefix, View view)
    {
    }

    @Deprecated
    public static void stopRecyclerTracing()
    {
    }

    @Deprecated
    public static void trace(View view, HierarchyTraceType type)
    {
    }

    @Deprecated
    public static void startHierarchyTracing(String prefix, View view)
    {
    }

    @Deprecated
    public static void stopHierarchyTracing()
    {
    }

    public static void dumpCapturedView(String tag, Object view)
    {
        if (view == null) return;

        android.util.Log.d(tag, view.getClass().getName() + ": " + view);
    }
}
