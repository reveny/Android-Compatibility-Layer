package android.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlBlock;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.PathParser;
import android.util.TypedValue;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class AnimatorInflater
{
    private static final String TAG = "AnimatorInflater";
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    private static final int TOGETHER = 0;
    private static final int SEQUENTIALLY = 1;

    private static final int VALUE_TYPE_FLOAT = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_PATH = 2;
    private static final int VALUE_TYPE_COLOR = 3;
    private static final int VALUE_TYPE_UNDEFINED = 4;

    public AnimatorInflater()
    {
    }

    public static Animator loadAnimator(Context context, int id) throws Resources.NotFoundException
    {
        if (context == null) return null;

        return loadAnimator(context.getResources(), context.getTheme(), id);
    }

    public static Animator loadAnimator(Resources resources, Resources.Theme theme, int id) throws Resources.NotFoundException
    {
        if (resources == null) return null;

        XmlResourceParser parser = null;

        try
        {
            parser = resources.getAnimation(id);
            if (parser == null) return null;

            return createAnimatorFromXml(resources, theme, parser);
        }
        catch (XmlPullParserException | IOException | RuntimeException failure)
        {
            if (failure instanceof Resources.NotFoundException) throw (Resources.NotFoundException)failure;

            Resources.NotFoundException wrapped = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(id));
            wrapped.initCause(failure);
            throw wrapped;
        }
        finally
        {
            if (parser != null) parser.close();
        }
    }

    public static StateListAnimator loadStateListAnimator(Context context, int id) throws Resources.NotFoundException
    {
        Resources resources = context.getResources();
        XmlResourceParser parser = null;

        try
        {
            parser = resources.getAnimation(id);
            if (parser == null) return null;

            return createStateListAnimatorFromXml(context, resources, context.getTheme(), parser);
        }
        catch (XmlPullParserException | IOException | RuntimeException failure)
        {
            if (failure instanceof Resources.NotFoundException) throw (Resources.NotFoundException)failure;

            Resources.NotFoundException wrapped = new Resources.NotFoundException("Can't load state list animator resource ID #0x" +
                                                                                  Integer.toHexString(id));
            wrapped.initCause(failure);
            throw wrapped;
        }
        finally
        {
            if (parser != null) parser.close();
        }
    }

    private static boolean advanceToStart(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int event = parser.getEventType();

        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) event = parser.next();

        return event == XmlPullParser.START_TAG;
    }

    private static StateListAnimator createStateListAnimatorFromXml(Context context, Resources resources, Resources.Theme theme,
                                                                    XmlPullParser parser) throws XmlPullParserException, IOException
    {
        StateListAnimator stateListAnimator = new StateListAnimator();

        if (!advanceToStart(parser)) return stateListAnimator;

        int depth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if (!"item".equals(parser.getName()))
            {
                skipElement(parser);
                continue;
            }

            AttributeSet attributes = Xml.asAttributeSet(parser);
            int[] states = readStateSet(attributes);
            int animatorId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "animator", 0);
            Animator animator = null;

            if (animatorId != 0)
            {
                animator = loadAnimator(resources, theme, animatorId);
                skipElement(parser);
            }
            else
            {
                int itemDepth = parser.getDepth();

                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
                {
                    if (event == XmlPullParser.END_TAG && parser.getDepth() <= itemDepth) break;
                    if (event != XmlPullParser.START_TAG) continue;

                    Animator child = parseElement(resources, theme, parser);
                    if (animator == null) animator = child;
                }
            }

            if (animator == null) throw new Resources.NotFoundException("animation state item must have a valid animation");

            stateListAnimator.addState(states, animator);
        }

        return stateListAnimator;
    }

    private static int[] readStateSet(AttributeSet attributes)
    {
        ArrayList<Integer> collected = new ArrayList<>();

        for (int index = 0; index < attributes.getAttributeCount(); index++)
        {
            int name = attributes.getAttributeNameResource(index);
            if (name == 0) continue;

            if (attributes instanceof XmlBlock.Parser)
            {
                int[] packed = ((XmlBlock.Parser)attributes).getAttributeTypedValue(index);
                if (packed == null || packed[0] != TypedValue.TYPE_INT_BOOLEAN) continue;
            }
            else
            {
                String value = attributes.getAttributeValue(index);
                if (!"true".equals(value) && !"false".equals(value)) continue;
            }

            collected.add(attributes.getAttributeBooleanValue(index, false) ? name : -name);
        }

        int[] states = new int[collected.size()];
        for (int index = 0; index < states.length; index++) states[index] = collected.get(index);

        return states;
    }

    private static Animator createAnimatorFromXml(Resources resources, Resources.Theme theme, XmlPullParser parser)
        throws XmlPullParserException, IOException
    {
        if (!advanceToStart(parser)) return null;

        return parseElement(resources, theme, parser);
    }

    private static void skipElement(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int depth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) return;
        }
    }

    private static Animator parseElement(Resources resources, Resources.Theme theme, XmlPullParser parser)
        throws XmlPullParserException, IOException
    {
        String name = parser.getName();
        AttributeSet attributes = Xml.asAttributeSet(parser);

        if ("set".equals(name)) return parseSet(resources, theme, parser, attributes);

        if ("objectAnimator".equals(name) || "animator".equals(name))
        {
            return parseValueAnimator(resources, theme, parser, attributes, "objectAnimator".equals(name));
        }

        Log.w(TAG, "Unknown animator name: " + name);
        skipElement(parser);

        return null;
    }

    private static AnimatorSet parseSet(Resources resources, Resources.Theme theme, XmlPullParser parser, AttributeSet attributes)
        throws XmlPullParserException, IOException
    {
        AnimatorSet set = new AnimatorSet();
        int ordering = integer(attributes, "ordering", TOGETHER);
        ArrayList<Animator> children = new ArrayList<>();

        int depth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) break;
            if (event != XmlPullParser.START_TAG) continue;

            Animator child = parseElement(resources, theme, parser);
            if (child != null) children.add(child);
        }

        if (!children.isEmpty())
        {
            Animator[] array = children.toArray(new Animator[0]);

            if (ordering == TOGETHER) set.playTogether(array);
            else set.playSequentially(array);
        }

        return set;
    }

    private static ValueAnimator parseValueAnimator(Resources resources, Resources.Theme theme, XmlPullParser parser,
                                                    AttributeSet attributes, boolean isObjectAnimator)
        throws XmlPullParserException, IOException
    {
        ValueAnimator animator = isObjectAnimator ? new ObjectAnimator() : new ValueAnimator();

        animator.setDuration(Math.max(0, integer(attributes, "duration", 300)));
        animator.setStartDelay(Math.max(0, integer(attributes, "startOffset", 0)));
        animator.setRepeatCount(integer(attributes, "repeatCount", 0));
        animator.setRepeatMode(integer(attributes, "repeatMode", ValueAnimator.RESTART));

        TimeInterpolator interpolator = interpolator(resources, theme, attributes);
        if (interpolator != null) animator.setInterpolator(interpolator);

        int valueType = integer(attributes, "valueType", VALUE_TYPE_UNDEFINED);
        String propertyName = attributes.getAttributeValue(ANDROID_NAMESPACE, "propertyName");

        if (isObjectAnimator)
        {
            ObjectAnimator objectAnimator = (ObjectAnimator)animator;
            String pathData = attributes.getAttributeValue(ANDROID_NAMESPACE, "pathData");

            if (pathData != null)
            {
                String xName = attributes.getAttributeValue(ANDROID_NAMESPACE, "propertyXName");
                String yName = attributes.getAttributeValue(ANDROID_NAMESPACE, "propertyYName");

                if (valueType == VALUE_TYPE_PATH || valueType == VALUE_TYPE_UNDEFINED) valueType = VALUE_TYPE_FLOAT;

                if (xName == null && yName == null)
                {
                    throw new XmlPullParserException(attributes.getPositionDescription() +
                                                     " propertyXName or propertyYName is needed for PathData");
                }

                Path path = PathParser.createPathFromPathData(pathData);
                float error = 0.5f * resources.getDisplayMetrics().density;
                PathKeyframes keyframes = KeyframeSet.ofPath(path, error);
                ArrayList<PropertyValuesHolder> holders = new ArrayList<>();

                if (xName != null)
                {
                    holders.add(valueType == VALUE_TYPE_FLOAT ? PropertyValuesHolder.ofXFloat(xName, null, keyframes, true)
                                                               : PropertyValuesHolder.ofXInt(xName, null, keyframes, true));
                }

                if (yName != null)
                {
                    holders.add(valueType == VALUE_TYPE_FLOAT ? PropertyValuesHolder.ofXFloat(yName, null, keyframes, false)
                                                               : PropertyValuesHolder.ofXInt(yName, null, keyframes, false));
                }

                objectAnimator.setValues(holders.toArray(new PropertyValuesHolder[0]));
            }
            else
            {
                PropertyValuesHolder holder = valuesHolder(resources, theme, attributes, valueType, propertyName);
                if (holder != null) objectAnimator.setValues(holder);

                objectAnimator.setPropertyName(propertyName);
            }
        }
        else
        {
            PropertyValuesHolder holder = valuesHolder(resources, theme, attributes, valueType, "");
            if (holder != null) animator.setValues(holder);
        }

        ArrayList<PropertyValuesHolder> children = new ArrayList<>();
        int depth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if (!"propertyValuesHolder".equals(parser.getName()))
            {
                skipElement(parser);
                continue;
            }

            PropertyValuesHolder holder = parsePropertyValuesHolder(resources, theme, parser);
            if (holder != null) children.add(holder);
        }

        if (!children.isEmpty()) animator.setValues(children.toArray(new PropertyValuesHolder[0]));

        return animator;
    }

    private static PropertyValuesHolder parsePropertyValuesHolder(Resources resources, Resources.Theme theme, XmlPullParser parser)
        throws XmlPullParserException, IOException
    {
        AttributeSet attributes = Xml.asAttributeSet(parser);
        String propertyName = attributes.getAttributeValue(ANDROID_NAMESPACE, "propertyName");
        int valueType = integer(attributes, "valueType", VALUE_TYPE_UNDEFINED);

        ArrayList<Keyframe> keyframes = new ArrayList<>();
        int depth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if (!"keyframe".equals(parser.getName()))
            {
                skipElement(parser);
                continue;
            }

            AttributeSet keyframeAttributes = Xml.asAttributeSet(parser);

            if (valueType == VALUE_TYPE_UNDEFINED)
            {
                TypedValue value = typedValue(resources, theme, keyframeAttributes, "value");
                valueType = value != null && isColorType(value.type) ? VALUE_TYPE_COLOR : VALUE_TYPE_FLOAT;
            }

            Keyframe keyframe = parseKeyframe(resources, theme, keyframeAttributes, valueType);
            if (keyframe != null) keyframes.add(keyframe);

            skipElement(parser);
        }

        if (keyframes.isEmpty()) return valuesHolder(resources, theme, attributes, valueType, propertyName);

        int count = keyframes.size();
        Keyframe firstKeyframe = keyframes.get(0);
        Keyframe lastKeyframe = keyframes.get(count - 1);

        float endFraction = lastKeyframe.getFraction();

        if (endFraction < 1)
        {
            if (endFraction < 0)
            {
                lastKeyframe.setFraction(1);
            }
            else
            {
                keyframes.add(createNewKeyframe(lastKeyframe, 1));
                count++;
            }
        }

        float startFraction = firstKeyframe.getFraction();

        if (startFraction != 0)
        {
            if (startFraction < 0)
            {
                firstKeyframe.setFraction(0);
            }
            else
            {
                keyframes.add(0, createNewKeyframe(firstKeyframe, 0));
                count++;
            }
        }

        Keyframe[] array = keyframes.toArray(new Keyframe[0]);

        for (int index = 0; index < count; index++)
        {
            Keyframe keyframe = array[index];
            if (keyframe.getFraction() >= 0) continue;

            if (index == 0)
            {
                keyframe.setFraction(0);
            }
            else if (index == count - 1)
            {
                keyframe.setFraction(1);
            }
            else
            {
                int startIndex = index;
                int endIndex = index;

                for (int inner = startIndex + 1; inner < count - 1; inner++)
                {
                    if (array[inner].getFraction() >= 0) break;
                    endIndex = inner;
                }

                float gap = array[endIndex + 1].getFraction() - array[startIndex - 1].getFraction();
                distributeKeyframes(array, gap, startIndex, endIndex);
            }
        }

        PropertyValuesHolder holder = PropertyValuesHolder.ofKeyframe(propertyName, array);
        if (valueType == VALUE_TYPE_COLOR) holder.setEvaluator(ArgbEvaluator.getInstance());

        return holder;
    }

    private static Keyframe createNewKeyframe(Keyframe sample, float fraction)
    {
        if (sample.getType() == float.class) return Keyframe.ofFloat(fraction);
        if (sample.getType() == int.class) return Keyframe.ofInt(fraction);

        return Keyframe.ofObject(fraction);
    }

    private static void distributeKeyframes(Keyframe[] keyframes, float gap, int startIndex, int endIndex)
    {
        int count = endIndex - startIndex + 2;
        float increment = gap / count;

        for (int index = startIndex; index <= endIndex; index++) keyframes[index].setFraction(keyframes[index - 1].getFraction() + increment);
    }

    private static Keyframe parseKeyframe(Resources resources, Resources.Theme theme, AttributeSet attributes, int valueType)
    {
        float fraction = floatAttribute(resources, theme, attributes, "fraction", -1);
        TypedValue value = typedValue(resources, theme, attributes, "value");
        boolean hasValue = value != null;

        Keyframe keyframe;

        if (valueType == VALUE_TYPE_INT || valueType == VALUE_TYPE_COLOR)
        {
            keyframe = hasValue ? Keyframe.ofInt(fraction, intValue(resources, value, valueType == VALUE_TYPE_COLOR)) : Keyframe.ofInt(fraction);
        }
        else
        {
            keyframe = hasValue ? Keyframe.ofFloat(fraction, floatValue(resources, value)) : Keyframe.ofFloat(fraction);
        }

        TimeInterpolator interpolator = interpolator(resources, theme, attributes);
        if (interpolator != null) keyframe.setInterpolator(interpolator);

        return keyframe;
    }

    private static PropertyValuesHolder valuesHolder(Resources resources, Resources.Theme theme, AttributeSet attributes, int valueType,
                                                     String propertyName)
    {
        TypedValue from = typedValue(resources, theme, attributes, "valueFrom");
        TypedValue to = typedValue(resources, theme, attributes, "valueTo");

        boolean hasFrom = from != null && from.type != TypedValue.TYPE_NULL;
        boolean hasTo = to != null && to.type != TypedValue.TYPE_NULL;

        if (!hasFrom && !hasTo) return null;

        if (valueType == VALUE_TYPE_UNDEFINED)
        {
            if ((hasFrom && isColorType(from.type)) || (hasTo && isColorType(to.type))) valueType = VALUE_TYPE_COLOR;
            else valueType = VALUE_TYPE_FLOAT;
        }

        if (valueType == VALUE_TYPE_PATH)
        {
            Log.w(TAG, "path morphing for " + propertyName + " is not supported; the animator will not change the path");
            return null;
        }

        PropertyValuesHolder holder;

        if (valueType == VALUE_TYPE_FLOAT)
        {
            if (hasFrom && hasTo) holder = PropertyValuesHolder.ofFloat(propertyName, floatValue(resources, from), floatValue(resources, to));
            else if (hasFrom) holder = PropertyValuesHolder.ofFloat(propertyName, floatValue(resources, from));
            else holder = PropertyValuesHolder.ofFloat(propertyName, floatValue(resources, to));

            return holder;
        }

        boolean color = valueType == VALUE_TYPE_COLOR;

        if (hasFrom && hasTo)
        {
            holder = PropertyValuesHolder.ofInt(propertyName, intValue(resources, from, color), intValue(resources, to, color));
        }
        else if (hasFrom)
        {
            holder = PropertyValuesHolder.ofInt(propertyName, intValue(resources, from, color));
        }
        else
        {
            holder = PropertyValuesHolder.ofInt(propertyName, intValue(resources, to, color));
        }

        if (color) holder.setEvaluator(ArgbEvaluator.getInstance());

        return holder;
    }

    private static boolean isColorType(int type)
    {
        return type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    private static float floatValue(Resources resources, TypedValue value)
    {
        switch (value.type)
        {
            case TypedValue.TYPE_FLOAT:
                return Float.intBitsToFloat(value.data);

            case TypedValue.TYPE_DIMENSION:
                return TypedValue.complexToDimension(value.data, resources.getDisplayMetrics());

            case TypedValue.TYPE_FRACTION:
                return TypedValue.complexToFraction(value.data, 1, 1);

            case TypedValue.TYPE_STRING:
                return parseNumber(value.string == null ? null : value.string.toString(), 0);

            default:
                return value.data;
        }
    }

    private static int intValue(Resources resources, TypedValue value, boolean color)
    {
        if (color && isColorType(value.type)) return value.data;

        switch (value.type)
        {
            case TypedValue.TYPE_FLOAT:
            case TypedValue.TYPE_DIMENSION:
            case TypedValue.TYPE_FRACTION:
            case TypedValue.TYPE_STRING:
                return (int)floatValue(resources, value);

            default:
                return value.data;
        }
    }

    private static float floatAttribute(Resources resources, Resources.Theme theme, AttributeSet attributes, String name, float fallback)
    {
        TypedValue value = typedValue(resources, theme, attributes, name);

        return value == null ? fallback : floatValue(resources, value);
    }

    private static int integer(AttributeSet attributes, String name, int fallback)
    {
        if (attributes instanceof XmlBlock.Parser)
        {
            int[] packed = ((XmlBlock.Parser)attributes).getAttributeTypedValue(ANDROID_NAMESPACE, name);

            if (packed != null && packed[0] >= TypedValue.TYPE_FIRST_INT && packed[0] <= TypedValue.TYPE_LAST_INT) return packed[1];
            if (packed != null && packed[0] == TypedValue.TYPE_FLOAT) return (int)Float.intBitsToFloat(packed[1]);
        }

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null) return fallback;

        switch (text)
        {
            case "infinite": return ValueAnimator.INFINITE;
            case "restart": return ValueAnimator.RESTART;
            case "reverse": return ValueAnimator.REVERSE;
            case "together": return TOGETHER;
            case "sequentially": return SEQUENTIALLY;
            case "floatType": return VALUE_TYPE_FLOAT;
            case "intType": return VALUE_TYPE_INT;
            case "pathType": return VALUE_TYPE_PATH;
            case "colorType": return VALUE_TYPE_COLOR;
            default: break;
        }

        try
        {
            return Integer.decode(text);
        }
        catch (NumberFormatException failure)
        {
            return fallback;
        }
    }

    private static TimeInterpolator interpolator(Resources resources, Resources.Theme theme, AttributeSet attributes)
    {
        int id = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "interpolator", 0);
        if (id == 0) return null;

        try
        {
            return android.view.animation.AnimationUtils.loadInterpolator(resources, theme, id);
        }
        catch (RuntimeException failure)
        {
            Log.w(TAG, "could not load interpolator 0x" + Integer.toHexString(id) + ": " + failure);
            return null;
        }
    }

    private static TypedValue typedValue(Resources resources, Resources.Theme theme, AttributeSet attributes, String name)
    {
        TypedValue value = new TypedValue();

        if (attributes instanceof XmlBlock.Parser)
        {
            int[] packed = ((XmlBlock.Parser)attributes).getAttributeTypedValue(ANDROID_NAMESPACE, name);
            if (packed == null) return null;

            value.type = packed[0];
            value.data = packed[1];

            if (value.type == TypedValue.TYPE_STRING) value.string = attributes.getAttributeValue(ANDROID_NAMESPACE, name);

            if (value.type == TypedValue.TYPE_ATTRIBUTE && theme != null)
            {
                TypedValue resolved = new TypedValue();
                if (!theme.resolveAttribute(value.data, resolved, true)) return null;

                return resolved;
            }

            if (value.type == TypedValue.TYPE_REFERENCE)
            {
                if (value.data == 0) return null;

                try
                {
                    TypedValue resolved = new TypedValue();
                    resources.getValue(value.data, resolved, true);

                    return resolved;
                }
                catch (Resources.NotFoundException failure)
                {
                    return null;
                }
            }

            return value;
        }

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null) return null;

        if (text.startsWith("#"))
        {
            value.type = TypedValue.TYPE_INT_COLOR_ARGB8;
            value.data = parseColor(text);
        }
        else if (text.indexOf('.') >= 0 || !Character.isDigit(text.charAt(text.length() - 1)))
        {
            value.type = TypedValue.TYPE_FLOAT;
            value.data = Float.floatToIntBits(parseNumber(text, 0));
        }
        else
        {
            value.type = TypedValue.TYPE_INT_DEC;
            value.data = (int)parseNumber(text, 0);
        }

        return value;
    }

    private static int parseColor(String text)
    {
        String digits = text.substring(1);

        try
        {
            if (digits.length() == 6) return 0xFF000000 | (int)Long.parseLong(digits, 16);
            if (digits.length() == 8) return (int)Long.parseLong(digits, 16);

            if (digits.length() == 3 || digits.length() == 4)
            {
                int value = 0;

                for (int index = 0; index < digits.length(); index++)
                {
                    int nibble = Character.digit(digits.charAt(index), 16);
                    value = (value << 8) | (nibble << 4) | nibble;
                }

                return digits.length() == 3 ? 0xFF000000 | value : value;
            }
        }
        catch (NumberFormatException failure)
        {
            return 0;
        }

        return 0;
    }

    private static float parseNumber(String text, float fallback)
    {
        if (text == null) return fallback;

        int end = text.length();

        while (end > 0)
        {
            char character = text.charAt(end - 1);
            if ((character >= '0' && character <= '9') || character == '.') break;
            end--;
        }

        if (end == 0) return fallback;

        try
        {
            return Float.parseFloat(text.substring(0, end));
        }
        catch (NumberFormatException failure)
        {
            return fallback;
        }
    }
}
