package android.graphics;

import java.util.ArrayList;
import java.util.List;

public class Region
{
    public enum Op
    {
        DIFFERENCE,
        INTERSECT,
        UNION,
        XOR,
        REVERSE_DIFFERENCE,
        REPLACE
    }

    private final List<Rect> mParts = new ArrayList<>();

    public Region()
    {
    }

    public Region(Region region)
    {
        setTo(region);
    }

    public Region(Rect rect)
    {
        if (rect != null) addPart(rect.left, rect.top, rect.right, rect.bottom);
    }

    public Region(int left, int top, int right, int bottom)
    {
        addPart(left, top, right, bottom);
    }

    Rect[] parts()
    {
        return mParts.toArray(new Rect[0]);
    }

    private void addPart(int left, int top, int right, int bottom)
    {
        if (right <= left || bottom <= top) return;

        mParts.add(new Rect(left, top, right, bottom));
    }

    private void setTo(Region region)
    {
        mParts.clear();

        if (region == null) return;

        for (Rect part : region.mParts) mParts.add(new Rect(part));
    }

    public void setEmpty()
    {
        mParts.clear();
    }

    public boolean set(Region region)
    {
        setTo(region);
        return !mParts.isEmpty();
    }

    public boolean set(Rect rect)
    {
        mParts.clear();

        if (rect != null) addPart(rect.left, rect.top, rect.right, rect.bottom);

        return !mParts.isEmpty();
    }

    public boolean set(int left, int top, int right, int bottom)
    {
        mParts.clear();
        addPart(left, top, right, bottom);

        return !mParts.isEmpty();
    }

    public boolean setPath(Path path, Region clip)
    {
        mParts.clear();

        if (path == null) return false;

        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        addPart((int)Math.floor(bounds.left), (int)Math.floor(bounds.top), (int)Math.ceil(bounds.right),
                (int)Math.ceil(bounds.bottom));

        if (clip != null) op(clip, Op.INTERSECT);

        return !mParts.isEmpty();
    }

    public boolean isEmpty()
    {
        return mParts.isEmpty();
    }

    public boolean isRect()
    {
        return mParts.size() == 1;
    }

    public boolean isComplex()
    {
        return mParts.size() > 1;
    }

    public Rect getBounds()
    {
        Rect bounds = new Rect();
        getBounds(bounds);

        return bounds;
    }

    public boolean getBounds(Rect into)
    {
        if (into == null) return !mParts.isEmpty();

        if (mParts.isEmpty())
        {
            into.setEmpty();
            return false;
        }

        Rect first = mParts.get(0);
        into.set(first.left, first.top, first.right, first.bottom);

        for (int index = 1; index < mParts.size(); index++)
        {
            Rect part = mParts.get(index);

            if (part.left < into.left) into.left = part.left;
            if (part.top < into.top) into.top = part.top;
            if (part.right > into.right) into.right = part.right;
            if (part.bottom > into.bottom) into.bottom = part.bottom;
        }

        return true;
    }

    public Path getBoundaryPath()
    {
        Path path = new Path();

        for (Rect part : mParts) path.addRect(part.left, part.top, part.right, part.bottom, Path.Direction.CW);

        return path;
    }

    public boolean getBoundaryPath(Path into)
    {
        if (into == null) return !mParts.isEmpty();

        into.reset();

        for (Rect part : mParts) into.addRect(part.left, part.top, part.right, part.bottom, Path.Direction.CW);

        return !mParts.isEmpty();
    }

    public boolean contains(int x, int y)
    {
        for (Rect part : mParts)
        {
            if (x >= part.left && x < part.right && y >= part.top && y < part.bottom) return true;
        }

        return false;
    }

    public boolean quickContains(Rect rect)
    {
        return rect != null && quickContains(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean quickContains(int left, int top, int right, int bottom)
    {
        for (Rect part : mParts)
        {
            if (left >= part.left && top >= part.top && right <= part.right && bottom <= part.bottom) return true;
        }

        return false;
    }

    public boolean quickReject(Rect rect)
    {
        return rect == null || quickReject(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean quickReject(int left, int top, int right, int bottom)
    {
        for (Rect part : mParts)
        {
            if (intersects(part, left, top, right, bottom)) return false;
        }

        return true;
    }

    public boolean quickReject(Region region)
    {
        if (region == null) return true;

        for (Rect part : region.mParts)
        {
            if (!quickReject(part)) return false;
        }

        return true;
    }

    public void translate(int dx, int dy)
    {
        translate(dx, dy, this);
    }

    public void translate(int dx, int dy, Region into)
    {
        if (into == null) return;

        List<Rect> moved = new ArrayList<>();
        for (Rect part : mParts) moved.add(new Rect(part.left + dx, part.top + dy, part.right + dx, part.bottom + dy));

        into.mParts.clear();
        into.mParts.addAll(moved);
    }

    public boolean union(Rect rect)
    {
        return op(rect, Op.UNION);
    }

    public boolean op(Rect rect, Op operation)
    {
        Region other = new Region(rect);
        return op(other, operation);
    }

    public boolean op(int left, int top, int right, int bottom, Op operation)
    {
        return op(new Region(left, top, right, bottom), operation);
    }

    public boolean op(Region region, Op operation)
    {
        return op(this, region, operation);
    }

    public boolean op(Rect rect, Region region, Op operation)
    {
        return op(new Region(rect), region, operation);
    }

    public boolean op(Region first, Region second, Op operation)
    {
        List<Rect> left = copyOf(first);
        List<Rect> right = copyOf(second);
        List<Rect> result;

        if (operation == Op.REPLACE)
        {
            result = right;
        }
        else if (operation == Op.UNION)
        {
            result = left;
            for (Rect part : right) subtractInto(result, part, result);
            result.addAll(right);
        }
        else if (operation == Op.INTERSECT)
        {
            result = new ArrayList<>();

            for (Rect a : left)
            {
                for (Rect b : right)
                {
                    if (!intersects(a, b.left, b.top, b.right, b.bottom)) continue;

                    result.add(new Rect(Math.max(a.left, b.left), Math.max(a.top, b.top), Math.min(a.right, b.right),
                                        Math.min(a.bottom, b.bottom)));
                }
            }
        }
        else if (operation == Op.DIFFERENCE)
        {
            result = left;
            for (Rect part : right) subtractInto(result, part, result);
        }
        else if (operation == Op.REVERSE_DIFFERENCE)
        {
            result = right;
            for (Rect part : left) subtractInto(result, part, result);
        }
        else
        {
            List<Rect> onlyLeft = copyOf(first);
            for (Rect part : right) subtractInto(onlyLeft, part, onlyLeft);

            List<Rect> onlyRight = copyOf(second);
            for (Rect part : left) subtractInto(onlyRight, part, onlyRight);

            result = onlyLeft;
            result.addAll(onlyRight);
        }

        mParts.clear();
        for (Rect part : result)
        {
            if (part.right > part.left && part.bottom > part.top) mParts.add(part);
        }

        return !mParts.isEmpty();
    }

    private static List<Rect> copyOf(Region region)
    {
        List<Rect> copy = new ArrayList<>();

        if (region != null)
        {
            for (Rect part : region.mParts) copy.add(new Rect(part));
        }

        return copy;
    }

    private static void subtractInto(List<Rect> parts, Rect cut, List<Rect> into)
    {
        List<Rect> kept = new ArrayList<>();

        for (Rect part : parts)
        {
            if (!intersects(part, cut.left, cut.top, cut.right, cut.bottom))
            {
                kept.add(part);
                continue;
            }

            if (part.top < cut.top) kept.add(new Rect(part.left, part.top, part.right, cut.top));
            if (part.bottom > cut.bottom) kept.add(new Rect(part.left, cut.bottom, part.right, part.bottom));

            int top = Math.max(part.top, cut.top);
            int bottom = Math.min(part.bottom, cut.bottom);

            if (bottom > top)
            {
                if (part.left < cut.left) kept.add(new Rect(part.left, top, cut.left, bottom));
                if (part.right > cut.right) kept.add(new Rect(cut.right, top, part.right, bottom));
            }
        }

        into.clear();
        into.addAll(kept);
    }

    private static boolean intersects(Rect part, int left, int top, int right, int bottom)
    {
        return part.left < right && left < part.right && part.top < bottom && top < part.bottom;
    }

    @Override
    public String toString()
    {
        return "Region(" + getBounds() + (isComplex() ? " and " + (mParts.size() - 1) + " more" : "") + ")";
    }
}
