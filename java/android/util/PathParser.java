package android.util;

import android.graphics.Path;

public class PathParser
{
    public static Path createPathFromPathData(String data)
    {
        Path path = new Path();
        if (!parseInto(data, path)) return null;

        return path;
    }

    public static boolean canParse(String data)
    {
        return parse(data, new CountingSink());
    }

    private static final class CountingSink implements Sink
    {
        @Override
        public void moveTo(float x, float y)
        {
        }

        @Override
        public void lineTo(float x, float y)
        {
        }

        @Override
        public void quadTo(float controlX, float controlY, float x, float y)
        {
        }

        @Override
        public void cubicTo(float firstX, float firstY, float secondX, float secondY, float x, float y)
        {
        }

        @Override
        public void close()
        {
        }
    }

    public static boolean parseInto(String data, Path path)
    {
        if (path == null) return false;

        return parse(data, new PathSink(path));
    }

    interface Sink
    {
        void moveTo(float x, float y);

        void lineTo(float x, float y);

        void quadTo(float controlX, float controlY, float x, float y);

        void cubicTo(float firstX, float firstY, float secondX, float secondY, float x, float y);

        void close();
    }

    private static final class PathSink implements Sink
    {
        private final Path mPath;

        PathSink(Path path)
        {
            mPath = path;
        }

        @Override
        public void moveTo(float x, float y)
        {
            mPath.moveTo(x, y);
        }

        @Override
        public void lineTo(float x, float y)
        {
            mPath.lineTo(x, y);
        }

        @Override
        public void quadTo(float controlX, float controlY, float x, float y)
        {
            mPath.quadTo(controlX, controlY, x, y);
        }

        @Override
        public void cubicTo(float firstX, float firstY, float secondX, float secondY, float x, float y)
        {
            mPath.cubicTo(firstX, firstY, secondX, secondY, x, y);
        }

        @Override
        public void close()
        {
            mPath.close();
        }
    }

    static boolean parse(String data, Sink path)
    {
        if (data == null || path == null) return false;

        Cursor cursor = new Cursor(data);

        float currentX = 0;
        float currentY = 0;
        float startX = 0;
        float startY = 0;
        float lastControlX = 0;
        float lastControlY = 0;

        char previous = 0;
        char command = 0;

        while (true)
        {
            cursor.skipSeparators();
            if (cursor.done()) break;

            char next = cursor.peek();

            if (isCommand(next))
            {
                command = next;
                cursor.advance();
            }
            else if (command == 0)
            {
                return false;
            }
            else if (command == 'M')
            {
                command = 'L';
            }
            else if (command == 'm')
            {
                command = 'l';
            }

            boolean relative = Character.isLowerCase(command);
            char normalised = Character.toUpperCase(command);

            switch (normalised)
            {
                case 'M':
                {
                    float x = cursor.number();
                    float y = cursor.number();
                    if (cursor.failed()) return false;

                    currentX = relative ? currentX + x : x;
                    currentY = relative ? currentY + y : y;
                    startX = currentX;
                    startY = currentY;

                    path.moveTo(currentX, currentY);
                    break;
                }

                case 'L':
                {
                    float x = cursor.number();
                    float y = cursor.number();
                    if (cursor.failed()) return false;

                    currentX = relative ? currentX + x : x;
                    currentY = relative ? currentY + y : y;

                    path.lineTo(currentX, currentY);
                    break;
                }

                case 'H':
                {
                    float x = cursor.number();
                    if (cursor.failed()) return false;

                    currentX = relative ? currentX + x : x;
                    path.lineTo(currentX, currentY);
                    break;
                }

                case 'V':
                {
                    float y = cursor.number();
                    if (cursor.failed()) return false;

                    currentY = relative ? currentY + y : y;
                    path.lineTo(currentX, currentY);
                    break;
                }

                case 'C':
                {
                    float firstX = cursor.number();
                    float firstY = cursor.number();
                    float secondX = cursor.number();
                    float secondY = cursor.number();
                    float endX = cursor.number();
                    float endY = cursor.number();
                    if (cursor.failed()) return false;

                    if (relative)
                    {
                        firstX += currentX;
                        firstY += currentY;
                        secondX += currentX;
                        secondY += currentY;
                        endX += currentX;
                        endY += currentY;
                    }

                    path.cubicTo(firstX, firstY, secondX, secondY, endX, endY);

                    lastControlX = secondX;
                    lastControlY = secondY;
                    currentX = endX;
                    currentY = endY;
                    break;
                }

                case 'S':
                {
                    float secondX = cursor.number();
                    float secondY = cursor.number();
                    float endX = cursor.number();
                    float endY = cursor.number();
                    if (cursor.failed()) return false;

                    if (relative)
                    {
                        secondX += currentX;
                        secondY += currentY;
                        endX += currentX;
                        endY += currentY;
                    }

                    float firstX = currentX;
                    float firstY = currentY;

                    if (previous == 'C' || previous == 'S')
                    {
                        firstX = 2 * currentX - lastControlX;
                        firstY = 2 * currentY - lastControlY;
                    }

                    path.cubicTo(firstX, firstY, secondX, secondY, endX, endY);

                    lastControlX = secondX;
                    lastControlY = secondY;
                    currentX = endX;
                    currentY = endY;
                    break;
                }

                case 'Q':
                {
                    float controlX = cursor.number();
                    float controlY = cursor.number();
                    float endX = cursor.number();
                    float endY = cursor.number();
                    if (cursor.failed()) return false;

                    if (relative)
                    {
                        controlX += currentX;
                        controlY += currentY;
                        endX += currentX;
                        endY += currentY;
                    }

                    path.quadTo(controlX, controlY, endX, endY);

                    lastControlX = controlX;
                    lastControlY = controlY;
                    currentX = endX;
                    currentY = endY;
                    break;
                }

                case 'T':
                {
                    float endX = cursor.number();
                    float endY = cursor.number();
                    if (cursor.failed()) return false;

                    if (relative)
                    {
                        endX += currentX;
                        endY += currentY;
                    }

                    float controlX = currentX;
                    float controlY = currentY;

                    if (previous == 'Q' || previous == 'T')
                    {
                        controlX = 2 * currentX - lastControlX;
                        controlY = 2 * currentY - lastControlY;
                    }

                    path.quadTo(controlX, controlY, endX, endY);

                    lastControlX = controlX;
                    lastControlY = controlY;
                    currentX = endX;
                    currentY = endY;
                    break;
                }

                case 'A':
                {
                    float radiusX = cursor.number();
                    float radiusY = cursor.number();
                    float rotation = cursor.number();
                    boolean largeArc = cursor.flag();
                    boolean sweep = cursor.flag();
                    float endX = cursor.number();
                    float endY = cursor.number();
                    if (cursor.failed()) return false;

                    if (relative)
                    {
                        endX += currentX;
                        endY += currentY;
                    }

                    arcTo(path, currentX, currentY, endX, endY, radiusX, radiusY, rotation, largeArc, sweep);

                    currentX = endX;
                    currentY = endY;
                    break;
                }

                case 'Z':
                {
                    path.close();
                    currentX = startX;
                    currentY = startY;
                    break;
                }

                default:
                    return false;
            }

            previous = normalised;
        }

        return true;
    }

    private static boolean isCommand(char value)
    {
        switch (Character.toUpperCase(value))
        {
            case 'M':
            case 'L':
            case 'H':
            case 'V':
            case 'C':
            case 'S':
            case 'Q':
            case 'T':
            case 'A':
            case 'Z':
                return true;
            default:
                return false;
        }
    }

    private static void arcTo(Sink path, float fromX, float fromY, float toX, float toY, float radiusX, float radiusY,
                              float rotationDegrees, boolean largeArc, boolean sweep)
    {
        if (fromX == toX && fromY == toY) return;

        if (radiusX == 0 || radiusY == 0)
        {
            path.lineTo(toX, toY);
            return;
        }

        radiusX = Math.abs(radiusX);
        radiusY = Math.abs(radiusY);

        double rotation = Math.toRadians(rotationDegrees);
        double cosine = Math.cos(rotation);
        double sine = Math.sin(rotation);

        double halfDeltaX = (fromX - toX) / 2.0;
        double halfDeltaY = (fromY - toY) / 2.0;

        double turnedX = cosine * halfDeltaX + sine * halfDeltaY;
        double turnedY = -sine * halfDeltaX + cosine * halfDeltaY;

        double scaledX = turnedX / radiusX;
        double scaledY = turnedY / radiusY;
        double overshoot = scaledX * scaledX + scaledY * scaledY;

        if (overshoot > 1)
        {
            double growth = Math.sqrt(overshoot);
            radiusX = (float) (radiusX * growth);
            radiusY = (float) (radiusY * growth);
            scaledX /= growth;
            scaledY /= growth;
        }

        double numerator = 1.0 - scaledX * scaledX - scaledY * scaledY;
        if (numerator < 0) numerator = 0;

        double factor = Math.sqrt(numerator / (scaledX * scaledX + scaledY * scaledY));
        if (largeArc == sweep) factor = -factor;

        double centreTurnedX = factor * radiusX * scaledY;
        double centreTurnedY = -factor * radiusY * scaledX;

        double centreX = cosine * centreTurnedX - sine * centreTurnedY + (fromX + toX) / 2.0;
        double centreY = sine * centreTurnedX + cosine * centreTurnedY + (fromY + toY) / 2.0;

        double startAngle = angleBetween(1, 0, (turnedX - centreTurnedX) / radiusX, (turnedY - centreTurnedY) / radiusY);
        double sweepAngle = angleBetween((turnedX - centreTurnedX) / radiusX, (turnedY - centreTurnedY) / radiusY,
                                         (-turnedX - centreTurnedX) / radiusX, (-turnedY - centreTurnedY) / radiusY);

        if (!sweep && sweepAngle > 0) sweepAngle -= 2 * Math.PI;
        else if (sweep && sweepAngle < 0) sweepAngle += 2 * Math.PI;

        emitArc(path, centreX, centreY, radiusX, radiusY, startAngle, sweepAngle, cosine, sine);
    }

    private static double angleBetween(double firstX, double firstY, double secondX, double secondY)
    {
        double dot = firstX * secondX + firstY * secondY;
        double length = Math.sqrt((firstX * firstX + firstY * firstY) * (secondX * secondX + secondY * secondY));

        if (length == 0) return 0;

        double cosine = dot / length;
        if (cosine > 1) cosine = 1;
        if (cosine < -1) cosine = -1;

        double angle = Math.acos(cosine);
        return firstX * secondY - firstY * secondX < 0 ? -angle : angle;
    }

    private static void emitArc(Sink path, double centreX, double centreY, float radiusX, float radiusY, double startAngle,
                                double sweepAngle, double cosine, double sine)
    {
        int segments = (int) Math.ceil(Math.abs(sweepAngle) / (Math.PI / 2));
        if (segments < 1) segments = 1;

        double step = sweepAngle / segments;
        double control = 4.0 / 3.0 * Math.tan(step / 4.0);

        double angle = startAngle;

        for (int segment = 0; segment < segments; segment++)
        {
            double next = angle + step;

            double sineStart = Math.sin(angle);
            double cosineStart = Math.cos(angle);
            double sineEnd = Math.sin(next);
            double cosineEnd = Math.cos(next);

            double firstX = radiusX * (cosineStart - control * sineStart);
            double firstY = radiusY * (sineStart + control * cosineStart);
            double secondX = radiusX * (cosineEnd + control * sineEnd);
            double secondY = radiusY * (sineEnd - control * cosineEnd);
            double endX = radiusX * cosineEnd;
            double endY = radiusY * sineEnd;

            path.cubicTo((float) (centreX + cosine * firstX - sine * firstY),
                         (float) (centreY + sine * firstX + cosine * firstY),
                         (float) (centreX + cosine * secondX - sine * secondY),
                         (float) (centreY + sine * secondX + cosine * secondY),
                         (float) (centreX + cosine * endX - sine * endY),
                         (float) (centreY + sine * endX + cosine * endY));

            angle = next;
        }
    }

    private static final class Cursor
    {
        private final String mText;
        private int mAt;
        private boolean mFailed;

        Cursor(String text)
        {
            mText = text;
        }

        boolean done()
        {
            return mAt >= mText.length();
        }

        boolean failed()
        {
            return mFailed;
        }

        char peek()
        {
            return mText.charAt(mAt);
        }

        void advance()
        {
            mAt++;
        }

        void skipSeparators()
        {
            while (mAt < mText.length())
            {
                char value = mText.charAt(mAt);
                if (value != ' ' && value != ',' && value != '\t' && value != '\n' && value != '\r') break;

                mAt++;
            }
        }

        boolean flag()
        {
            skipSeparators();
            if (mAt >= mText.length())
            {
                mFailed = true;
                return false;
            }

            char value = mText.charAt(mAt);
            if (value != '0' && value != '1')
            {
                mFailed = true;
                return false;
            }

            mAt++;
            return value == '1';
        }

        float number()
        {
            skipSeparators();

            int start = mAt;
            boolean seenDigit = false;
            boolean seenDot = false;

            if (mAt < mText.length() && (mText.charAt(mAt) == '-' || mText.charAt(mAt) == '+')) mAt++;

            while (mAt < mText.length())
            {
                char value = mText.charAt(mAt);

                if (value >= '0' && value <= '9')
                {
                    seenDigit = true;
                    mAt++;
                    continue;
                }

                if (value == '.' && !seenDot)
                {
                    seenDot = true;
                    mAt++;
                    continue;
                }

                if ((value == 'e' || value == 'E') && seenDigit)
                {
                    int mark = mAt;
                    mAt++;

                    if (mAt < mText.length() && (mText.charAt(mAt) == '-' || mText.charAt(mAt) == '+')) mAt++;

                    if (mAt < mText.length() && mText.charAt(mAt) >= '0' && mText.charAt(mAt) <= '9')
                    {
                        while (mAt < mText.length() && mText.charAt(mAt) >= '0' && mText.charAt(mAt) <= '9') mAt++;
                        continue;
                    }

                    mAt = mark;
                }

                break;
            }

            if (!seenDigit)
            {
                mFailed = true;
                return 0;
            }

            try
            {
                return Float.parseFloat(mText.substring(start, mAt));
            }
            catch (NumberFormatException failure)
            {
                mFailed = true;
                return 0;
            }
        }
    }
}
