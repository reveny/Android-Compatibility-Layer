package android.graphics;

public class Point
{
    public int x;
    public int y;

    public Point()
    {
    }

    public Point(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Point(Point source)
    {
        x = source.x;
        y = source.y;
    }

    public void set(int newX, int newY)
    {
        x = newX;
        y = newY;
    }

    public final void negate()
    {
        x = -x;
        y = -y;
    }

    public final void offset(int deltaX, int deltaY)
    {
        x += deltaX;
        y += deltaY;
    }

    public final boolean equals(int otherX, int otherY)
    {
        return x == otherX && y == otherY;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Point)) return false;

        Point point = (Point)other;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode()
    {
        return 31 * x + y;
    }

    @Override
    public String toString()
    {
        return "Point(" + x + ", " + y + ")";
    }
}
