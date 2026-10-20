package android.graphics;

public final class Insets
{
    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    private Insets(int left, int top, int right, int bottom)
    {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static Insets of(int left, int top, int right, int bottom)
    {
        if (left == 0 && top == 0 && right == 0 && bottom == 0) return NONE;
        return new Insets(left, top, right, bottom);
    }

    public static Insets of(Rect rect)
    {
        return rect == null ? NONE : of(rect.left, rect.top, rect.right, rect.bottom);
    }

    public Rect toRect()
    {
        return new Rect(left, top, right, bottom);
    }

    public static Insets add(Insets first, Insets second)
    {
        return of(first.left + second.left, first.top + second.top, first.right + second.right, first.bottom + second.bottom);
    }

    public static Insets max(Insets first, Insets second)
    {
        return of(Math.max(first.left, second.left), Math.max(first.top, second.top), Math.max(first.right, second.right),
                  Math.max(first.bottom, second.bottom));
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Insets)) return false;

        Insets that = (Insets)other;
        return left == that.left && top == that.top && right == that.right && bottom == that.bottom;
    }

    @Override
    public int hashCode()
    {
        return ((left * 31 + top) * 31 + right) * 31 + bottom;
    }

    @Override
    public String toString()
    {
        return "Insets{left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + "}";
    }
}
