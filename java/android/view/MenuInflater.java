package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlBlock;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import org.xmlpull.v1.XmlPullParser;

public class MenuInflater
{
    private static final String TAG = "MenuInflater";
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String[] SHOW_AS_ACTION_LABELS = {"never", "ifRoom", "always", "withText", "collapseActionView"};

    private final Context mContext;

    public MenuInflater(Context context)
    {
        mContext = context;
    }

    public void inflate(int menuRes, Menu menu)
    {
        XmlResourceParser parser = null;

        try
        {
            parser = mContext.getResources().getLayout(menuRes);
            parseMenu(parser, parser, menu);
        }
        catch (Exception failure)
        {
            throw new InflateException("Error inflating menu XML", failure);
        }
        finally
        {
            if (parser != null) parser.close();
        }
    }

    private void parseMenu(XmlPullParser parser, AttributeSet attributes, Menu menu) throws Exception
    {
        int event = parser.getEventType();
        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) event = parser.next();

        if (event == XmlPullParser.END_DOCUMENT || !"menu".equals(parser.getName()))
        {
            throw new IllegalArgumentException("Expecting menu, got " + parser.getName());
        }

        parseChildren(parser, attributes, menu, new GroupState());
    }

    private static final class GroupState
    {
        int id = Menu.NONE;
        int category;
        int order;
        int checkable;
        boolean visible = true;
        boolean enabled = true;
    }

    private void parseChildren(XmlPullParser parser, AttributeSet attributes, Menu menu, GroupState group) throws Exception
    {
        int depth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) return;
            if (event != XmlPullParser.START_TAG) continue;

            String tag = parser.getName();

            if ("group".equals(tag))
            {
                GroupState nested = new GroupState();
                nested.id = reference(attributes, "id", Menu.NONE);
                nested.category = integer(attributes, "menuCategory", 0);
                nested.order = integer(attributes, "orderInCategory", 0);
                nested.checkable = integer(attributes, "checkableBehavior", 0);
                nested.visible = flag(attributes, "visible", true);
                nested.enabled = flag(attributes, "enabled", true);

                parseChildren(parser, attributes, menu, nested);
                continue;
            }

            if ("item".equals(tag))
            {
                parseItem(parser, attributes, menu, group);
                continue;
            }

            skip(parser);
        }
    }

    private void parseItem(XmlPullParser parser, AttributeSet attributes, Menu menu, GroupState group) throws Exception
    {
        int id = reference(attributes, "id", Menu.NONE);
        int category = integer(attributes, "menuCategory", group.category) & Menu.CATEGORY_MASK;
        int order = integer(attributes, "orderInCategory", group.order) | category;
        CharSequence title = text(attributes, "title");
        CharSequence condensed = text(attributes, "titleCondensed");
        int icon = reference(attributes, "icon", 0);
        int checkable = attributes.getAttributeValue(ANDROID_NAMESPACE, "checkable") != null
                            ? (flag(attributes, "checkable", false) ? 1 : 0)
                            : group.checkable;
        boolean checked = flag(attributes, "checked", false);
        boolean visible = flag(attributes, "visible", group.visible);
        boolean enabled = flag(attributes, "enabled", group.enabled);
        int showAsAction = showAsAction(attributes);
        int actionLayout = reference(attributes, "actionLayout", 0);
        String actionViewClass = attributes.getAttributeValue(ANDROID_NAMESPACE, "actionViewClass");

        MenuItem item;
        boolean hasSubMenu = false;
        SubMenu subMenu = null;

        int depth = parser.getDepth();
        int event;
        int marker = parser.getLineNumber();

        item = menu.add(group.id, id, order, title);

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if ("menu".equals(parser.getName()))
            {
                if (!hasSubMenu)
                {
                    menu.removeItem(id);
                    subMenu = menu.addSubMenu(group.id, id, order, title);
                    item = subMenu.getItem();
                    hasSubMenu = true;
                }

                parseChildren(parser, attributes, subMenu, new GroupState());
                continue;
            }

            skip(parser);
        }

        if (condensed != null) item.setTitleCondensed(condensed);
        if (icon != 0) item.setIcon(icon);
        item.setCheckable(checkable != 0);
        if (checkable == 2 && menu instanceof RuntimeMenu) ((RuntimeMenu)menu).setGroupCheckable(group.id, true, true);
        item.setChecked(checked);
        item.setVisible(visible);
        item.setEnabled(enabled);
        if (showAsAction >= 0) item.setShowAsAction(showAsAction);

        if (actionLayout != 0)
        {
            item.setActionView(actionLayout);
        }
        else if (actionViewClass != null)
        {
            try
            {
                Class<?> type = mContext.getClassLoader().loadClass(actionViewClass);
                item.setActionView((View)type.getConstructor(Context.class).newInstance(mContext));
            }
            catch (ReflectiveOperationException failure)
            {
                Log.w(TAG, "cannot instantiate the action view " + actionViewClass + " for the item on line " + marker, failure);
            }
        }
    }

    private static void skip(XmlPullParser parser) throws Exception
    {
        int depth = 1;

        while (depth > 0)
        {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) return;
            if (event == XmlPullParser.START_TAG) depth++;
            if (event == XmlPullParser.END_TAG) depth--;
        }
    }

    private static int[] typedValue(AttributeSet attributes, String name)
    {
        if (!(attributes instanceof XmlBlock.Parser)) return null;

        return ((XmlBlock.Parser)attributes).getAttributeTypedValue(ANDROID_NAMESPACE, name);
    }

    private static int reference(AttributeSet attributes, String name, int fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] == TypedValue.TYPE_REFERENCE) return packed[1];

        return fallback;
    }

    private static int integer(AttributeSet attributes, String name, int fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] >= TypedValue.TYPE_FIRST_INT && packed[0] <= TypedValue.TYPE_LAST_INT) return packed[1];

        String value = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (value == null) return fallback;

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException malformed)
        {
            return fallback;
        }
    }

    private static boolean flag(AttributeSet attributes, String name, boolean fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] == TypedValue.TYPE_INT_BOOLEAN) return packed[1] != 0;

        String value = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        return value == null ? fallback : "true".equals(value);
    }

    private CharSequence text(AttributeSet attributes, String name)
    {
        int[] packed = typedValue(attributes, name);

        if (packed != null && packed[0] == TypedValue.TYPE_REFERENCE && packed[1] != 0)
        {
            try
            {
                return mContext.getResources().getText(packed[1]);
            }
            catch (Resources.NotFoundException missing)
            {
                return null;
            }
        }

        return attributes.getAttributeValue(ANDROID_NAMESPACE, name);
    }

    private static int showAsAction(AttributeSet attributes)
    {
        int[] packed = typedValue(attributes, "showAsAction");
        if (packed != null && packed[0] >= TypedValue.TYPE_FIRST_INT && packed[0] <= TypedValue.TYPE_LAST_INT) return packed[1];

        String value = attributes.getAttributeValue(ANDROID_NAMESPACE, "showAsAction");
        if (value == null) return -1;

        int flags = 0;
        for (String part : value.split("\\|"))
        {
            for (int index = 0; index < SHOW_AS_ACTION_LABELS.length; index++)
            {
                if (SHOW_AS_ACTION_LABELS[index].equals(part.trim())) flags |= index == 0 ? 0 : 1 << (index - 1);
            }
        }

        return flags;
    }
}
