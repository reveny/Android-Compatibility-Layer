import android.graphics.Matrix;
import android.graphics.RectF;

public class GraphicsUnit
{
    private static int checks;
    private static int failures;

    private static void Expect(boolean condition, String description)
    {
        checks++;

        if (condition)
        {
            System.out.println("  ok    " + description);
            return;
        }

        System.out.println("  FAIL  " + description);
        failures++;
    }

    private static void ExpectNear(float actual, float wanted, String description)
    {
        Expect(Math.abs(actual - wanted) < 0.001f, description + " (wanted " + wanted + ", got " + actual + ")");
    }

    private static float[] Map(Matrix matrix, float x, float y)
    {
        float[] point = {x, y};
        matrix.mapPoints(point);

        return point;
    }

    private static void CheckConcatIsNotIgnored()
    {
        Matrix move = new Matrix();
        move.setTranslate(10, 20);

        Matrix scale = new Matrix();
        scale.setScale(2, 3);

        Matrix combined = new Matrix(move);
        combined.preConcat(scale);

        float[] mapped = Map(combined, 1, 1);
        ExpectNear(mapped[0], 12, "preConcat scales before it translates");
        ExpectNear(mapped[1], 23, "preConcat scales before it translates, vertically");

        Matrix other = new Matrix(move);
        other.postConcat(scale);

        float[] posted = Map(other, 1, 1);
        ExpectNear(posted[0], 22, "postConcat translates before it scales");
        ExpectNear(posted[1], 63, "postConcat translates before it scales, vertically");

        Matrix product = new Matrix();
        product.setConcat(move, scale);
        Expect(product.equals(combined), "setConcat matches a preConcat onto the first");
    }

    private static void CheckRotation()
    {
        Matrix turn = new Matrix();
        turn.setRotate(90);

        float[] mapped = Map(turn, 1, 0);
        ExpectNear(mapped[0], 0, "a quarter turn sends x to the origin");
        ExpectNear(mapped[1], 1, "a quarter turn sends x onto y");

        Matrix aboutPoint = new Matrix();
        aboutPoint.setRotate(90, 10, 10);

        float[] pivot = Map(aboutPoint, 10, 10);
        ExpectNear(pivot[0], 10, "a rotation about a pivot leaves the pivot alone");
        ExpectNear(pivot[1], 10, "a rotation about a pivot leaves the pivot alone, vertically");
    }

    private static void CheckScaleCarriesSkew()
    {
        Matrix turned = new Matrix();
        turned.setRotate(90);
        turned.postScale(2, 2);

        float[] mapped = Map(turned, 1, 0);
        ExpectNear(mapped[0], 0, "scaling after a rotation scales the rotated axes");
        ExpectNear(mapped[1], 2, "scaling after a rotation scales the rotated axes, vertically");
    }

    private static void CheckPreTranslateUnderRotation()
    {
        Matrix turned = new Matrix();
        turned.setRotate(90);
        turned.preTranslate(5, 0);

        float[] mapped = Map(turned, 0, 0);
        ExpectNear(mapped[0], 0, "a pre-translate moves along the rotated axes");
        ExpectNear(mapped[1], 5, "a pre-translate moves along the rotated axes, vertically");
    }

    private static void CheckInverseOfARotation()
    {
        Matrix turned = new Matrix();
        turned.setRotate(37);
        turned.postTranslate(11, -4);
        turned.preScale(2, 5);

        Matrix inverse = new Matrix();
        Expect(turned.invert(inverse), "a rotated, scaled, translated matrix inverts");

        float[] point = {3, 7};
        turned.mapPoints(point);
        inverse.mapPoints(point);

        ExpectNear(point[0], 3, "the inverse takes a mapped point back");
        ExpectNear(point[1], 7, "the inverse takes a mapped point back, vertically");

        Matrix singular = new Matrix();
        singular.setScale(0, 4);
        Expect(!singular.invert(new Matrix()), "a singular matrix reports that it cannot invert");
    }

    private static void CheckMapRectUnderRotation()
    {
        Matrix turned = new Matrix();
        turned.setRotate(45);

        RectF box = new RectF(-1, -1, 1, 1);
        boolean stayed = turned.mapRect(box);

        Expect(!stayed, "a forty five degree turn does not keep a rect a rect");
        ExpectNear(box.left, (float) -Math.sqrt(2), "the mapped bounds cover the rotated corners");
        ExpectNear(box.right, (float) Math.sqrt(2), "the mapped bounds cover the rotated corners, on the right");

        Matrix quarter = new Matrix();
        quarter.setRotate(90);
        Expect(quarter.rectStaysRect(), "a quarter turn does keep a rect a rect");
    }

    private static void CheckMapRadius()
    {
        Matrix grown = new Matrix();
        grown.setScale(3, 3);

        ExpectNear(grown.mapRadius(2), 6, "a uniform scale scales a radius by the same amount");
    }

    private static void CheckRectToRect()
    {
        Matrix fit = new Matrix();
        RectF from = new RectF(0, 0, 10, 20);
        RectF to = new RectF(0, 0, 100, 100);

        fit.setRectToRect(from, to, Matrix.ScaleToFit.CENTER);

        RectF result = new RectF(from);
        fit.mapRect(result);

        ExpectNear(result.width(), 50, "a centred fit keeps the aspect ratio");
        ExpectNear(result.height(), 100, "a centred fit fills the constrained axis");
        ExpectNear(result.left, 25, "a centred fit centres what is left over");
    }

    public static void main(String[] args)
    {
        System.out.println("graphics geometry:");

        CheckConcatIsNotIgnored();
        CheckRotation();
        CheckScaleCarriesSkew();
        CheckPreTranslateUnderRotation();
        CheckInverseOfARotation();
        CheckMapRectUnderRotation();
        CheckMapRadius();
        CheckRectToRect();

        if (failures != 0)
        {
            System.out.println("graphics-unit FAILED: " + failures + " of " + checks + " checks failed");
            System.exit(1);
        }

        System.out.println("graphics-unit PASSED: " + checks + " checks");
    }
}
