package runtime;

public final class TextMeasurers
{
    private static TextMeasurer sInstalled;

    private TextMeasurers()
    {
    }

    public static void install(TextMeasurer measurer)
    {
        sInstalled = measurer;
    }

    public static TextMeasurer get()
    {
        if (sInstalled == null)
        {
            Strict.unimplemented("runtime.TextMeasurers.get with no measurer installed - text would fall back to "
                                 + "M2's fabricated fixed-advance metrics");
            sInstalled = new FixedAdvanceTextMeasurer();
        }

        return sInstalled;
    }

    public static boolean isNativeBacked()
    {
        return sInstalled != null && !(sInstalled instanceof FixedAdvanceTextMeasurer);
    }
}
