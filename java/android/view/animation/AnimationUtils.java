package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

public class AnimationUtils
{
    private static final String TAG = "AnimationUtils";

    private static final int SHORT_ANIMATION_TIME = 200;
    private static final int MEDIUM_ANIMATION_TIME = 400;
    private static final int LONG_ANIMATION_TIME = 500;

    private static final int ANIM_FADE_IN = 0x010a0000;
    private static final int ANIM_FADE_OUT = 0x010a0001;
    private static final int ANIM_SLIDE_IN_LEFT = 0x010a0002;
    private static final int ANIM_SLIDE_OUT_RIGHT = 0x010a0003;
    private static final int ANIM_ACCELERATE_DECELERATE_INTERPOLATOR = 0x010a0004;
    private static final int ANIM_ACCELERATE_INTERPOLATOR = 0x010a0005;
    private static final int ANIM_DECELERATE_INTERPOLATOR = 0x010a0006;
    private static final int ANIM_ANTICIPATE_INTERPOLATOR = 0x010a0007;
    private static final int ANIM_OVERSHOOT_INTERPOLATOR = 0x010a0008;
    private static final int ANIM_ANTICIPATE_OVERSHOOT_INTERPOLATOR = 0x010a0009;
    private static final int ANIM_BOUNCE_INTERPOLATOR = 0x010a000a;
    private static final int ANIM_LINEAR_INTERPOLATOR = 0x010a000b;
    private static final int ANIM_CYCLE_INTERPOLATOR = 0x010a000c;

    private static final int INTERPOLATOR_ACCELERATE_QUAD = 0x010c0000;
    private static final int INTERPOLATOR_DECELERATE_QUAD = 0x010c0001;
    private static final int INTERPOLATOR_ACCELERATE_CUBIC = 0x010c0002;
    private static final int INTERPOLATOR_DECELERATE_CUBIC = 0x010c0003;
    private static final int INTERPOLATOR_ACCELERATE_QUINT = 0x010c0004;
    private static final int INTERPOLATOR_DECELERATE_QUINT = 0x010c0005;
    private static final int INTERPOLATOR_ACCELERATE_DECELERATE = 0x010c0006;
    private static final int INTERPOLATOR_ANTICIPATE = 0x010c0007;
    private static final int INTERPOLATOR_OVERSHOOT = 0x010c0008;
    private static final int INTERPOLATOR_ANTICIPATE_OVERSHOOT = 0x010c0009;
    private static final int INTERPOLATOR_BOUNCE = 0x010c000a;
    private static final int INTERPOLATOR_LINEAR = 0x010c000b;
    private static final int INTERPOLATOR_CYCLE = 0x010c000c;
    private static final int INTERPOLATOR_FAST_OUT_SLOW_IN = 0x010c000d;
    private static final int INTERPOLATOR_LINEAR_OUT_SLOW_IN = 0x010c000e;
    private static final int INTERPOLATOR_FAST_OUT_LINEAR_IN = 0x010c000f;
    private static final int INTERPOLATOR_FAST_OUT_EXTRA_SLOW_IN = 0x010c001a;

    private static final String FAST_OUT_EXTRA_SLOW_IN_PATH =
        "M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1";

    public AnimationUtils()
    {
    }

    public static long currentAnimationTimeMillis()
    {
        return android.os.SystemClock.uptimeMillis();
    }

    public static Animation loadAnimation(Context context, int id) throws Resources.NotFoundException
    {
        Animation builtin = frameworkAnimation(id);
        if (builtin != null) return builtin;

        XmlResourceParser parser = context.getResources().getXml(id);

        try
        {
            Animation animation = createAnimationFromXml(context, parser, null, Xml.asAttributeSet(parser));
            if (animation != null) return animation;

            Log.w(TAG, "animation resource 0x" + Integer.toHexString(id) + " is empty");
        }
        catch (Exception failure)
        {
            Log.w(TAG, "could not inflate animation 0x" + Integer.toHexString(id) + ": " + failure);
        }
        finally
        {
            parser.close();
        }

        AlphaAnimation fallback = new AlphaAnimation(1.0f, 1.0f);
        fallback.setDuration(0);
        return fallback;
    }

    private static Animation createAnimationFromXml(Context context, XmlPullParser parser, AnimationSet parent, AttributeSet attributes)
        throws Exception
    {
        Animation animation = null;

        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT)
        {
            if (type != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if (name.equals("set"))
            {
                animation = new AnimationSet(context, attributes);
                createAnimationFromXml(context, parser, (AnimationSet)animation, attributes);
            }
            else if (name.equals("alpha"))
            {
                animation = new AlphaAnimation(context, attributes);
            }
            else if (name.equals("scale"))
            {
                animation = new ScaleAnimation(context, attributes);
            }
            else if (name.equals("rotate"))
            {
                animation = new RotateAnimation(context, attributes);
            }
            else if (name.equals("translate"))
            {
                animation = new TranslateAnimation(context, attributes);
            }
            else
            {
                Log.w(TAG, "unsupported animation element <" + name + ">, so it is skipped");
                animation = new AlphaAnimation(context, attributes);
            }

            if (parent != null) parent.addAnimation(animation);
        }

        return animation;
    }

    public static LayoutAnimationController loadLayoutAnimation(Context context, int id) throws Resources.NotFoundException
    {
        XmlResourceParser parser = context.getResources().getXml(id);

        try
        {
            LayoutAnimationController controller = createLayoutAnimationFromXml(context, parser, Xml.asAttributeSet(parser));
            if (controller != null) return controller;
        }
        catch (Exception failure)
        {
            Log.w(TAG, "could not inflate layout animation 0x" + Integer.toHexString(id) + ": " + failure);
        }
        finally
        {
            parser.close();
        }

        return new LayoutAnimationController(new AlphaAnimation(1.0f, 1.0f));
    }

    private static LayoutAnimationController createLayoutAnimationFromXml(Context context, XmlPullParser parser, AttributeSet attributes)
        throws Exception
    {
        LayoutAnimationController controller = null;

        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT)
        {
            if (type != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if (name.equals("layoutAnimation")) controller = new LayoutAnimationController(context, attributes);
            else if (name.equals("gridLayoutAnimation")) controller = new GridLayoutAnimationController(context, attributes);
            else Log.w(TAG, "unknown layout animation element <" + name + ">");
        }

        return controller;
    }

    public static Animation makeInAnimation(Context context, boolean fromLeft)
    {
        Animation animation = slide(fromLeft ? -0.5f : 0.5f, 0.0f, true, 0.0f, 1.0f);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setStartTime(currentAnimationTimeMillis());
        return animation;
    }

    public static Animation makeOutAnimation(Context context, boolean toRight)
    {
        Animation animation = slide(0.0f, toRight ? 0.5f : -0.5f, true, 1.0f, 0.0f);
        animation.setInterpolator(new AccelerateInterpolator());
        animation.setStartTime(currentAnimationTimeMillis());
        return animation;
    }

    public static Animation makeInChildBottomAnimation(Context context)
    {
        AnimationSet set = new AnimationSet(true);

        TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f,
                                                              Animation.RELATIVE_TO_PARENT, 0.5f, Animation.ABSOLUTE, 0.0f);
        translate.setDuration(SHORT_ANIMATION_TIME);
        set.addAnimation(translate);

        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(SHORT_ANIMATION_TIME);
        set.addAnimation(alpha);

        set.setInterpolator(new AccelerateInterpolator());
        set.setStartTime(currentAnimationTimeMillis());
        return set;
    }

    private static Animation slide(float fromX, float toX, boolean withAlpha, float fromAlpha, float toAlpha)
    {
        AnimationSet set = new AnimationSet(true);

        TranslateAnimation translate = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, fromX, Animation.RELATIVE_TO_PARENT, toX,
                                                              Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f);
        translate.setDuration(MEDIUM_ANIMATION_TIME);
        set.addAnimation(translate);

        if (withAlpha)
        {
            AlphaAnimation alpha = new AlphaAnimation(fromAlpha, toAlpha);
            alpha.setDuration(MEDIUM_ANIMATION_TIME);
            set.addAnimation(alpha);
        }

        return set;
    }

    private static Animation frameworkAnimation(int id)
    {
        switch (id)
        {
            case ANIM_FADE_IN:
            {
                AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
                animation.setInterpolator(new DecelerateInterpolator());
                animation.setDuration(LONG_ANIMATION_TIME);
                return animation;
            }
            case ANIM_FADE_OUT:
            {
                AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
                animation.setInterpolator(new AccelerateInterpolator());
                animation.setDuration(LONG_ANIMATION_TIME);
                return animation;
            }
            case ANIM_SLIDE_IN_LEFT:
                return slide(-0.5f, 0.0f, true, 0.0f, 1.0f);
            case ANIM_SLIDE_OUT_RIGHT:
                return slide(0.0f, 0.5f, true, 1.0f, 0.0f);
            default:
                return null;
        }
    }

    private static Interpolator frameworkInterpolator(int id)
    {
        switch (id)
        {
            case ANIM_LINEAR_INTERPOLATOR:
            case INTERPOLATOR_LINEAR:
                return new LinearInterpolator();
            case ANIM_ACCELERATE_INTERPOLATOR:
            case INTERPOLATOR_ACCELERATE_QUAD:
                return new AccelerateInterpolator();
            case ANIM_DECELERATE_INTERPOLATOR:
            case INTERPOLATOR_DECELERATE_QUAD:
                return new DecelerateInterpolator();
            case INTERPOLATOR_ACCELERATE_CUBIC:
                return new AccelerateInterpolator(1.5f);
            case INTERPOLATOR_DECELERATE_CUBIC:
                return new DecelerateInterpolator(1.5f);
            case INTERPOLATOR_ACCELERATE_QUINT:
                return new AccelerateInterpolator(2.5f);
            case INTERPOLATOR_DECELERATE_QUINT:
                return new DecelerateInterpolator(2.5f);
            case ANIM_ACCELERATE_DECELERATE_INTERPOLATOR:
            case INTERPOLATOR_ACCELERATE_DECELERATE:
                return new AccelerateDecelerateInterpolator();
            case ANIM_ANTICIPATE_INTERPOLATOR:
            case INTERPOLATOR_ANTICIPATE:
                return new AnticipateInterpolator();
            case ANIM_OVERSHOOT_INTERPOLATOR:
            case INTERPOLATOR_OVERSHOOT:
                return new OvershootInterpolator();
            case ANIM_ANTICIPATE_OVERSHOOT_INTERPOLATOR:
            case INTERPOLATOR_ANTICIPATE_OVERSHOOT:
                return new AnticipateOvershootInterpolator();
            case ANIM_BOUNCE_INTERPOLATOR:
            case INTERPOLATOR_BOUNCE:
                return new BounceInterpolator();
            case ANIM_CYCLE_INTERPOLATOR:
            case INTERPOLATOR_CYCLE:
                return new CycleInterpolator(1.0f);
            case INTERPOLATOR_FAST_OUT_SLOW_IN:
                return new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
            case INTERPOLATOR_LINEAR_OUT_SLOW_IN:
                return new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
            case INTERPOLATOR_FAST_OUT_LINEAR_IN:
                return new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
            case INTERPOLATOR_FAST_OUT_EXTRA_SLOW_IN:
                return new PathInterpolator(FAST_OUT_EXTRA_SLOW_IN_PATH);
            default:
                return null;
        }
    }

    public static Interpolator loadInterpolator(Context context, int id) throws Resources.NotFoundException
    {
        return loadInterpolator(context, context.getResources(), context.getTheme(), id);
    }

    public static Interpolator loadInterpolator(Resources resources, Resources.Theme theme, int id) throws Resources.NotFoundException
    {
        return loadInterpolator(null, resources, theme, id);
    }

    private static Interpolator loadInterpolator(Context context, Resources resources, Resources.Theme theme, int id)
        throws Resources.NotFoundException
    {
        Interpolator builtin = frameworkInterpolator(id);
        if (builtin != null) return builtin;

        XmlResourceParser parser = resources.getXml(id);

        try
        {
            Interpolator interpolator = createInterpolatorFromXml(context, parser);
            if (interpolator != null) return interpolator;

            Log.w(TAG, "interpolator resource 0x" + Integer.toHexString(id) + " is empty");
        }
        catch (Exception failure)
        {
            Log.w(TAG, "could not inflate interpolator 0x" + Integer.toHexString(id) + ": " + failure);
        }
        finally
        {
            parser.close();
        }

        return new LinearInterpolator();
    }

    private static Interpolator createInterpolatorFromXml(Context context, XmlPullParser parser) throws Exception
    {
        Interpolator interpolator = null;

        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT)
        {
            if (type != XmlPullParser.START_TAG) continue;

            AttributeSet attributes = Xml.asAttributeSet(parser);
            String name = parser.getName();

            if (name.equals("linearInterpolator"))
            {
                interpolator = new LinearInterpolator();
            }
            else if (name.equals("accelerateInterpolator"))
            {
                interpolator = new AccelerateInterpolator(context, attributes);
            }
            else if (name.equals("decelerateInterpolator"))
            {
                interpolator = new DecelerateInterpolator(context, attributes);
            }
            else if (name.equals("accelerateDecelerateInterpolator"))
            {
                interpolator = new AccelerateDecelerateInterpolator();
            }
            else if (name.equals("cycleInterpolator"))
            {
                interpolator = new CycleInterpolator(context, attributes);
            }
            else if (name.equals("anticipateInterpolator"))
            {
                interpolator = new AnticipateInterpolator(context, attributes);
            }
            else if (name.equals("overshootInterpolator"))
            {
                interpolator = new OvershootInterpolator(context, attributes);
            }
            else if (name.equals("anticipateOvershootInterpolator"))
            {
                interpolator = new AnticipateOvershootInterpolator(context, attributes);
            }
            else if (name.equals("bounceInterpolator"))
            {
                interpolator = new BounceInterpolator();
            }
            else if (name.equals("pathInterpolator"))
            {
                interpolator = new PathInterpolator(context, attributes);
            }
            else
            {
                Log.w(TAG, "unknown interpolator element <" + name + ">, so it falls back to linear");
                interpolator = new LinearInterpolator();
            }
        }

        return interpolator;
    }
}
