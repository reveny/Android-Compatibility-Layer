package android.util;

public class PathDataUnit
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

    private static final class Recorder implements PathParser.Sink
    {
        private final StringBuilder mLog = new StringBuilder();

        private static String Round(float value)
        {
            float rounded = Math.round(value * 100) / 100.0f;
            if (rounded == Math.rint(rounded)) return String.valueOf((int) rounded);

            return String.valueOf(rounded);
        }

        @Override
        public void moveTo(float x, float y)
        {
            mLog.append("M").append(Round(x)).append(",").append(Round(y)).append(" ");
        }

        @Override
        public void lineTo(float x, float y)
        {
            mLog.append("L").append(Round(x)).append(",").append(Round(y)).append(" ");
        }

        @Override
        public void quadTo(float controlX, float controlY, float x, float y)
        {
            mLog.append("Q").append(Round(controlX)).append(",").append(Round(controlY)).append(",").append(Round(x))
                .append(",").append(Round(y)).append(" ");
        }

        @Override
        public void cubicTo(float firstX, float firstY, float secondX, float secondY, float x, float y)
        {
            mLog.append("C").append(Round(firstX)).append(",").append(Round(firstY)).append(",").append(Round(secondX))
                .append(",").append(Round(secondY)).append(",").append(Round(x)).append(",").append(Round(y)).append(" ");
        }

        @Override
        public void close()
        {
            mLog.append("Z ");
        }

        String log()
        {
            return mLog.toString().trim();
        }
    }

    private static String Parse(String data)
    {
        Recorder recorder = new Recorder();
        if (!PathParser.parse(data, recorder)) return "REFUSED";

        return recorder.log();
    }

    private static void ExpectParse(String data, String wanted, String description)
    {
        String got = Parse(data);
        Expect(wanted.equals(got), description + " [" + data + "] -> " + got);
    }

    private static void CheckBasicCommands()
    {
        ExpectParse("M10,20 L30,40 Z", "M10,20 L30,40 Z", "absolute move, line and close");
        ExpectParse("m10,20 l5,5", "M10,20 L15,25", "relative commands accumulate");
        ExpectParse("M0,0 H10 V20", "M0,0 L10,0 L10,20", "horizontal and vertical lines hold the other axis");
        ExpectParse("M0,0 h10 v20", "M0,0 L10,0 L10,20", "relative horizontal and vertical");
    }

    private static void CheckRepeatedParameters()
    {
        ExpectParse("M0,0 L1,1 2,2 3,3", "M0,0 L1,1 L2,2 L3,3", "a line command repeats without being named again");
        ExpectParse("M0,0 1,1 2,2", "M0,0 L1,1 L2,2", "a repeated move becomes a line, as the grammar says");
        ExpectParse("m0,0 1,1 2,2", "M0,0 L1,1 L3,3", "a repeated relative move becomes a relative line");
    }

    private static void CheckNumberForms()
    {
        ExpectParse("M1-2L3-4", "M1,-2 L3,-4", "a minus sign separates numbers with no comma");
        ExpectParse("M.5.5L1.5.5", "M0.5,0.5 L1.5,0.5", "a decimal point separates numbers with no comma");
        ExpectParse("M1e2,1E-1", "M100,0.1", "exponents are read");
        ExpectParse("M +1 , +2", "M1,2", "leading plus signs and loose separators are read");
    }

    private static void CheckCurves()
    {
        ExpectParse("M0,0 C1,2,3,4,5,6", "M0,0 C1,2,3,4,5,6", "a cubic passes its control points through");
        ExpectParse("M0,0 Q1,2,3,4", "M0,0 Q1,2,3,4", "a quadratic passes its control point through");

        ExpectParse("M0,0 C1,1,2,2,3,3 S4,4,5,5", "M0,0 C1,1,2,2,3,3 C4,4,4,4,5,5",
                    "a smooth cubic reflects the previous control point");
        ExpectParse("M0,0 S4,4,5,5", "M0,0 C0,0,4,4,5,5", "a smooth cubic with no previous curve starts at the current point");
        ExpectParse("M0,0 Q1,1,2,2 T4,4", "M0,0 Q1,1,2,2 Q3,3,4,4", "a smooth quadratic reflects the previous control point");
    }

    private static void CheckArcs()
    {
        String quarter = Parse("M10,0 A10,10 0 0 1 0,10");
        Expect(quarter.startsWith("M10,0 C"), "an arc becomes cubics");
        Expect(quarter.endsWith("0,10"), "an arc ends where it was told to");

        String flags = Parse("M0,0 a5,5 0 1,1 10,0");
        Expect(!flags.equals("REFUSED"), "arc flags parse when written without spaces");

        ExpectParse("M0,0 A0,0 0 0 1 10,10", "M0,0 L10,10", "an arc with no radius is a straight line");
    }

    private static void CheckRefusals()
    {
        Expect(Parse("10,10 L20,20").equals("REFUSED"), "data that starts without any command is refused");
        Expect(Parse("M10").equals("REFUSED"), "a move missing its second number is refused");
        Expect(Parse("M0,0 A5,5 0 2 1 10,10").equals("REFUSED"), "an arc flag that is not zero or one is refused");
        Expect(PathParser.canParse("M0,0 L1,1"), "canParse accepts what parses");
        Expect(!PathParser.canParse("nonsense"), "canParse refuses what does not");
        Expect(!PathParser.canParse(null), "canParse refuses nothing at all");
        Expect(Parse("").equals(""), "empty data parses to nothing, rather than failing");
    }

    private static void CheckRealIcon()
    {
        String check = "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z";
        String got = Parse(check);

        Expect(!got.equals("REFUSED"), "a real material icon path parses");
        Expect(got.startsWith("M9,16.17 L4.83,12 "), "the icon's first two points are right");
        Expect(got.endsWith("Z"), "the icon closes");
    }

    public static void main(String[] args)
    {
        System.out.println("path data:");

        CheckBasicCommands();
        CheckRepeatedParameters();
        CheckNumberForms();
        CheckCurves();
        CheckArcs();
        CheckRefusals();
        CheckRealIcon();

        if (failures != 0)
        {
            System.out.println("path-data FAILED: " + failures + " of " + checks + " checks failed");
            System.exit(1);
        }

        System.out.println("path-data PASSED: " + checks + " checks");
    }
}
