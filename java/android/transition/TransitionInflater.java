package android.transition;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.ViewGroup;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

public class TransitionInflater
{
    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = { Context.class, AttributeSet.class };
    private static final HashMap<String, Constructor> sConstructors = new HashMap<>();

    private final Context mContext;

    private TransitionInflater(Context context)
    {
        mContext = context;
    }

    public static TransitionInflater from(Context context)
    {
        return new TransitionInflater(context);
    }

    public Transition inflateTransition(int resource)
    {
        XmlResourceParser parser = mContext.getResources().getXml(resource);

        try
        {
            return createTransitionFromXml(parser, parser, null);
        }
        catch (XmlPullParserException exception)
        {
            throw new InflateException(exception.getMessage(), exception);
        }
        catch (IOException exception)
        {
            throw new InflateException(parser.getPositionDescription() + ": " + exception.getMessage(), exception);
        }
        finally
        {
            parser.close();
        }
    }

    public TransitionManager inflateTransitionManager(int resource, ViewGroup sceneRoot)
    {
        XmlResourceParser parser = mContext.getResources().getXml(resource);

        try
        {
            return createTransitionManagerFromXml(parser, parser, sceneRoot);
        }
        catch (XmlPullParserException exception)
        {
            throw new InflateException(exception.getMessage(), exception);
        }
        catch (IOException exception)
        {
            throw new InflateException(parser.getPositionDescription() + ": " + exception.getMessage(), exception);
        }
        finally
        {
            parser.close();
        }
    }

    private Transition createTransitionFromXml(XmlPullParser parser, AttributeSet attributes, Transition parent)
        throws XmlPullParserException, IOException
    {
        Transition transition = null;

        int type;
        int depth = parser.getDepth();

        TransitionSet transitionSet = parent instanceof TransitionSet ? (TransitionSet)parent : null;

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT)
        {
            if (type != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("fade".equals(name)) transition = new Fade(mContext, attributes);
            else if ("changeBounds".equals(name)) transition = new ChangeBounds(mContext, attributes);
            else if ("slide".equals(name)) transition = new Slide(mContext, attributes);
            else if ("explode".equals(name)) transition = new Explode(mContext, attributes);
            else if ("changeImageTransform".equals(name)) transition = new ChangeImageTransform(mContext, attributes);
            else if ("changeTransform".equals(name)) transition = new ChangeTransform(mContext, attributes);
            else if ("changeClipBounds".equals(name)) transition = new ChangeClipBounds(mContext, attributes);
            else if ("autoTransition".equals(name)) transition = new AutoTransition(mContext, attributes);
            else if ("recolor".equals(name)) transition = new ChangeBounds(mContext, attributes);
            else if ("changeScroll".equals(name)) transition = new ChangeScroll(mContext, attributes);
            else if ("transitionSet".equals(name)) transition = new TransitionSet(mContext, attributes);
            else if ("transition".equals(name)) transition = (Transition)createCustom(attributes, Transition.class, "transition");
            else if ("targets".equals(name)) getTargetIds(parser, attributes, parent);
            else if ("arcMotion".equals(name)) setPathMotion(parent, new ArcMotion(mContext, attributes));
            else if ("pathMotion".equals(name)) setPathMotion(parent, (PathMotion)createCustom(attributes, PathMotion.class, "pathMotion"));
            else if ("patternPathMotion".equals(name)) setPathMotion(parent, new PatternPathMotion(mContext, attributes));
            else throw new RuntimeException("Unknown scene name: " + parser.getName());

            if (transition == null) continue;

            createTransitionFromXml(parser, attributes, transition);

            if (transitionSet != null)
            {
                transitionSet.addTransition(transition);
                transition = null;
            }
            else if (parent != null)
            {
                throw new InflateException("Could not add transition to another transition.");
            }
        }

        return transition;
    }

    private static void setPathMotion(Transition parent, PathMotion pathMotion)
    {
        if (parent == null) throw new RuntimeException("Invalid use of pathMotion element");

        parent.setPathMotion(pathMotion);
    }

    private Object createCustom(AttributeSet attributes, Class expectedType, String tag)
    {
        String className = attributes.getAttributeValue(null, "class");
        if (className == null) throw new InflateException(tag + " tag must have a 'class' attribute");

        try
        {
            synchronized (sConstructors)
            {
                Constructor constructor = sConstructors.get(className);
                if (constructor == null)
                {
                    Class<?> type = mContext.getClassLoader().loadClass(className).asSubclass(expectedType);
                    constructor = type.getConstructor(CONSTRUCTOR_SIGNATURE);
                    constructor.setAccessible(true);
                    sConstructors.put(className, constructor);
                }

                return constructor.newInstance(mContext, attributes);
            }
        }
        catch (ReflectiveOperationException | ClassCastException exception)
        {
            throw new InflateException("Could not instantiate " + expectedType + " class " + className, exception);
        }
    }

    private void getTargetIds(XmlPullParser parser, AttributeSet attributes, Transition transition)
        throws XmlPullParserException, IOException
    {
        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT)
        {
            if (type != XmlPullParser.START_TAG) continue;

            String name = parser.getName();
            if (!"target".equals(name)) throw new RuntimeException("Unknown scene name: " + parser.getName());
            if (transition == null) continue;

            int id = TransitionAttributes.getResource(attributes, "targetId", 0);
            String targetName;
            String className;

            if (id != 0)
            {
                transition.addTarget(id);
            }
            else if ((id = TransitionAttributes.getResource(attributes, "excludeId", 0)) != 0)
            {
                transition.excludeTarget(id, true);
            }
            else if ((targetName = TransitionAttributes.getString(attributes, "targetName")) != null)
            {
                transition.addTarget(targetName);
            }
            else if ((targetName = TransitionAttributes.getString(attributes, "excludeName")) != null)
            {
                transition.excludeTarget(targetName, true);
            }
            else if ((className = TransitionAttributes.getString(attributes, "excludeClass")) != null)
            {
                transition.excludeTarget(loadClass(className), true);
            }
            else if ((className = TransitionAttributes.getString(attributes, "targetClass")) != null)
            {
                transition.addTarget(loadClass(className));
            }
        }
    }

    private Class loadClass(String className)
    {
        try
        {
            return mContext.getClassLoader().loadClass(className.trim());
        }
        catch (ClassNotFoundException exception)
        {
            throw new RuntimeException("Could not create " + className, exception);
        }
    }

    private TransitionManager createTransitionManagerFromXml(XmlPullParser parser, AttributeSet attributes, ViewGroup sceneRoot)
        throws XmlPullParserException, IOException
    {
        int type;
        int depth = parser.getDepth();
        TransitionManager transitionManager = null;

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT)
        {
            if (type != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("transitionManager".equals(name))
            {
                transitionManager = new TransitionManager();
            }
            else if ("transition".equals(name) && transitionManager != null)
            {
                loadTransition(attributes, sceneRoot, transitionManager);
            }
            else
            {
                throw new RuntimeException("Unknown scene name: " + parser.getName());
            }
        }

        return transitionManager;
    }

    private void loadTransition(AttributeSet attributes, ViewGroup sceneRoot, TransitionManager transitionManager)
    {
        int transitionId = TransitionAttributes.getResource(attributes, "transition", -1);
        int fromId = TransitionAttributes.getResource(attributes, "fromScene", -1);
        int toId = TransitionAttributes.getResource(attributes, "toScene", -1);

        Scene fromScene = fromId < 0 ? null : Scene.getSceneForLayout(sceneRoot, fromId, mContext);
        Scene toScene = toId < 0 ? null : Scene.getSceneForLayout(sceneRoot, toId, mContext);

        if (transitionId < 0) return;

        Transition transition = inflateTransition(transitionId);
        if (transition == null) return;

        if (toScene == null) throw new RuntimeException("No toScene for transition ID " + transitionId);

        if (fromScene == null) transitionManager.setTransition(toScene, transition);
        else transitionManager.setTransition(fromScene, toScene, transition);
    }
}
