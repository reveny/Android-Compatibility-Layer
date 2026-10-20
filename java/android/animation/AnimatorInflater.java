package android.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

public class AnimatorInflater
{
    private AnimatorInflater()
    {
    }

    public static Animator loadAnimator(Context context, int resourceId) throws Resources.NotFoundException
    {
        if (context == null) return null;

        return loadAnimator(context.getResources(), context.getTheme(), resourceId);
    }

    public static Animator loadAnimator(Resources resources, Resources.Theme theme, int resourceId)
        throws Resources.NotFoundException
    {
        if (resources == null) return null;

        XmlResourceParser parser = resources.getXml(resourceId);

        if (parser == null) return null;

        try
        {
            return build(parser);
        }
        catch (Exception failure)
        {
            throw new Resources.NotFoundException("could not inflate animator 0x" + Integer.toHexString(resourceId));
        }
        finally
        {
            parser.close();
        }
    }

    private static Animator build(XmlResourceParser parser) throws Exception
    {
        int event = parser.next();

        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) event = parser.next();

        if (event != XmlPullParser.START_TAG) return null;

        String name = parser.getName();
        AttributeSet attributes = Xml.asAttributeSet(parser);

        if ("set".equals(name))
        {
            AnimatorSet set = new AnimatorSet();
            java.util.ArrayList<Animator> children = new java.util.ArrayList<>();

            int depth = parser.getDepth();

            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) break;
                if (event != XmlPullParser.START_TAG) continue;

                Animator child = objectAnimatorFrom(parser.getName(), Xml.asAttributeSet(parser));
                if (child != null) children.add(child);
            }

            set.playTogether(children.toArray(new Animator[0]));
            return set;
        }

        return objectAnimatorFrom(name, attributes);
    }

    private static Animator objectAnimatorFrom(String name, AttributeSet attributes)
    {
        if (!"objectAnimator".equals(name) && !"animator".equals(name)) return null;

        float from = attributes.getAttributeFloatValue(null, "valueFrom", 0.0f);
        float to = attributes.getAttributeFloatValue(null, "valueTo", 1.0f);
        int duration = attributes.getAttributeIntValue(null, "duration", 300);

        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(duration);

        return animator;
    }
}
