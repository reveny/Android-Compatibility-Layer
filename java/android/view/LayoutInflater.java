package android.view;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import org.xmlpull.v1.XmlPullParser;
import java.lang.reflect.Constructor;
import java.util.HashMap;

public abstract class LayoutInflater
{
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = {Context.class, AttributeSet.class};
    private static final String[] PREFIXES = {"android.widget.", "android.view.", "android.webkit."};

    static final boolean TRACE_INFLATE = runtime.Settings.tracing("inflate");

    private final HashMap<String, Constructor<?>> mConstructors = new HashMap<>();
    protected final Context mContext;
    private Factory mFactory;
    private Factory2 mFactory2;
    private Filter mFilter;

    protected LayoutInflater(Context context)
    {
        mContext = context;
    }

    public static LayoutInflater from(Context context)
    {
        if (context == null) throw new IllegalArgumentException("context must not be null");

        Object service = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (service instanceof LayoutInflater) return (LayoutInflater)service;

        throw new AssertionError("LayoutInflater not found for " + context);
    }

    public abstract LayoutInflater cloneInContext(Context newContext);

    public Context getContext()
    {
        return mContext;
    }

    public final Factory getFactory()
    {
        return mFactory;
    }

    public final Factory2 getFactory2()
    {
        return mFactory2;
    }

    public void setFactory(Factory factory)
    {
        if (mFactory != null) throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        mFactory = factory;
    }

    public void setFactory2(Factory2 factory)
    {
        if (mFactory2 != null) throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        mFactory2 = factory;
        mFactory = factory;
    }

    public Filter getFilter()
    {
        return mFilter;
    }

    public void setFilter(Filter filter)
    {
        mFilter = filter;
    }

    public View inflate(int resourceId, ViewGroup root)
    {
        return inflate(resourceId, root, root != null);
    }

    public View inflate(int resourceId, ViewGroup root, boolean attachToRoot)
    {
        XmlResourceParser parser = mContext.getResources().getLayout(resourceId);
        try
        {
            return inflate(parser, root, attachToRoot);
        }
        finally
        {
            parser.close();
        }
    }

    public View inflate(XmlPullParser parser, ViewGroup root)
    {
        return inflate(parser, root, root != null);
    }

    public View inflate(XmlPullParser parser, ViewGroup root, boolean attachToRoot)
    {
        int event = advance(parser);
        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT)
        {
            event = advance(parser);
        }

        if (event != XmlPullParser.START_TAG)
        {
            throw new InflateException(parser.getPositionDescription() + ": No start tag found");
        }

        String name = parser.getName();

        if ("merge".equals(name))
        {
            if (root == null || !attachToRoot) throw new InflateException("<merge /> can be used only with a valid ViewGroup root");
            inflateChildren(parser, root, parser.getDepth());
            return root;
        }

        AttributeSet attributes = asAttributeSet(parser);
        View created = createViewFromTag(root, name, mContext, attributes);
        ViewGroup.LayoutParams params = root != null ? generateLayoutParams(root, attributes) : null;

        if (root != null && !attachToRoot && params != null) created.setLayoutParams(params);

        inflateChildren(parser, created, parser.getDepth());

        if (root != null && attachToRoot) root.addView(created, params);
        return root != null && attachToRoot ? root : created;
    }

    private static AttributeSet asAttributeSet(XmlPullParser parser)
    {
        if (parser instanceof AttributeSet) return (AttributeSet)parser;
        throw new InflateException(parser.getPositionDescription() + ": inflation requires a compiled resource parser");
    }

    private static int advance(XmlPullParser parser)
    {
        try
        {
            return parser.next();
        }
        catch (org.xmlpull.v1.XmlPullParserException malformed)
        {
            throw new InflateException(parser.getPositionDescription() + ": " + malformed.getMessage(), malformed);
        }
        catch (java.io.IOException unreadable)
        {
            throw new InflateException(parser.getPositionDescription() + ": " + unreadable.getMessage(), unreadable);
        }
    }

    private void inflateChildren(XmlPullParser parser, View parent, int parentDepth)
    {
        readChildren(parser, parent, parentDepth);

        if (parent != null) parent.onFinishInflate();
    }

    private void readChildren(XmlPullParser parser, View parent, int parentDepth)
    {
        for (;;)
        {
            int event = advance(parser);
            if (event == XmlPullParser.END_DOCUMENT) return;

            if (event == XmlPullParser.END_TAG)
            {
                if (parser.getDepth() <= parentDepth) return;
                continue;
            }

            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("requestFocus".equals(name) || "tag".equals(name)) continue;

            if (!(parent instanceof ViewGroup))
            {
                throw new InflateException(parser.getPositionDescription() + ": " + name + " needs a ViewGroup parent");
            }

            ViewGroup group = (ViewGroup)parent;

            if ("include".equals(name))
            {
                inflateInclude(parser, group);
                continue;
            }

            if ("merge".equals(name))
            {
                inflateChildren(parser, group, parser.getDepth());
                continue;
            }

            if ("fragment".equals(name))
            {
                inflateFragment(parser, group);
                continue;
            }

            AttributeSet attributes = asAttributeSet(parser);
            View child = createViewFromTag(group, name, mContext, attributes);
            ViewGroup.LayoutParams params = generateLayoutParams(group, attributes);
            inflateChildren(parser, child, parser.getDepth());
            group.addView(child, params);
        }
    }

    private void inflateFragment(XmlPullParser parser, ViewGroup parent)
    {
        AttributeSet attributes = asAttributeSet(parser);

        String className = attributes.getAttributeValue(ANDROID_NAMESPACE, "name");
        if (className == null) className = attributes.getAttributeValue(null, "class");

        if (className == null)
        {
            throw new InflateException(parser.getPositionDescription() + ": <fragment> needs a name or class");
        }

        int id = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "id", View.NO_ID);
        String tag = attributes.getAttributeValue(ANDROID_NAMESPACE, "tag");

        android.app.Fragment fragment = android.app.Fragment.instantiate(mContext, className);

        fragment.setId(id);
        fragment.setTag(tag);
        fragment.attachTo(activityOf(mContext), mContext);
        fragment.onAttach(mContext);
        fragment.onCreate(null);

        View created = fragment.onCreateView(this, parent, null);

        if (created == null) created = new View(mContext);

        if (id != View.NO_ID && created.getId() == View.NO_ID) created.setId(id);

        fragment.setView(created);

        ViewGroup.LayoutParams params = generateLayoutParams(parent, attributes);
        parent.addView(created, params);

        fragment.onViewCreated(created, null);
        fragment.onActivityCreated(null);

        if (TRACE_INFLATE)
        {
            android.util.Log.i("LayoutInflater", "inflated fragment " + className + " id=0x" + Integer.toHexString(id));
        }
    }

    private static android.app.Activity activityOf(Context context)
    {
        Context cursor = context;

        while (cursor instanceof android.content.ContextWrapper)
        {
            if (cursor instanceof android.app.Activity) return (android.app.Activity)cursor;

            cursor = ((android.content.ContextWrapper)cursor).getBaseContext();
        }

        return cursor instanceof android.app.Activity ? (android.app.Activity)cursor : null;
    }

    private void inflateInclude(XmlPullParser parser, ViewGroup parent)
    {
        AttributeSet attributes = asAttributeSet(parser);

        int layout = attributes.getAttributeResourceValue(null, "layout", 0);
        if (layout == 0) throw new InflateException("You must specify a layout in the include tag");

        int overrideId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "id", View.NO_ID);
        int overrideVisibility = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "visibility", -1);
        boolean overrideLayout = attributes.getAttributeValue(ANDROID_NAMESPACE, "layout_width") != null
                                 || attributes.getAttributeValue(ANDROID_NAMESPACE, "layout_height") != null;

        int before = parent.getChildCount();

        XmlResourceParser included = mContext.getResources().getLayout(layout);
        try
        {
            inflate(included, parent, true);
        }
        finally
        {
            included.close();
        }

        for (int index = before; index < parent.getChildCount(); index++)
        {
            View child = parent.getChildAt(index);
            if (overrideId != View.NO_ID) child.setId(overrideId);
            if (overrideVisibility >= 0 && overrideVisibility <= 2) child.setVisibility(overrideVisibility);
            if (overrideLayout) child.setLayoutParams(parent.generateLayoutParams(attributes));
        }
    }

    private ViewGroup.LayoutParams generateLayoutParams(ViewGroup parent, AttributeSet set)
    {
        return parent.generateLayoutParams(set);
    }

    public final View createView(String name, String prefix, AttributeSet set)
    {
        String qualified = prefix != null ? prefix + name : name;

        try
        {
            Constructor<?> constructor = mConstructors.get(qualified);
            if (constructor == null)
            {
                Class<?> type = mContext.getClassLoader().loadClass(qualified);
                constructor = type.getConstructor(CONSTRUCTOR_SIGNATURE);
                constructor.setAccessible(true);
                mConstructors.put(qualified, constructor);
            }

            View created = (View)constructor.newInstance(mContext, set);

            if (TRACE_INFLATE)
            {
                android.util.Log.i("LayoutInflater", "inflated " + qualified + " id=0x"
                                                         + Integer.toHexString(created.getId()));
            }

            return created;
        }
        catch (ClassNotFoundException absent)
        {
            return null;
        }
        catch (Exception cause)
        {
            throw new InflateException("Could not inflate " + qualified, cause);
        }
    }

    protected View onCreateView(String name, AttributeSet set)
    {
        for (String prefix : PREFIXES)
        {
            View created = createView(name, prefix, set);
            if (created != null) return created;
        }
        return null;
    }

    protected View onCreateView(View parent, String name, AttributeSet set)
    {
        return onCreateView(name, set);
    }

    private View createViewFromTag(View parent, String name, Context context, AttributeSet set)
    {
        if ("view".equals(name)) name = set.getAttributeValue(null, "class");

        if (mFactory2 != null)
        {
            View created = mFactory2.onCreateView(parent, name, context, set);
            if (created != null) return created;
        }
        else if (mFactory != null)
        {
            View created = mFactory.onCreateView(name, context, set);
            if (created != null) return created;
        }

        View created = name.indexOf('.') < 0 ? onCreateView(parent, name, set) : createView(name, null, set);
        if (created != null) return created;

        throw new InflateException(set.getPositionDescription() + ": Class not found: " + name);
    }

    public interface Factory
    {
        View onCreateView(String name, Context context, AttributeSet set);
    }

    public interface Factory2 extends Factory
    {
        View onCreateView(View parent, String name, Context context, AttributeSet set);
    }

    public interface Filter
    {
        boolean onLoadClass(Class<?> type);
    }
}
