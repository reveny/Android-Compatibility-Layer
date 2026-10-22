package android.util;

public class StringBuilderPrinter implements Printer
{
    private final StringBuilder mBuilder;

    public StringBuilderPrinter(StringBuilder builder)
    {
        mBuilder = builder;
    }

    @Override
    public void println(String line)
    {
        mBuilder.append(line);

        int length = line.length();
        if (length <= 0 || line.charAt(length - 1) != '\n') mBuilder.append('\n');
    }
}
