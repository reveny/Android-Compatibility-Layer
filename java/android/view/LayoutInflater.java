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
    private static final HashMap<String, Constructor<? extends View>> sConstructorMap = new HashMap<>();
    private static final int[] INCLUDE_VISIBILITIES = {View.VISIBLE, View.INVISIBLE, View.GONE};

    static final boolean TRACE_INFLATE = runtime.Settings.tracing("inflate");

    protected final Context mContext;
    private Factory mFactory;
    private Factory2 mFactory2;
    private Factory2 mPrivateFactory;
    private Filter mFilter;
    private boolean mFactorySet;
    private HashMap<String, Boolean> mFilterMap;

    protected LayoutInflater(Context context)
    {
        mContext = context;
    }

    protected LayoutInflater(LayoutInflater original, Context newContext)
    {
        mContext = newContext;
        mFactory = original.mFactory;
        mFactory2 = original.mFactory2;
        mPrivateFactory = original.mPrivateFactory;
        setFilter(original.mFilter);
    }

    public static LayoutInflater from(Context context)
    {
        if (context == null) throw new IllegalArgumentException("context must not be null");

        Object service = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (service instanceof LayoutInflater) return (LayoutInflater)service;

        throw new AssertionError("LayoutInflater not found.");
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
        if (mFactorySet) throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        if (factory == null) throw new NullPointerException("Given factory can not be null");

        mFactorySet = true;
        if (mFactory == null) mFactory = factory;
        else mFactory = new FactoryMerger(factory, null, mFactory, mFactory2);
    }

    public void setFactory2(Factory2 factory)
    {
        if (mFactorySet) throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        if (factory == null) throw new NullPointerException("Given factory can not be null");

        mFactorySet = true;

        if (mFactory == null)
        {
            mFactory = factory;
            mFactory2 = factory;
            return;
        }

        FactoryMerger merger = new FactoryMerger(factory, factory, mFactory, mFactory2);
        mFactory = merger;
        mFactory2 = merger;
    }

    public void setPrivateFactory(Factory2 factory)
    {
        if (mPrivateFactory == null) mPrivateFactory = factory;
        else mPrivateFactory = new FactoryMerger(factory, factory, mPrivateFactory, mPrivateFactory);
    }

    public Filter getFilter()
    {
        return mFilter;
    }

    public void setFilter(Filter filter)
    {
        mFilter = filter;
        if (filter != null) mFilterMap = new HashMap<>();
    }

    public View inflate(int resource, ViewGroup root)
    {
        return inflate(resource, root, root != null);
    }

    public View inflate(int resource, ViewGroup root, boolean attachToRoot)
    {
        XmlResourceParser parser = getContext().getResources().getLayout(resource);
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
            throw new InflateException(parser.getPositionDescription() + ": No start tag found!");
        }

        String name = parser.getName();
        Context inflaterContext = mContext;

        if ("merge".equals(name))
        {
            if (root == null || !attachToRoot)
            {
                throw new InflateException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
            }

            readChildren(parser, root, inflaterContext, parser.getDepth());
            root.onFinishInflate();
            return root;
        }

        AttributeSet attributes = asAttributeSet(parser);
        View created = createViewFromTag(root, name, inflaterContext, attributes, false);
        ViewGroup.LayoutParams params = root != null ? root.generateLayoutParams(attributes) : null;

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
        readChildren(parser, parent, parent.getContext(), parentDepth);

        parent.onFinishInflate();
    }

    private void readChildren(XmlPullParser parser, View parent, Context context, int parentDepth)
    {
        boolean pendingRequestFocus = false;

        for (;;)
        {
            int event = advance(parser);
            if (event == XmlPullParser.END_DOCUMENT) break;

            if (event == XmlPullParser.END_TAG)
            {
                if (parser.getDepth() <= parentDepth) break;
                continue;
            }

            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("requestFocus".equals(name))
            {
                pendingRequestFocus = true;
                skipTag(parser);
                continue;
            }

            if ("tag".equals(name))
            {
                parseViewTag(parser, parent);
                continue;
            }

            if (!(parent instanceof ViewGroup))
            {
                throw new InflateException(parser.getPositionDescription() + ": " + name + " needs a ViewGroup parent");
            }

            ViewGroup group = (ViewGroup)parent;

            if ("include".equals(name))
            {
                if (parser.getDepth() == 0) throw new InflateException("<include /> cannot be the root element");

                inflateInclude(parser, group, context);
                continue;
            }

            if ("merge".equals(name)) throw new InflateException("<merge /> must be the root element");

            if ("fragment".equals(name))
            {
                inflateFragment(parser, group);
                continue;
            }

            AttributeSet attributes = asAttributeSet(parser);
            View child = createViewFromTag(group, name, context, attributes, false);
            ViewGroup.LayoutParams params = group.generateLayoutParams(attributes);
            inflateChildren(parser, child, parser.getDepth());
            group.addView(child, params);
        }

        if (pendingRequestFocus) parent.restoreDefaultFocus();
    }

    private static void skipTag(XmlPullParser parser)
    {
        int depth = parser.getDepth();

        for (;;)
        {
            int event = advance(parser);
            if (event == XmlPullParser.END_DOCUMENT) return;
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) return;
        }
    }

    private void parseViewTag(XmlPullParser parser, View view)
    {
        AttributeSet attributes = asAttributeSet(parser);

        int key = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "id", 0);
        String value = attributes.getAttributeValue(ANDROID_NAMESPACE, "value");
        if (key != 0) view.setTag(key, value);

        skipTag(parser);
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

        ViewGroup.LayoutParams params = parent.generateLayoutParams(attributes);
        parent.addView(created, params);

        fragment.onViewCreated(created, null);
        fragment.onActivityCreated(null);

        skipTag(parser);

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

    private void inflateInclude(XmlPullParser parser, ViewGroup parent, Context context)
    {
        AttributeSet attributes = asAttributeSet(parser);

        int layout = attributes.getAttributeResourceValue(null, "layout", 0);
        if (layout == 0)
        {
            String value = attributes.getAttributeValue(null, "layout");
            if (value == null || value.isEmpty())
            {
                throw new InflateException("You must specify a layout in the include tag: <include layout=\"@layout/layoutID\" />");
            }

            throw new InflateException("You must specify a valid layout reference. The layout ID " + value + " is not valid.");
        }

        int themeResource = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "theme", 0);
        if (themeResource != 0) context = new ContextThemeWrapper(context, themeResource);

        int overrideId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "id", View.NO_ID);
        int overrideVisibility = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "visibility", -1);
        boolean overrideLayout = attributes.getAttributeValue(ANDROID_NAMESPACE, "layout_width") != null
                                 || attributes.getAttributeValue(ANDROID_NAMESPACE, "layout_height") != null;

        int before = parent.getChildCount();

        XmlResourceParser included = context.getResources().getLayout(layout);
        try
        {
            inflateIncluded(included, parent, context);
        }
        finally
        {
            included.close();
        }

        for (int index = before; index < parent.getChildCount(); index++)
        {
            View child = parent.getChildAt(index);
            if (overrideId != View.NO_ID) child.setId(overrideId);
            if (overrideVisibility >= 0 && overrideVisibility <= 2) child.setVisibility(INCLUDE_VISIBILITIES[overrideVisibility]);
            if (overrideLayout)
            {
                try
                {
                    child.setLayoutParams(parent.generateLayoutParams(attributes));
                }
                catch (RuntimeException incomplete)
                {
                    if (TRACE_INFLATE) android.util.Log.i("LayoutInflater", "the include tag's layout params were incomplete", incomplete);
                }
            }
        }

        skipTag(parser);
    }

    private void inflateIncluded(XmlPullParser parser, ViewGroup parent, Context context)
    {
        int event = advance(parser);
        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT)
        {
            event = advance(parser);
        }

        if (event != XmlPullParser.START_TAG) throw new InflateException(parser.getPositionDescription() + ": No start tag found!");

        String name = parser.getName();

        if ("merge".equals(name))
        {
            readChildren(parser, parent, context, parser.getDepth());
            return;
        }

        AttributeSet attributes = asAttributeSet(parser);
        View created = createViewFromTag(parent, name, context, attributes, false);
        ViewGroup.LayoutParams params = parent.generateLayoutParams(attributes);
        inflateChildren(parser, created, parser.getDepth());
        parent.addView(created, params);
    }

    public final View createView(Context viewContext, String name, String prefix, AttributeSet attrs)
        throws ClassNotFoundException, InflateException
    {
        if (viewContext == null) throw new NullPointerException("viewContext");
        if (name == null) throw new NullPointerException("name");

        String qualified = prefix != null ? prefix + name : name;
        Constructor<? extends View> constructor;

        synchronized (sConstructorMap)
        {
            constructor = sConstructorMap.get(qualified);
        }

        if (constructor != null && !verifyClassLoader(constructor))
        {
            constructor = null;

            synchronized (sConstructorMap)
            {
                sConstructorMap.remove(qualified);
            }
        }

        Class<? extends View> type = null;

        try
        {
            if (constructor == null)
            {
                type = Class.forName(qualified, false, mContext.getClassLoader()).asSubclass(View.class);

                if (mFilter != null && !mFilter.onLoadClass(type)) failNotAllowed(name, prefix, viewContext, attrs);

                constructor = type.getConstructor(CONSTRUCTOR_SIGNATURE);
                constructor.setAccessible(true);

                synchronized (sConstructorMap)
                {
                    sConstructorMap.put(qualified, constructor);
                }
            }
            else if (mFilter != null)
            {
                Boolean allowed = mFilterMap.get(qualified);

                if (allowed == null)
                {
                    type = Class.forName(qualified, false, mContext.getClassLoader()).asSubclass(View.class);
                    allowed = mFilter.onLoadClass(type);
                    mFilterMap.put(qualified, allowed);
                }

                if (!allowed) failNotAllowed(name, prefix, viewContext, attrs);
            }

            View created = constructor.newInstance(viewContext, attrs);
            if (created instanceof ViewStub) ((ViewStub)created).setLayoutInflater(cloneInContext(viewContext));

            if (TRACE_INFLATE)
            {
                android.util.Log.i("LayoutInflater", "inflated " + qualified + " id=0x" + Integer.toHexString(created.getId()));
            }

            return created;
        }
        catch (NoSuchMethodException missing)
        {
            throw new InflateException(attrs.getPositionDescription() + ": Error inflating class " + qualified, missing);
        }
        catch (ClassCastException notView)
        {
            throw new InflateException(attrs.getPositionDescription() + ": Class is not a View " + qualified, notView);
        }
        catch (ClassNotFoundException absent)
        {
            throw absent;
        }
        catch (java.lang.reflect.InvocationTargetException failed)
        {
            Throwable cause = failed.getCause() == null ? failed : failed.getCause();
            throw new InflateException(attrs.getPositionDescription() + ": Error inflating class " + qualified, cause);
        }
        catch (InflateException rethrown)
        {
            throw rethrown;
        }
        catch (Exception failed)
        {
            throw new InflateException(attrs.getPositionDescription() + ": Error inflating class " + qualified, failed);
        }
    }

    public final View createView(String name, String prefix, AttributeSet attrs) throws ClassNotFoundException, InflateException
    {
        Context context = mCurrentContext != null ? mCurrentContext : mContext;
        return createView(context, name, prefix, attrs);
    }

    private Context mCurrentContext;

    private boolean verifyClassLoader(Constructor<? extends View> constructor)
    {
        ClassLoader constructorLoader = constructor.getDeclaringClass().getClassLoader();
        if (constructorLoader == LayoutInflater.class.getClassLoader()) return true;

        ClassLoader cursor = mContext.getClassLoader();
        while (cursor != null)
        {
            if (constructorLoader == cursor) return true;
            cursor = cursor.getParent();
        }

        return false;
    }

    private void failNotAllowed(String name, String prefix, Context context, AttributeSet attrs)
    {
        throw new InflateException(attrs.getPositionDescription() + ": Class not allowed to be inflated "
                                   + (prefix != null ? prefix + name : name));
    }

    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException
    {
        return createView(name, "android.view.", attrs);
    }

    protected View onCreateView(View parent, String name, AttributeSet attrs) throws ClassNotFoundException
    {
        return onCreateView(name, attrs);
    }

    public View onCreateView(Context viewContext, View parent, String name, AttributeSet attrs) throws ClassNotFoundException
    {
        return onCreateView(parent, name, attrs);
    }

    private View createViewFromTag(View parent, String name, Context context, AttributeSet attrs, boolean ignoreThemeAttr)
    {
        if ("view".equals(name)) name = attrs.getAttributeValue(null, "class");
        if (name == null) throw new InflateException(attrs.getPositionDescription() + ": <view> needs a class attribute");

        if (!ignoreThemeAttr)
        {
            int themeResource = attrs.getAttributeResourceValue(ANDROID_NAMESPACE, "theme", 0);
            if (themeResource != 0) context = new ContextThemeWrapper(context, themeResource);
        }

        View created = tryCreateView(parent, name, context, attrs);
        if (created != null) return created;

        Context previous = mCurrentContext;
        mCurrentContext = context;

        try
        {
            if (name.indexOf('.') < 0) created = onCreateView(context, parent, name, attrs);
            else created = createView(context, name, null, attrs);
        }
        catch (ClassNotFoundException absent)
        {
            throw new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name, absent);
        }
        finally
        {
            mCurrentContext = previous;
        }

        if (created == null) throw new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);

        return created;
    }

    private View tryCreateView(View parent, String name, Context context, AttributeSet attrs)
    {
        View created = null;

        if (mFactory2 != null) created = mFactory2.onCreateView(parent, name, context, attrs);
        else if (mFactory != null) created = mFactory.onCreateView(name, context, attrs);

        if (created == null && mPrivateFactory != null) created = mPrivateFactory.onCreateView(parent, name, context, attrs);

        return created;
    }

    public interface Factory
    {
        View onCreateView(String name, Context context, AttributeSet attrs);
    }

    public interface Factory2 extends Factory
    {
        View onCreateView(View parent, String name, Context context, AttributeSet attrs);
    }

    public interface Filter
    {
        boolean onLoadClass(Class clazz);
    }

    private static final class FactoryMerger implements Factory2
    {
        private final Factory mFirst;
        private final Factory mSecond;
        private final Factory2 mFirst2;
        private final Factory2 mSecond2;

        FactoryMerger(Factory first, Factory2 first2, Factory second, Factory2 second2)
        {
            mFirst = first;
            mSecond = second;
            mFirst2 = first2;
            mSecond2 = second2;
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs)
        {
            View created = mFirst.onCreateView(name, context, attrs);
            if (created != null) return created;

            return mSecond.onCreateView(name, context, attrs);
        }

        @Override
        public View onCreateView(View parent, String name, Context context, AttributeSet attrs)
        {
            View created = mFirst2 != null ? mFirst2.onCreateView(parent, name, context, attrs) : mFirst.onCreateView(name, context, attrs);
            if (created != null) return created;

            return mSecond2 != null ? mSecond2.onCreateView(parent, name, context, attrs) : mSecond.onCreateView(name, context, attrs);
        }
    }
}
