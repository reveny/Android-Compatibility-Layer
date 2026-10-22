package android.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class RemoteViews implements LayoutInflater.Filter, Parcelable
{
    public static final String EXTRA_CHECKED = "android.widget.extra.CHECKED";
    public static final String EXTRA_SHARED_ELEMENT_BOUNDS = "android.widget.extra.SHARED_ELEMENT_BOUNDS";
    public static final int MARGIN_LEFT = 0;
    public static final int MARGIN_TOP = 1;
    public static final int MARGIN_RIGHT = 2;
    public static final int MARGIN_BOTTOM = 3;
    public static final int MARGIN_START = 4;
    public static final int MARGIN_END = 5;

    private static final String TAG = "RemoteViews";
    private static final int CONTEXT_RESTRICTED = 4;

    private static final int MODE_NORMAL = 0;
    private static final int MODE_ORIENTATION = 1;
    private static final int MODE_SIZED = 2;
    private static final int MODE_DRAW_INSTRUCTIONS = 3;

    private static final int KIND_REFLECT = 1;
    private static final int KIND_REFLECT_NIGHT = 2;
    private static final int KIND_RESOURCE = 3;
    private static final int KIND_ATTRIBUTE = 4;
    private static final int KIND_DIMENSION = 5;
    private static final int KIND_IMAGE_ICON = 6;
    private static final int KIND_ADD_VIEW = 7;
    private static final int KIND_REMOVE_ALL = 8;
    private static final int KIND_PADDING = 9;
    private static final int KIND_TEXT_SIZE = 10;
    private static final int KIND_COMPOUND = 11;
    private static final int KIND_CHRONOMETER = 12;
    private static final int KIND_CLICK = 13;
    private static final int KIND_CHECKED_RESPONSE = 14;
    private static final int KIND_TEMPLATE = 15;
    private static final int KIND_EMPTY_VIEW = 16;
    private static final int KIND_LAYOUT = 17;
    private static final int KIND_COLLECTION = 18;
    private static final int KIND_SERVICE_ADAPTER = 19;
    private static final int KIND_PROGRESS = 20;
    private static final int KIND_STYLUS = 21;

    private static final int RESOLVE_COLOR = 0;
    private static final int RESOLVE_COLOR_STATE_LIST = 1;
    private static final int RESOLVE_DIMENSION_INT = 2;
    private static final int RESOLVE_DIMENSION_FLOAT = 3;
    private static final int RESOLVE_TEXT = 4;

    private static final int LAYOUT_SOURCE_VALUE = 0;
    private static final int LAYOUT_SOURCE_DIMENSION = 1;
    private static final int LAYOUT_SOURCE_ATTRIBUTE = 2;

    private static final int LAYOUT_WIDTH = 100;
    private static final int LAYOUT_HEIGHT = 101;
    private static final int LAYOUT_OUTLINE_RADIUS = 102;

    private static final HashSet<String> REMOTABLE_CLASSES = new HashSet<>(Arrays.asList(
        "android.widget.AdapterViewFlipper", "android.widget.AnalogClock", "android.widget.Button", "android.widget.CheckBox",
        "android.widget.Chronometer", "android.widget.FrameLayout", "android.widget.GridLayout", "android.widget.GridView",
        "android.widget.ImageButton", "android.widget.ImageView", "android.widget.LinearLayout", "android.widget.ListView",
        "android.widget.ProgressBar", "android.widget.RadioButton", "android.widget.RadioGroup", "android.widget.RelativeLayout",
        "android.widget.StackView", "android.widget.Switch", "android.widget.TextClock", "android.widget.TextView",
        "android.widget.ViewFlipper", "android.view.ViewStub"));

    private static final WeakHashMap<View, PendingIntent> sTemplates = new WeakHashMap<>();

    private final String mPackageName;
    private final int mLayoutId;
    private final int mViewId;
    private final int mMode;
    private final ArrayList<Action> mActions = new ArrayList<>();
    private int mLightBackgroundLayoutId;
    private RemoteViews mLandscape;
    private RemoteViews mPortrait;
    private LinkedHashMap<SizeF, RemoteViews> mSizedViews;
    private DrawInstructions mDrawInstructions;

    public RemoteViews(String packageName, int layoutId)
    {
        this(packageName, layoutId, View.NO_ID);
    }

    public RemoteViews(String packageName, int layoutId, int viewId)
    {
        mPackageName = packageName;
        mLayoutId = layoutId;
        mViewId = viewId;
        mMode = MODE_NORMAL;
    }

    public RemoteViews(RemoteViews landscape, RemoteViews portrait)
    {
        if (landscape == null || portrait == null) throw new IllegalArgumentException("Both RemoteViews must be non-null");
        if (!landscape.hasSamePackage(portrait)) throw new IllegalArgumentException("Both RemoteViews must share the same package");

        mPackageName = portrait.mPackageName;
        mLayoutId = portrait.mLayoutId;
        mViewId = portrait.mViewId;
        mLightBackgroundLayoutId = portrait.mLightBackgroundLayoutId;
        mMode = MODE_ORIENTATION;
        mLandscape = landscape;
        mPortrait = portrait;
    }

    public RemoteViews(Map<SizeF, RemoteViews> remoteViews)
    {
        if (remoteViews == null || remoteViews.isEmpty()) throw new IllegalArgumentException("The set of RemoteViews cannot be empty");
        if (remoteViews.size() > 16) throw new IllegalArgumentException("Too many RemoteViews in constructor");

        RemoteViews smallest = null;
        float smallestArea = Float.MAX_VALUE;

        for (Map.Entry<SizeF, RemoteViews> entry : remoteViews.entrySet())
        {
            RemoteViews views = entry.getValue();
            if (views == null) throw new IllegalArgumentException("RemoteViews cannot be null");
            if (smallest != null && !smallest.hasSamePackage(views))
            {
                throw new IllegalArgumentException("All RemoteViews must share the same package");
            }

            float area = entry.getKey().getWidth() * entry.getKey().getHeight();
            if (smallest == null || area < smallestArea)
            {
                smallest = views;
                smallestArea = area;
            }
        }

        mPackageName = smallest.mPackageName;
        mLayoutId = smallest.mLayoutId;
        mViewId = smallest.mViewId;
        mLightBackgroundLayoutId = smallest.mLightBackgroundLayoutId;
        mMode = MODE_SIZED;
        mSizedViews = new LinkedHashMap<>(remoteViews);
    }

    public RemoteViews(DrawInstructions drawInstructions)
    {
        if (drawInstructions == null) throw new IllegalArgumentException("drawInstructions cannot be null");

        mPackageName = null;
        mLayoutId = 0;
        mViewId = View.NO_ID;
        mMode = MODE_DRAW_INSTRUCTIONS;
        mDrawInstructions = drawInstructions;
    }

    public RemoteViews(RemoteViews source)
    {
        mPackageName = source.mPackageName;
        mLayoutId = source.mLayoutId;
        mViewId = source.mViewId;
        mMode = source.mMode;
        mLightBackgroundLayoutId = source.mLightBackgroundLayoutId;
        mDrawInstructions = source.mDrawInstructions;
        if (source.mLandscape != null) mLandscape = new RemoteViews(source.mLandscape);
        if (source.mPortrait != null) mPortrait = new RemoteViews(source.mPortrait);

        if (source.mSizedViews != null)
        {
            mSizedViews = new LinkedHashMap<>();
            for (Map.Entry<SizeF, RemoteViews> entry : source.mSizedViews.entrySet())
            {
                mSizedViews.put(entry.getKey(), new RemoteViews(entry.getValue()));
            }
        }

        for (Action action : source.mActions) mActions.add(action.copy());
    }

    public RemoteViews(Parcel parcel)
    {
        mMode = parcel.readInt();
        mPackageName = parcel.readString();
        mLayoutId = parcel.readInt();
        mViewId = parcel.readInt();
        mLightBackgroundLayoutId = parcel.readInt();

        if (mMode == MODE_ORIENTATION)
        {
            mLandscape = new RemoteViews(parcel);
            mPortrait = new RemoteViews(parcel);
        }
        else if (mMode == MODE_SIZED)
        {
            int count = parcel.readInt();
            mSizedViews = new LinkedHashMap<>();
            for (int index = 0; index < count; index++)
            {
                SizeF size = new SizeF(parcel.readFloat(), parcel.readFloat());
                mSizedViews.put(size, new RemoteViews(parcel));
            }
        }
        else if (mMode == MODE_DRAW_INSTRUCTIONS)
        {
            int count = parcel.readInt();
            ArrayList<byte[]> instructions = new ArrayList<>();
            for (int index = 0; index < count; index++) instructions.add(parcel.createByteArray());
            mDrawInstructions = new DrawInstructions(instructions);
        }

        int count = parcel.readInt();
        for (int index = 0; index < count; index++) mActions.add(Action.read(parcel));
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(mMode);
        out.writeString(mPackageName);
        out.writeInt(mLayoutId);
        out.writeInt(mViewId);
        out.writeInt(mLightBackgroundLayoutId);

        if (mMode == MODE_ORIENTATION)
        {
            mLandscape.writeToParcel(out, flags);
            mPortrait.writeToParcel(out, flags);
        }
        else if (mMode == MODE_SIZED)
        {
            out.writeInt(mSizedViews.size());
            for (Map.Entry<SizeF, RemoteViews> entry : mSizedViews.entrySet())
            {
                out.writeFloat(entry.getKey().getWidth());
                out.writeFloat(entry.getKey().getHeight());
                entry.getValue().writeToParcel(out, flags);
            }
        }
        else if (mMode == MODE_DRAW_INSTRUCTIONS)
        {
            out.writeInt(mDrawInstructions.mInstructions.size());
            for (byte[] instruction : mDrawInstructions.mInstructions) out.writeByteArray(instruction);
        }

        out.writeInt(mActions.size());
        for (Action action : mActions) action.write(out);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Parcelable.Creator<RemoteViews> CREATOR = new Parcelable.Creator<RemoteViews>()
    {
        @Override
        public RemoteViews createFromParcel(Parcel parcel)
        {
            return new RemoteViews(parcel);
        }

        @Override
        public RemoteViews[] newArray(int size)
        {
            return new RemoteViews[size];
        }
    };

    private boolean hasSamePackage(RemoteViews other)
    {
        return mPackageName == null ? other.mPackageName == null : mPackageName.equals(other.mPackageName);
    }

    @Override
    public RemoteViews clone()
    {
        if (mMode == MODE_ORIENTATION || mMode == MODE_SIZED)
        {
            throw new IllegalStateException("Cannot clone a RemoteViews built from several RemoteViews");
        }

        return new RemoteViews(this);
    }

    public String getPackage()
    {
        return mPackageName;
    }

    public int getLayoutId()
    {
        return mLayoutId;
    }

    public int getViewId()
    {
        return mViewId;
    }

    public void setLightBackgroundLayoutId(int layoutId)
    {
        mLightBackgroundLayoutId = layoutId;
    }

    @Override
    public boolean onLoadClass(Class type)
    {
        if (type == null) return false;

        return type.isAnnotationPresent(RemoteView.class) || REMOTABLE_CLASSES.contains(type.getName());
    }

    public View apply(Context context, ViewGroup parent)
    {
        RemoteViews target = chooseLayout(context);

        if (target.mMode == MODE_DRAW_INSTRUCTIONS)
        {
            android.util.Log.w(TAG, "draw instructions cannot be played back here, so an empty view stands in for them");
            return new View(context);
        }

        if (target.mLayoutId == 0)
        {
            android.util.Log.w(TAG, "no layout was given for " + target.mPackageName + ", so nothing was inflated");
            return null;
        }

        Context inflationContext = packageContext(context, target.mPackageName);
        LayoutInflater inflater = LayoutInflater.from(inflationContext).cloneInContext(inflationContext);
        inflater.setFilter(target);

        View view = inflater.inflate(target.mLayoutId, parent, false);
        if (target.mViewId != View.NO_ID) view.setId(target.mViewId);
        view.setTag(TAG.hashCode(), Integer.valueOf(target.mLayoutId));

        target.performApply(view, parent, context);
        return view;
    }

    public void reapply(Context context, View view)
    {
        RemoteViews target = chooseLayout(context);
        if (target.mMode == MODE_DRAW_INSTRUCTIONS) return;

        Object layout = view.getTag(TAG.hashCode());
        if (layout instanceof Integer && (Integer)layout != target.mLayoutId)
        {
            throw new ActionException("Attempting to re-apply RemoteViews to a view that was not created from the same layout");
        }

        ViewGroup parent = view.getParent() instanceof ViewGroup ? (ViewGroup)view.getParent() : null;
        target.performApply(view, parent, context);
    }

    private RemoteViews chooseLayout(Context context)
    {
        if (mMode == MODE_ORIENTATION)
        {
            boolean landscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            return landscape ? mLandscape : mPortrait;
        }

        if (mMode == MODE_SIZED)
        {
            RemoteViews smallest = null;
            float smallestArea = Float.MAX_VALUE;

            for (Map.Entry<SizeF, RemoteViews> entry : mSizedViews.entrySet())
            {
                float area = entry.getKey().getWidth() * entry.getKey().getHeight();
                if (smallest == null || area < smallestArea)
                {
                    smallest = entry.getValue();
                    smallestArea = area;
                }
            }

            return smallest;
        }

        return this;
    }

    private static Context packageContext(Context context, String packageName)
    {
        if (packageName == null || packageName.equals(context.getPackageName())) return context;

        try
        {
            return context.createPackageContext(packageName, CONTEXT_RESTRICTED);
        }
        catch (Throwable missing)
        {
            android.util.Log.w(TAG, "package " + packageName + " is not available, so its layout is inflated from " + context.getPackageName());
            return context;
        }
    }

    private void performApply(View root, ViewGroup parent, Context context)
    {
        for (Action action : mActions) action.apply(root, parent, context);
    }

    private void addAction(Action action)
    {
        if (mMode == MODE_ORIENTATION || mMode == MODE_SIZED)
        {
            throw new IllegalStateException("RemoteViews built from several RemoteViews cannot be modified; modify the children instead");
        }

        if (mMode == MODE_DRAW_INSTRUCTIONS) throw new IllegalStateException("RemoteViews made from draw instructions cannot be modified");

        mActions.add(action);
    }

    private void reflect(int viewId, String method, Class<?> type, Object value)
    {
        addAction(new Action(KIND_REFLECT, viewId, method, type, value, null, null));
    }

    private void resource(int kind, int viewId, String method, Class<?> type, int resourceId, int resolve)
    {
        addAction(new Action(kind, viewId, method, type, null, null, new int[] {resourceId, resolve}));
    }

    public void addView(int viewId, RemoteViews nestedView)
    {
        if (nestedView == null) throw new NullPointerException("nestedView");

        addAction(new Action(KIND_ADD_VIEW, viewId, null, null, nestedView, null, new int[] {-1}));
    }

    public void addStableView(int viewId, RemoteViews nestedView, int stableId)
    {
        if (nestedView == null) throw new NullPointerException("nestedView");

        addAction(new Action(KIND_ADD_VIEW, viewId, null, null, nestedView, null, new int[] {stableId}));
    }

    public void removeAllViews(int viewId)
    {
        addAction(new Action(KIND_REMOVE_ALL, viewId, null, null, null, null, null));
    }

    public void showNext(int viewId)
    {
        reflect(viewId, "showNext", null, null);
    }

    public void showPrevious(int viewId)
    {
        reflect(viewId, "showPrevious", null, null);
    }

    public void setDisplayedChild(int viewId, int childIndex)
    {
        reflect(viewId, "setDisplayedChild", int.class, childIndex);
    }

    public void setViewVisibility(int viewId, int visibility)
    {
        reflect(viewId, "setVisibility", int.class, visibility);
    }

    public void setTextViewText(int viewId, CharSequence text)
    {
        reflect(viewId, "setText", CharSequence.class, text);
    }

    public void setTextViewTextSize(int viewId, int units, float size)
    {
        addAction(new Action(KIND_TEXT_SIZE, viewId, null, null, size, null, new int[] {units}));
    }

    public void setTextViewCompoundDrawables(int viewId, int left, int top, int right, int bottom)
    {
        addAction(new Action(KIND_COMPOUND, viewId, "absolute", null, null, null, new int[] {left, top, right, bottom}));
    }

    public void setTextViewCompoundDrawablesRelative(int viewId, int start, int top, int end, int bottom)
    {
        addAction(new Action(KIND_COMPOUND, viewId, "relative", null, null, null, new int[] {start, top, end, bottom}));
    }

    public void setImageViewResource(int viewId, int srcId)
    {
        reflect(viewId, "setImageResource", int.class, srcId);
    }

    public void setImageViewUri(int viewId, Uri uri)
    {
        reflect(viewId, "setImageURI", Uri.class, uri);
    }

    public void setImageViewBitmap(int viewId, Bitmap bitmap)
    {
        reflect(viewId, "setImageBitmap", Bitmap.class, bitmap);
    }

    public void setImageViewIcon(int viewId, Icon icon)
    {
        addAction(new Action(KIND_IMAGE_ICON, viewId, null, null, icon, null, null));
    }

    public void setEmptyView(int viewId, int emptyViewId)
    {
        addAction(new Action(KIND_EMPTY_VIEW, viewId, null, null, null, null, new int[] {emptyViewId}));
    }

    public void setChronometer(int viewId, long base, String format, boolean started)
    {
        addAction(new Action(KIND_CHRONOMETER, viewId, null, null, base, format, new int[] {started ? 1 : 0}));
    }

    public void setChronometerCountDown(int viewId, boolean isCountDown)
    {
        reflect(viewId, "setCountDown", boolean.class, isCountDown);
    }

    public void setProgressBar(int viewId, int max, int progress, boolean indeterminate)
    {
        addAction(new Action(KIND_PROGRESS, viewId, null, null, null, null, new int[] {max, progress, indeterminate ? 1 : 0}));
    }

    public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent)
    {
        addAction(new Action(KIND_CLICK, viewId, null, null, pendingIntent, null, null));
    }

    public void setOnClickResponse(int viewId, RemoteResponse response)
    {
        if (response == null) throw new NullPointerException("response");

        addAction(new Action(KIND_CLICK, viewId, null, null, response.mPendingIntent, response.mFillIntent, null));
    }

    public void setOnCheckedChangeResponse(int viewId, RemoteResponse response)
    {
        if (response == null) throw new NullPointerException("response");

        addAction(new Action(KIND_CHECKED_RESPONSE, viewId, null, null, response.mPendingIntent, response.mFillIntent, null));
    }

    public void setOnStylusHandwritingPendingIntent(int viewId, PendingIntent pendingIntent)
    {
        addAction(new Action(KIND_STYLUS, viewId, null, null, pendingIntent, null, null));
    }

    public void setPendingIntentTemplate(int viewId, PendingIntent pendingIntentTemplate)
    {
        addAction(new Action(KIND_TEMPLATE, viewId, null, null, pendingIntentTemplate, null, null));
    }

    public void setOnClickFillInIntent(int viewId, Intent fillInIntent)
    {
        addAction(new Action(KIND_CLICK, viewId, null, null, null, fillInIntent, null));
    }

    public void setTextColor(int viewId, int color)
    {
        reflect(viewId, "setTextColor", int.class, color);
    }

    public void setColorStateList(int viewId, String methodName, ColorStateList value)
    {
        reflect(viewId, methodName, ColorStateList.class, value);
    }

    public void setColorStateList(int viewId, String methodName, ColorStateList notNight, ColorStateList night)
    {
        addAction(new Action(KIND_REFLECT_NIGHT, viewId, methodName, ColorStateList.class, notNight, night, null));
    }

    public void setColorStateList(int viewId, String methodName, int colorResource)
    {
        resource(KIND_RESOURCE, viewId, methodName, ColorStateList.class, colorResource, RESOLVE_COLOR_STATE_LIST);
    }

    public void setColorStateListAttr(int viewId, String methodName, int colorAttr)
    {
        resource(KIND_ATTRIBUTE, viewId, methodName, ColorStateList.class, colorAttr, RESOLVE_COLOR_STATE_LIST);
    }

    public void setRemoteAdapter(int viewId, Intent intent)
    {
        addAction(new Action(KIND_SERVICE_ADAPTER, viewId, null, null, intent, null, null));
    }

    public void setRemoteAdapter(int appWidgetId, int viewId, Intent intent)
    {
        setRemoteAdapter(viewId, intent);
    }

    public void setRemoteAdapter(int viewId, RemoteCollectionItems items)
    {
        if (items == null) throw new NullPointerException("items");

        addAction(new Action(KIND_COLLECTION, viewId, null, null, items, null, null));
    }

    public void setScrollPosition(int viewId, int position)
    {
        reflect(viewId, "smoothScrollToPosition", int.class, position);
    }

    public void setRelativeScrollPosition(int viewId, int offset)
    {
        reflect(viewId, "smoothScrollByOffset", int.class, offset);
    }

    public void setViewPadding(int viewId, int left, int top, int right, int bottom)
    {
        addAction(new Action(KIND_PADDING, viewId, null, null, null, null, new int[] {left, top, right, bottom}));
    }

    public void setViewLayoutMarginDimen(int viewId, int type, int dimen)
    {
        layout(viewId, type, LAYOUT_SOURCE_DIMENSION, dimen, 0);
    }

    public void setViewLayoutMarginAttr(int viewId, int type, int attr)
    {
        layout(viewId, type, LAYOUT_SOURCE_ATTRIBUTE, attr, 0);
    }

    public void setViewLayoutMargin(int viewId, int type, float value, int units)
    {
        layout(viewId, type, LAYOUT_SOURCE_VALUE, units, value);
    }

    public void setViewLayoutWidth(int viewId, float width, int units)
    {
        layout(viewId, LAYOUT_WIDTH, LAYOUT_SOURCE_VALUE, units, width);
    }

    public void setViewLayoutWidthDimen(int viewId, int widthDimen)
    {
        layout(viewId, LAYOUT_WIDTH, LAYOUT_SOURCE_DIMENSION, widthDimen, 0);
    }

    public void setViewLayoutWidthAttr(int viewId, int widthAttr)
    {
        layout(viewId, LAYOUT_WIDTH, LAYOUT_SOURCE_ATTRIBUTE, widthAttr, 0);
    }

    public void setViewLayoutHeight(int viewId, float height, int units)
    {
        layout(viewId, LAYOUT_HEIGHT, LAYOUT_SOURCE_VALUE, units, height);
    }

    public void setViewLayoutHeightDimen(int viewId, int heightDimen)
    {
        layout(viewId, LAYOUT_HEIGHT, LAYOUT_SOURCE_DIMENSION, heightDimen, 0);
    }

    public void setViewLayoutHeightAttr(int viewId, int heightAttr)
    {
        layout(viewId, LAYOUT_HEIGHT, LAYOUT_SOURCE_ATTRIBUTE, heightAttr, 0);
    }

    public void setViewOutlinePreferredRadius(int viewId, float radius, int units)
    {
        layout(viewId, LAYOUT_OUTLINE_RADIUS, LAYOUT_SOURCE_VALUE, units, radius);
    }

    public void setViewOutlinePreferredRadiusDimen(int viewId, int resId)
    {
        layout(viewId, LAYOUT_OUTLINE_RADIUS, LAYOUT_SOURCE_DIMENSION, resId, 0);
    }

    public void setViewOutlinePreferredRadiusAttr(int viewId, int attrId)
    {
        layout(viewId, LAYOUT_OUTLINE_RADIUS, LAYOUT_SOURCE_ATTRIBUTE, attrId, 0);
    }

    private void layout(int viewId, int target, int source, int argument, float value)
    {
        if (target < MARGIN_LEFT || (target > MARGIN_END && target < LAYOUT_WIDTH))
        {
            throw new IllegalArgumentException("Unknown margin type " + target);
        }

        addAction(new Action(KIND_LAYOUT, viewId, null, null, value, null, new int[] {target, source, argument}));
    }

    public void setBoolean(int viewId, String methodName, boolean value)
    {
        reflect(viewId, methodName, boolean.class, value);
    }

    public void setByte(int viewId, String methodName, byte value)
    {
        reflect(viewId, methodName, byte.class, (int)value);
    }

    public void setShort(int viewId, String methodName, short value)
    {
        reflect(viewId, methodName, short.class, value);
    }

    public void setInt(int viewId, String methodName, int value)
    {
        reflect(viewId, methodName, int.class, value);
    }

    public void setColor(int viewId, String methodName, int colorResource)
    {
        resource(KIND_RESOURCE, viewId, methodName, int.class, colorResource, RESOLVE_COLOR);
    }

    public void setColorAttr(int viewId, String methodName, int colorAttribute)
    {
        resource(KIND_ATTRIBUTE, viewId, methodName, int.class, colorAttribute, RESOLVE_COLOR);
    }

    public void setColorInt(int viewId, String methodName, int notNight, int night)
    {
        addAction(new Action(KIND_REFLECT_NIGHT, viewId, methodName, int.class, notNight, night, null));
    }

    public void setIntDimen(int viewId, String methodName, int dimenResource)
    {
        resource(KIND_RESOURCE, viewId, methodName, int.class, dimenResource, RESOLVE_DIMENSION_INT);
    }

    public void setIntDimen(int viewId, String methodName, float value, int unit)
    {
        addAction(new Action(KIND_DIMENSION, viewId, methodName, int.class, value, null, new int[] {unit}));
    }

    public void setIntDimenAttr(int viewId, String methodName, int dimenAttr)
    {
        resource(KIND_ATTRIBUTE, viewId, methodName, int.class, dimenAttr, RESOLVE_DIMENSION_INT);
    }

    public void setLong(int viewId, String methodName, long value)
    {
        reflect(viewId, methodName, long.class, value);
    }

    public void setFloat(int viewId, String methodName, float value)
    {
        reflect(viewId, methodName, float.class, value);
    }

    public void setFloatDimen(int viewId, String methodName, int dimenResource)
    {
        resource(KIND_RESOURCE, viewId, methodName, float.class, dimenResource, RESOLVE_DIMENSION_FLOAT);
    }

    public void setFloatDimen(int viewId, String methodName, float value, int unit)
    {
        addAction(new Action(KIND_DIMENSION, viewId, methodName, float.class, value, null, new int[] {unit}));
    }

    public void setFloatDimenAttr(int viewId, String methodName, int dimenAttr)
    {
        resource(KIND_ATTRIBUTE, viewId, methodName, float.class, dimenAttr, RESOLVE_DIMENSION_FLOAT);
    }

    public void setDouble(int viewId, String methodName, double value)
    {
        reflect(viewId, methodName, double.class, value);
    }

    public void setChar(int viewId, String methodName, char value)
    {
        reflect(viewId, methodName, char.class, (int)value);
    }

    public void setString(int viewId, String methodName, String value)
    {
        reflect(viewId, methodName, String.class, value);
    }

    public void setCharSequence(int viewId, String methodName, CharSequence value)
    {
        reflect(viewId, methodName, CharSequence.class, value);
    }

    public void setCharSequence(int viewId, String methodName, int stringResource)
    {
        resource(KIND_RESOURCE, viewId, methodName, CharSequence.class, stringResource, RESOLVE_TEXT);
    }

    public void setCharSequenceAttr(int viewId, String methodName, int stringAttribute)
    {
        resource(KIND_ATTRIBUTE, viewId, methodName, CharSequence.class, stringAttribute, RESOLVE_TEXT);
    }

    public void setUri(int viewId, String methodName, Uri value)
    {
        reflect(viewId, methodName, Uri.class, value);
    }

    public void setBitmap(int viewId, String methodName, Bitmap value)
    {
        reflect(viewId, methodName, Bitmap.class, value);
    }

    public void setBlendMode(int viewId, String methodName, BlendMode value)
    {
        reflect(viewId, methodName, BlendMode.class, value == null ? null : value.name());
    }

    public void setBundle(int viewId, String methodName, Bundle value)
    {
        reflect(viewId, methodName, Bundle.class, value);
    }

    public void setIntent(int viewId, String methodName, Intent value)
    {
        reflect(viewId, methodName, Intent.class, value);
    }

    public void setIcon(int viewId, String methodName, Icon value)
    {
        reflect(viewId, methodName, Icon.class, value);
    }

    public void setIcon(int viewId, String methodName, Icon notNight, Icon night)
    {
        addAction(new Action(KIND_REFLECT_NIGHT, viewId, methodName, Icon.class, notNight, night, null));
    }

    public void setContentDescription(int viewId, CharSequence contentDescription)
    {
        reflect(viewId, "setContentDescription", CharSequence.class, contentDescription);
    }

    public void setAccessibilityTraversalBefore(int viewId, int nextId)
    {
        reflect(viewId, "setAccessibilityTraversalBefore", int.class, nextId);
    }

    public void setAccessibilityTraversalAfter(int viewId, int nextId)
    {
        reflect(viewId, "setAccessibilityTraversalAfter", int.class, nextId);
    }

    public void setLabelFor(int viewId, int labeledId)
    {
        reflect(viewId, "setLabelFor", int.class, labeledId);
    }

    public void setCompoundButtonChecked(int viewId, boolean checked)
    {
        reflect(viewId, "setChecked", boolean.class, checked);
    }

    public void setRadioGroupChecked(int viewId, int checkedId)
    {
        reflect(viewId, "check", int.class, checkedId);
    }

    private static Class<?> typeFor(String name)
    {
        if (name == null) return null;

        switch (name)
        {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            default:
                try
                {
                    return Class.forName(name);
                }
                catch (ClassNotFoundException missing)
                {
                    throw new ActionException(missing);
                }
        }
    }

    private static Object convert(Class<?> type, Object value)
    {
        if (value == null) return null;
        if (type == byte.class) return (byte)((Number)value).intValue();
        if (type == char.class) return (char)((Number)value).intValue();
        if (type == short.class) return ((Number)value).shortValue();
        if (type == BlendMode.class && value instanceof String) return BlendMode.valueOf((String)value);

        return value;
    }

    private static boolean isNight(Context context)
    {
        Configuration configuration = context.getResources().getConfiguration();
        return (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void sendClick(View view, PendingIntent pendingIntent, Intent fillIn)
    {
        try
        {
            pendingIntent.send(view.getContext(), 0, fillIn);
        }
        catch (PendingIntent.CanceledException cancelled)
        {
            android.util.Log.w(TAG, "the click PendingIntent was cancelled", cancelled);
        }
    }

    private static PendingIntent templateFor(View view)
    {
        android.view.ViewParent current = view.getParent();

        while (current instanceof View)
        {
            PendingIntent template = sTemplates.get((View)current);
            if (template != null) return template;

            current = current.getParent();
        }

        return null;
    }

    private static final class Action
    {
        private final int mKind;
        private final int mViewId;
        private final String mMethod;
        private final Class<?> mType;
        private final Object mValue;
        private final Object mAlternate;
        private final int[] mNumbers;

        Action(int kind, int viewId, String method, Class<?> type, Object value, Object alternate, int[] numbers)
        {
            mKind = kind;
            mViewId = viewId;
            mMethod = method;
            mType = type;
            mValue = value;
            mAlternate = alternate;
            mNumbers = numbers;
        }

        Action copy()
        {
            Object value = mValue instanceof RemoteViews ? new RemoteViews((RemoteViews)mValue) : mValue;
            return new Action(mKind, mViewId, mMethod, mType, value, mAlternate, mNumbers == null ? null : mNumbers.clone());
        }

        void write(Parcel out)
        {
            out.writeInt(mKind);
            out.writeInt(mViewId);
            out.writeString(mMethod);
            out.writeString(mType == null ? null : mType.getName());

            if (mValue instanceof RemoteViews)
            {
                out.writeInt(1);
                ((RemoteViews)mValue).writeToParcel(out, 0);
            }
            else if (mValue instanceof RemoteCollectionItems)
            {
                out.writeInt(2);
                ((RemoteCollectionItems)mValue).writeToParcel(out, 0);
            }
            else
            {
                out.writeInt(0);
                out.writeValue(mValue);
            }

            out.writeValue(mAlternate);
            out.writeIntArray(mNumbers);
        }

        static Action read(Parcel source)
        {
            int kind = source.readInt();
            int viewId = source.readInt();
            String method = source.readString();
            Class<?> type = typeFor(source.readString());

            int valueKind = source.readInt();
            Object value;
            if (valueKind == 1) value = new RemoteViews(source);
            else if (valueKind == 2) value = RemoteCollectionItems.CREATOR.createFromParcel(source);
            else value = source.readValue(RemoteViews.class.getClassLoader());

            Object alternate = source.readValue(RemoteViews.class.getClassLoader());
            int[] numbers = source.createIntArray();

            return new Action(kind, viewId, method, type, value, alternate, numbers);
        }

        void apply(View root, ViewGroup parent, Context context)
        {
            View target = root.findViewById(mViewId);
            if (target == null) return;

            switch (mKind)
            {
                case KIND_REFLECT:
                    invoke(target, mMethod, mType, mValue);
                    break;
                case KIND_REFLECT_NIGHT:
                    invoke(target, mMethod, mType, isNight(context) ? mAlternate : mValue);
                    break;
                case KIND_RESOURCE:
                    invoke(target, mMethod, mType, resolveResource(target.getContext(), mNumbers[0], mNumbers[1]));
                    break;
                case KIND_ATTRIBUTE:
                    invoke(target, mMethod, mType, resolveAttribute(target.getContext(), mNumbers[0], mNumbers[1]));
                    break;
                case KIND_DIMENSION:
                    applyDimension(target);
                    break;
                case KIND_IMAGE_ICON:
                    applyIcon(target);
                    break;
                case KIND_ADD_VIEW:
                    applyAddView(target, context);
                    break;
                case KIND_REMOVE_ALL:
                    if (!(target instanceof ViewGroup)) throw new ActionException("view " + mViewId + " is not a ViewGroup");
                    ((ViewGroup)target).removeAllViews();
                    break;
                case KIND_PADDING:
                    target.setPadding(mNumbers[0], mNumbers[1], mNumbers[2], mNumbers[3]);
                    break;
                case KIND_TEXT_SIZE:
                    if (!(target instanceof TextView)) throw new ActionException("view " + mViewId + " is not a TextView");
                    ((TextView)target).setTextSize(mNumbers[0], (Float)mValue);
                    break;
                case KIND_COMPOUND:
                    applyCompound(target);
                    break;
                case KIND_CHRONOMETER:
                    applyChronometer(target);
                    break;
                case KIND_CLICK:
                    applyClick(target);
                    break;
                case KIND_CHECKED_RESPONSE:
                    applyCheckedResponse(target);
                    break;
                case KIND_TEMPLATE:
                    if (mValue == null) sTemplates.remove(target);
                    else sTemplates.put(target, (PendingIntent)mValue);
                    break;
                case KIND_EMPTY_VIEW:
                    if (!(target instanceof AdapterView)) throw new ActionException("view " + mViewId + " is not an AdapterView");
                    ((AdapterView<?>)target).setEmptyView(root.findViewById(mNumbers[0]));
                    break;
                case KIND_LAYOUT:
                    applyLayout(target);
                    break;
                case KIND_COLLECTION:
                    applyCollection(target, context);
                    break;
                case KIND_SERVICE_ADAPTER:
                    android.util.Log.w(TAG, "no RemoteViewsService host exists here, so the adapter for view " + mViewId + " stays empty");
                    break;
                case KIND_PROGRESS:
                    applyProgress(target);
                    break;
                case KIND_STYLUS:
                    applyStylus(target);
                    break;
                default:
                    throw new ActionException("unknown action " + mKind);
            }
        }

        private static void invoke(View target, String name, Class<?> type, Object value)
        {
            Method method;

            try
            {
                method = type == null ? target.getClass().getMethod(name) : target.getClass().getMethod(name, type);
            }
            catch (NoSuchMethodException missing)
            {
                throw new ActionException("view: " + target.getClass().getName() + " doesn't have method: " + name
                                          + (type == null ? "()" : "(" + type.getName() + ")"));
            }

            try
            {
                if (type == null) method.invoke(target);
                else method.invoke(target, convert(type, value));
            }
            catch (InvocationTargetException failure)
            {
                Throwable cause = failure.getCause();
                throw new ActionException(cause instanceof Exception ? (Exception)cause : failure);
            }
            catch (IllegalAccessException failure)
            {
                throw new ActionException(failure);
            }
        }

        private static Object resolveResource(Context context, int resourceId, int resolve)
        {
            android.content.res.Resources resources = context.getResources();

            switch (resolve)
            {
                case RESOLVE_COLOR:
                    return resources.getColor(resourceId, context.getTheme());
                case RESOLVE_COLOR_STATE_LIST:
                    return resources.getColorStateList(resourceId, context.getTheme());
                case RESOLVE_DIMENSION_INT:
                    return resourceId == 0 ? 0 : resources.getDimensionPixelSize(resourceId);
                case RESOLVE_DIMENSION_FLOAT:
                    return resourceId == 0 ? 0f : resources.getDimension(resourceId);
                default:
                    return resourceId == 0 ? null : resources.getText(resourceId);
            }
        }

        private static Object resolveAttribute(Context context, int attribute, int resolve)
        {
            android.content.res.TypedArray array = context.obtainStyledAttributes(new int[] {attribute});

            try
            {
                switch (resolve)
                {
                    case RESOLVE_COLOR:
                        return array.getColor(0, 0);
                    case RESOLVE_COLOR_STATE_LIST:
                        return array.getColorStateList(0);
                    case RESOLVE_DIMENSION_INT:
                        return array.getDimensionPixelSize(0, 0);
                    case RESOLVE_DIMENSION_FLOAT:
                        return array.getDimension(0, 0f);
                    default:
                        return array.getText(0);
                }
            }
            finally
            {
                array.recycle();
            }
        }

        private void applyDimension(View target)
        {
            float pixels = TypedValue.applyDimension(mNumbers[0], (Float)mValue, target.getResources().getDisplayMetrics());
            if (mType == int.class) invoke(target, mMethod, int.class, (int)pixels);
            else invoke(target, mMethod, float.class, pixels);
        }

        private void applyIcon(View target)
        {
            if (!(target instanceof ImageView)) throw new ActionException("view " + mViewId + " is not an ImageView");

            Icon icon = (Icon)mValue;
            ((ImageView)target).setImageDrawable(icon == null ? null : icon.loadDrawable(target.getContext()));
        }

        private void applyAddView(View target, Context context)
        {
            if (!(target instanceof ViewGroup)) throw new ActionException("view " + mViewId + " is not a ViewGroup");

            ViewGroup group = (ViewGroup)target;
            RemoteViews nested = (RemoteViews)mValue;
            int stableId = mNumbers[0];

            if (stableId >= 0)
            {
                for (int index = 0; index < group.getChildCount(); index++)
                {
                    View child = group.getChildAt(index);
                    if (!Integer.valueOf(stableId).equals(child.getTag(STABLE_TAG))) continue;

                    nested.reapply(context, child);
                    return;
                }
            }

            View child = nested.apply(context, group);
            if (child == null) return;

            if (stableId >= 0) child.setTag(STABLE_TAG, stableId);
            group.addView(child);
        }

        private void applyCompound(View target)
        {
            if (!(target instanceof TextView)) throw new ActionException("view " + mViewId + " is not a TextView");

            TextView text = (TextView)target;
            if ("relative".equals(mMethod))
            {
                text.setCompoundDrawablesRelativeWithIntrinsicBounds(mNumbers[0], mNumbers[1], mNumbers[2], mNumbers[3]);
            }
            else
            {
                text.setCompoundDrawablesWithIntrinsicBounds(mNumbers[0], mNumbers[1], mNumbers[2], mNumbers[3]);
            }
        }

        private void applyChronometer(View target)
        {
            if (!(target instanceof Chronometer)) throw new ActionException("view " + mViewId + " is not a Chronometer");

            Chronometer chronometer = (Chronometer)target;
            chronometer.setBase((Long)mValue);
            chronometer.setFormat((String)mAlternate);
            if (mNumbers[0] != 0) chronometer.start();
            else chronometer.stop();
        }

        private void applyClick(View target)
        {
            PendingIntent pendingIntent = (PendingIntent)mValue;
            Intent fillIn = (Intent)mAlternate;

            if (pendingIntent == null && fillIn == null)
            {
                target.setOnClickListener(null);
                return;
            }

            target.setOnClickListener(view ->
            {
                PendingIntent intent = pendingIntent != null ? pendingIntent : templateFor(view);
                if (intent == null)
                {
                    android.util.Log.w(TAG, "a fill-in intent was clicked outside any PendingIntent template");
                    return;
                }

                sendClick(view, intent, fillIn);
            });
        }

        private void applyCheckedResponse(View target)
        {
            if (!(target instanceof CompoundButton)) throw new ActionException("view " + mViewId + " is not a CompoundButton");

            PendingIntent pendingIntent = (PendingIntent)mValue;
            Intent fillIn = (Intent)mAlternate;

            ((CompoundButton)target).setOnCheckedChangeListener((button, checked) ->
            {
                PendingIntent intent = pendingIntent != null ? pendingIntent : templateFor(button);
                if (intent == null) return;

                Intent extras = fillIn == null ? new Intent() : new Intent(fillIn);
                extras.putExtra(EXTRA_CHECKED, checked);
                sendClick(button, intent, extras);
            });
        }

        private void applyLayout(View target)
        {
            int kind = mNumbers[0];
            int source = mNumbers[1];
            int argument = mNumbers[2];
            float amount = (Float)mValue;

            android.util.DisplayMetrics metrics = target.getResources().getDisplayMetrics();
            float pixels;
            boolean special = false;

            if (source == LAYOUT_SOURCE_DIMENSION)
            {
                pixels = argument == 0 ? 0 : target.getResources().getDimension(argument);
            }
            else if (source == LAYOUT_SOURCE_ATTRIBUTE)
            {
                pixels = (Float)resolveAttribute(target.getContext(), argument, RESOLVE_DIMENSION_FLOAT);
            }
            else
            {
                special = argument == TypedValue.COMPLEX_UNIT_PX && (amount == ViewGroup.LayoutParams.MATCH_PARENT
                                                                    || amount == ViewGroup.LayoutParams.WRAP_CONTENT);
                pixels = special ? amount : TypedValue.applyDimension(argument, amount, metrics);
            }

            if (kind == LAYOUT_OUTLINE_RADIUS)
            {
                float radius = pixels;
                target.setOutlineProvider(new android.view.ViewOutlineProvider()
                {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline)
                    {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                    }
                });
                target.setClipToOutline(true);
                return;
            }

            ViewGroup.LayoutParams params = target.getLayoutParams();
            if (params == null) return;

            int size = special ? (int)pixels : Math.round(pixels);

            if (kind == LAYOUT_WIDTH) params.width = size;
            else if (kind == LAYOUT_HEIGHT) params.height = size;
            else if (params instanceof ViewGroup.MarginLayoutParams) applyMargin((ViewGroup.MarginLayoutParams)params, kind, size);
            else return;

            target.setLayoutParams(params);
        }

        private static void applyMargin(ViewGroup.MarginLayoutParams params, int kind, int size)
        {
            switch (kind)
            {
                case MARGIN_LEFT:
                    params.leftMargin = size;
                    break;
                case MARGIN_TOP:
                    params.topMargin = size;
                    break;
                case MARGIN_RIGHT:
                    params.rightMargin = size;
                    break;
                case MARGIN_BOTTOM:
                    params.bottomMargin = size;
                    break;
                case MARGIN_START:
                    params.setMarginStart(size);
                    break;
                default:
                    params.setMarginEnd(size);
                    break;
            }
        }

        private void applyCollection(View target, Context context)
        {
            if (!(target instanceof AbsListView) && !(target instanceof AdapterView))
            {
                throw new ActionException("view " + mViewId + " cannot take a remote adapter");
            }

            RemoteCollectionItems items = (RemoteCollectionItems)mValue;
            CollectionAdapter adapter = new CollectionAdapter(context, items);

            if (target instanceof AbsListView) ((AbsListView)target).setAdapter(adapter);
            else invoke(target, "setAdapter", android.widget.Adapter.class, adapter);
        }

        private void applyProgress(View target)
        {
            if (!(target instanceof ProgressBar)) throw new ActionException("view " + mViewId + " is not a ProgressBar");

            ProgressBar progress = (ProgressBar)target;
            progress.setIndeterminate(mNumbers[2] != 0);
            if (mNumbers[2] != 0) return;

            progress.setMax(mNumbers[0]);
            progress.setProgress(mNumbers[1]);
        }

        private void applyStylus(View target)
        {
            PendingIntent pendingIntent = (PendingIntent)mValue;

            if (pendingIntent == null)
            {
                target.setHandwritingDelegatorCallback(null);
                return;
            }

            target.setHandwritingDelegatorCallback(() -> sendClick(target, pendingIntent, null));
            target.setAllowedHandwritingDelegatePackage(pendingIntent.getCreatorPackage());
        }
    }

    private static final int STABLE_TAG = "RemoteViews.stableId".hashCode();

    private static final class CollectionAdapter extends BaseAdapter
    {
        private final Context mContext;
        private final RemoteCollectionItems mItems;

        CollectionAdapter(Context context, RemoteCollectionItems items)
        {
            mContext = context;
            mItems = items;
        }

        @Override
        public int getCount()
        {
            return mItems.getItemCount();
        }

        @Override
        public Object getItem(int position)
        {
            return mItems.getItemView(position);
        }

        @Override
        public long getItemId(int position)
        {
            return mItems.getItemId(position);
        }

        @Override
        public boolean hasStableIds()
        {
            return mItems.hasStableIds();
        }

        @Override
        public int getViewTypeCount()
        {
            return Math.max(1, mItems.getViewTypeCount());
        }

        @Override
        public int getItemViewType(int position)
        {
            int layout = mItems.getItemView(position).getLayoutId();
            int index = mItems.mLayoutIds.indexOf(layout);
            return index < 0 || index >= getViewTypeCount() ? 0 : index;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            RemoteViews views = mItems.getItemView(position);

            if (convertView != null)
            {
                Object layout = convertView.getTag(TAG.hashCode());
                if (layout instanceof Integer && (Integer)layout == views.getLayoutId())
                {
                    views.reapply(mContext, convertView);
                    return convertView;
                }
            }

            View view = views.apply(mContext, parent);
            return view == null ? new View(mContext) : view;
        }
    }

    public static class ActionException extends RuntimeException
    {
        public ActionException(Exception exception)
        {
            super(exception);
        }

        public ActionException(String message)
        {
            super(message);
        }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RemoteView
    {
    }

    public static class RemoteResponse
    {
        private PendingIntent mPendingIntent;
        private Intent mFillIntent;
        private final ArrayList<Integer> mSharedElementIds = new ArrayList<>();
        private final ArrayList<String> mSharedElementNames = new ArrayList<>();

        public RemoteResponse()
        {
        }

        public static RemoteResponse fromPendingIntent(PendingIntent pendingIntent)
        {
            RemoteResponse response = new RemoteResponse();
            response.mPendingIntent = pendingIntent;
            return response;
        }

        public static RemoteResponse fromFillInIntent(Intent fillIntent)
        {
            RemoteResponse response = new RemoteResponse();
            response.mFillIntent = fillIntent;
            return response;
        }

        public RemoteResponse addSharedElement(int viewId, String sharedElementName)
        {
            mSharedElementIds.add(viewId);
            mSharedElementNames.add(sharedElementName);
            return this;
        }
    }

    public static final class RemoteCollectionItems implements Parcelable
    {
        private final long[] mIds;
        private final RemoteViews[] mViews;
        private final boolean mHasStableIds;
        private final int mViewTypeCount;
        private final ArrayList<Integer> mLayoutIds = new ArrayList<>();

        RemoteCollectionItems()
        {
            this(new long[0], new RemoteViews[0], false, 1);
        }

        private RemoteCollectionItems(long[] ids, RemoteViews[] views, boolean hasStableIds, int viewTypeCount)
        {
            mIds = ids;
            mViews = views;
            mHasStableIds = hasStableIds;

            for (RemoteViews view : views)
            {
                if (!mLayoutIds.contains(view.getLayoutId())) mLayoutIds.add(view.getLayoutId());
            }

            int count = viewTypeCount < 1 ? mLayoutIds.size() : viewTypeCount;
            if (count < mLayoutIds.size())
            {
                throw new IllegalArgumentException("View type count is set to " + viewTypeCount + ", but the collection contains "
                                                   + mLayoutIds.size() + " different layout ids");
            }

            mViewTypeCount = Math.max(1, count);
        }

        public int getItemCount()
        {
            return mIds.length;
        }

        public long getItemId(int position)
        {
            return mIds[position];
        }

        public RemoteViews getItemView(int position)
        {
            return mViews[position];
        }

        public int getViewTypeCount()
        {
            return mViewTypeCount;
        }

        public boolean hasStableIds()
        {
            return mHasStableIds;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            out.writeInt(mIds.length);
            for (int index = 0; index < mIds.length; index++)
            {
                out.writeLong(mIds[index]);
                mViews[index].writeToParcel(out, flags);
            }
            out.writeInt(mHasStableIds ? 1 : 0);
            out.writeInt(mViewTypeCount);
        }

        public static final Parcelable.Creator<RemoteCollectionItems> CREATOR = new Parcelable.Creator<RemoteCollectionItems>()
        {
            @Override
            public RemoteCollectionItems createFromParcel(Parcel source)
            {
                int count = source.readInt();
                long[] ids = new long[count];
                RemoteViews[] views = new RemoteViews[count];
                for (int index = 0; index < count; index++)
                {
                    ids[index] = source.readLong();
                    views[index] = new RemoteViews(source);
                }

                boolean stable = source.readInt() != 0;
                int viewTypeCount = source.readInt();
                return new RemoteCollectionItems(ids, views, stable, viewTypeCount);
            }

            @Override
            public RemoteCollectionItems[] newArray(int size)
            {
                return new RemoteCollectionItems[size];
            }
        };

        public static final class Builder
        {
            private final ArrayList<Long> mIds = new ArrayList<>();
            private final ArrayList<RemoteViews> mViews = new ArrayList<>();
            private boolean mHasStableIds;
            private int mViewTypeCount;

            public Builder()
            {
            }

            public Builder addItem(long id, RemoteViews view)
            {
                if (view == null) throw new NullPointerException("view");
                if (view.mMode != MODE_NORMAL) throw new IllegalArgumentException("Only plain RemoteViews can be collection items");

                mIds.add(id);
                mViews.add(view);
                return this;
            }

            public Builder setHasStableIds(boolean hasStableIds)
            {
                mHasStableIds = hasStableIds;
                return this;
            }

            public Builder setViewTypeCount(int viewTypeCount)
            {
                mViewTypeCount = viewTypeCount;
                return this;
            }

            public RemoteCollectionItems build()
            {
                long[] ids = new long[mIds.size()];
                for (int index = 0; index < ids.length; index++) ids[index] = mIds.get(index);

                return new RemoteCollectionItems(ids, mViews.toArray(new RemoteViews[0]), mHasStableIds, mViewTypeCount);
            }
        }
    }

    public static final class DrawInstructions
    {
        private final java.util.List<byte[]> mInstructions;

        DrawInstructions()
        {
            this(new ArrayList<>());
        }

        private DrawInstructions(java.util.List<byte[]> instructions)
        {
            mInstructions = instructions;
        }

        public static long getSupportedVersion()
        {
            return 0L;
        }

        public static final class Builder
        {
            private final ArrayList<byte[]> mInstructions;

            public Builder(java.util.List<byte[]> instructions)
            {
                if (instructions == null) throw new NullPointerException("instructions");

                mInstructions = new ArrayList<>(instructions);
            }

            public DrawInstructions build()
            {
                return new DrawInstructions(mInstructions);
            }
        }
    }
}
