package android.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public final class JsonWriter implements Closeable
{
    private final Writer mOut;
    private final List<JsonScope> mStack = new ArrayList<>();

    private String mIndent;
    private String mSeparator = ":";
    private boolean mLenient;

    public JsonWriter(Writer out)
    {
        if (out == null) throw new NullPointerException("out == null");

        mOut = out;
        mStack.add(JsonScope.EMPTY_DOCUMENT);
    }

    public void setIndent(String indent)
    {
        if (indent.isEmpty())
        {
            mIndent = null;
            mSeparator = ":";
        }
        else
        {
            mIndent = indent;
            mSeparator = ": ";
        }
    }

    public void setLenient(boolean lenient)
    {
        mLenient = lenient;
    }

    public boolean isLenient()
    {
        return mLenient;
    }

    public JsonWriter beginArray() throws IOException
    {
        return open(JsonScope.EMPTY_ARRAY, "[");
    }

    public JsonWriter endArray() throws IOException
    {
        return close(JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY, "]");
    }

    public JsonWriter beginObject() throws IOException
    {
        return open(JsonScope.EMPTY_OBJECT, "{");
    }

    public JsonWriter endObject() throws IOException
    {
        return close(JsonScope.EMPTY_OBJECT, JsonScope.NONEMPTY_OBJECT, "}");
    }

    private JsonWriter open(JsonScope empty, String openBracket) throws IOException
    {
        beforeValue(true);
        mStack.add(empty);
        mOut.write(openBracket);
        return this;
    }

    private JsonWriter close(JsonScope empty, JsonScope nonempty, String closeBracket) throws IOException
    {
        JsonScope context = peek();
        if (context != nonempty && context != empty) throw new IllegalStateException("Nesting problem: " + mStack);

        mStack.remove(mStack.size() - 1);
        if (context == nonempty) newline();

        mOut.write(closeBracket);
        return this;
    }

    private JsonScope peek()
    {
        return mStack.get(mStack.size() - 1);
    }

    private void replaceTop(JsonScope scope)
    {
        mStack.set(mStack.size() - 1, scope);
    }

    public JsonWriter name(String name) throws IOException
    {
        if (name == null) throw new NullPointerException("name == null");

        beforeName();
        string(name);
        return this;
    }

    public JsonWriter value(String value) throws IOException
    {
        if (value == null) return nullValue();

        beforeValue(false);
        string(value);
        return this;
    }

    public JsonWriter nullValue() throws IOException
    {
        beforeValue(false);
        mOut.write("null");
        return this;
    }

    public JsonWriter value(boolean value) throws IOException
    {
        beforeValue(false);
        mOut.write(value ? "true" : "false");
        return this;
    }

    public JsonWriter value(double value) throws IOException
    {
        if (!mLenient && (Double.isNaN(value) || Double.isInfinite(value)))
        {
            throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
        }

        beforeValue(false);
        mOut.append(Double.toString(value));
        return this;
    }

    public JsonWriter value(long value) throws IOException
    {
        beforeValue(false);
        mOut.write(Long.toString(value));
        return this;
    }

    public JsonWriter value(Number value) throws IOException
    {
        if (value == null) return nullValue();

        String string = value.toString();
        if (!mLenient && (string.equals("-Infinity") || string.equals("Infinity") || string.equals("NaN")))
        {
            throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
        }

        beforeValue(false);
        mOut.append(string);
        return this;
    }

    public void flush() throws IOException
    {
        mOut.flush();
    }

    @Override
    public void close() throws IOException
    {
        mOut.close();

        if (peek() != JsonScope.NONEMPTY_DOCUMENT) throw new IOException("Incomplete document");
    }

    private void string(String value) throws IOException
    {
        mOut.write("\"");
        for (int index = 0, length = value.length(); index < length; index++)
        {
            char character = value.charAt(index);
            switch (character)
            {
                case '"':
                case '\\':
                    mOut.write('\\');
                    mOut.write(character);
                    break;
                case '\t':
                    mOut.write("\\t");
                    break;
                case '\b':
                    mOut.write("\\b");
                    break;
                case '\n':
                    mOut.write("\\n");
                    break;
                case '\r':
                    mOut.write("\\r");
                    break;
                case '\f':
                    mOut.write("\\f");
                    break;
                case ' ':
                case ' ':
                    mOut.write(String.format("\\u%04x", (int)character));
                    break;
                default:
                    if (character <= 0x1F) mOut.write(String.format("\\u%04x", (int)character));
                    else mOut.write(character);
                    break;
            }
        }
        mOut.write("\"");
    }

    private void newline() throws IOException
    {
        if (mIndent == null) return;

        mOut.write("\n");
        for (int index = 1; index < mStack.size(); index++)
        {
            mOut.write(mIndent);
        }
    }

    private void beforeName() throws IOException
    {
        JsonScope context = peek();
        if (context == JsonScope.NONEMPTY_OBJECT) mOut.write(',');
        else if (context != JsonScope.EMPTY_OBJECT) throw new IllegalStateException("Nesting problem: " + mStack);

        newline();
        replaceTop(JsonScope.DANGLING_NAME);
    }

    private void beforeValue(boolean root) throws IOException
    {
        switch (peek())
        {
            case EMPTY_DOCUMENT:
                if (!mLenient && !root) throw new IllegalStateException("JSON must start with an array or an object.");
                replaceTop(JsonScope.NONEMPTY_DOCUMENT);
                break;
            case EMPTY_ARRAY:
                replaceTop(JsonScope.NONEMPTY_ARRAY);
                newline();
                break;
            case NONEMPTY_ARRAY:
                mOut.append(',');
                newline();
                break;
            case DANGLING_NAME:
                mOut.append(mSeparator);
                replaceTop(JsonScope.NONEMPTY_OBJECT);
                break;
            case NONEMPTY_DOCUMENT:
                throw new IllegalStateException("JSON must have only one top-level value.");
            default:
                throw new IllegalStateException("Nesting problem: " + mStack);
        }
    }
}
