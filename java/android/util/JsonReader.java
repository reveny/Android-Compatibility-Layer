package android.util;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public final class JsonReader implements Closeable
{
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private final Reader mIn;
    private final char[] mBuffer = new char[1024];
    private final List<JsonScope> mStack = new ArrayList<>();

    private boolean mLenient;
    private int mPosition;
    private int mLimit;
    private int mBufferStartLine = 1;
    private int mBufferStartColumn = 1;
    private JsonToken mToken;
    private String mName;
    private String mValue;
    private int mValuePosition;
    private int mValueLength;
    private boolean mSkipping;

    public JsonReader(Reader in)
    {
        if (in == null) throw new NullPointerException("in == null");

        push(JsonScope.EMPTY_DOCUMENT);
        mIn = in;
    }

    public void setLenient(boolean lenient)
    {
        mLenient = lenient;
    }

    public boolean isLenient()
    {
        return mLenient;
    }

    public void beginArray() throws IOException
    {
        expect(JsonToken.BEGIN_ARRAY);
    }

    public void endArray() throws IOException
    {
        expect(JsonToken.END_ARRAY);
    }

    public void beginObject() throws IOException
    {
        expect(JsonToken.BEGIN_OBJECT);
    }

    public void endObject() throws IOException
    {
        expect(JsonToken.END_OBJECT);
    }

    private void expect(JsonToken expected) throws IOException
    {
        peek();
        if (mToken != expected) throw new IllegalStateException("Expected " + expected + " but was " + peek());

        advance();
    }

    public boolean hasNext() throws IOException
    {
        peek();
        return mToken != JsonToken.END_OBJECT && mToken != JsonToken.END_ARRAY;
    }

    public JsonToken peek() throws IOException
    {
        if (mToken != null) return mToken;

        switch (peekStack())
        {
            case EMPTY_DOCUMENT:
                replaceTop(JsonScope.NONEMPTY_DOCUMENT);
                JsonToken firstToken = nextValue();
                if (!mLenient && mToken != JsonToken.BEGIN_ARRAY && mToken != JsonToken.BEGIN_OBJECT)
                {
                    throw new IOException("Expected JSON document to start with '[' or '{' but was " + mToken);
                }
                return firstToken;
            case EMPTY_ARRAY:
                return nextInArray(true);
            case NONEMPTY_ARRAY:
                return nextInArray(false);
            case EMPTY_OBJECT:
                return nextInObject(true);
            case DANGLING_NAME:
                return objectValue();
            case NONEMPTY_OBJECT:
                return nextInObject(false);
            case NONEMPTY_DOCUMENT:
                int character = nextNonWhitespace(false);
                if (character == -1) return JsonToken.END_DOCUMENT;

                mPosition--;
                if (!mLenient) throw syntaxError("Expected EOF");
                return nextValue();
            case CLOSED:
                throw new IllegalStateException("JsonReader is closed");
            default:
                throw new AssertionError();
        }
    }

    private JsonToken advance() throws IOException
    {
        peek();

        JsonToken result = mToken;
        mToken = null;
        mValue = null;
        mName = null;
        return result;
    }

    public String nextName() throws IOException
    {
        peek();
        if (mToken != JsonToken.NAME) throw new IllegalStateException("Expected a name but was " + peek());

        String result = mName;
        advance();
        return result;
    }

    public String nextString() throws IOException
    {
        peek();
        if (mToken != JsonToken.STRING && mToken != JsonToken.NUMBER)
        {
            throw new IllegalStateException("Expected a string but was " + peek());
        }

        String result = mValue;
        advance();
        return result;
    }

    public boolean nextBoolean() throws IOException
    {
        peek();
        if (mToken != JsonToken.BOOLEAN) throw new IllegalStateException("Expected a boolean but was " + mToken);

        boolean result = mValue == TRUE;
        advance();
        return result;
    }

    public void nextNull() throws IOException
    {
        peek();
        if (mToken != JsonToken.NULL) throw new IllegalStateException("Expected null but was " + mToken);

        advance();
    }

    public double nextDouble() throws IOException
    {
        peek();
        if (mToken != JsonToken.STRING && mToken != JsonToken.NUMBER)
        {
            throw new IllegalStateException("Expected a double but was " + mToken);
        }

        double result = Double.parseDouble(mValue);
        advance();
        return result;
    }

    public long nextLong() throws IOException
    {
        peek();
        if (mToken != JsonToken.STRING && mToken != JsonToken.NUMBER)
        {
            throw new IllegalStateException("Expected a long but was " + mToken);
        }

        long result;
        try
        {
            result = Long.parseLong(mValue);
        }
        catch (NumberFormatException ignored)
        {
            double asDouble = Double.parseDouble(mValue);
            result = (long)asDouble;
            if ((double)result != asDouble) throw new NumberFormatException(mValue);
        }

        advance();
        return result;
    }

    public int nextInt() throws IOException
    {
        peek();
        if (mToken != JsonToken.STRING && mToken != JsonToken.NUMBER)
        {
            throw new IllegalStateException("Expected an int but was " + mToken);
        }

        int result;
        try
        {
            result = Integer.parseInt(mValue);
        }
        catch (NumberFormatException ignored)
        {
            double asDouble = Double.parseDouble(mValue);
            result = (int)asDouble;
            if ((double)result != asDouble) throw new NumberFormatException(mValue);
        }

        advance();
        return result;
    }

    @Override
    public void close() throws IOException
    {
        mValue = null;
        mToken = null;
        mStack.clear();
        mStack.add(JsonScope.CLOSED);
        mIn.close();
    }

    public void skipValue() throws IOException
    {
        mSkipping = true;
        try
        {
            if (!hasNext() || peek() == JsonToken.END_DOCUMENT) throw new IllegalStateException("No element left to skip");

            int count = 0;
            do
            {
                JsonToken token = advance();
                if (token == JsonToken.BEGIN_ARRAY || token == JsonToken.BEGIN_OBJECT) count++;
                else if (token == JsonToken.END_ARRAY || token == JsonToken.END_OBJECT) count--;
            }
            while (count != 0);
        }
        finally
        {
            mSkipping = false;
        }
    }

    private JsonScope peekStack()
    {
        return mStack.get(mStack.size() - 1);
    }

    private JsonScope pop()
    {
        return mStack.remove(mStack.size() - 1);
    }

    private void push(JsonScope scope)
    {
        mStack.add(scope);
    }

    private void replaceTop(JsonScope scope)
    {
        mStack.set(mStack.size() - 1, scope);
    }

    private JsonToken nextInArray(boolean firstElement) throws IOException
    {
        if (firstElement)
        {
            replaceTop(JsonScope.NONEMPTY_ARRAY);
        }
        else
        {
            switch (nextNonWhitespace(true))
            {
                case ']':
                    pop();
                    return mToken = JsonToken.END_ARRAY;
                case ';':
                    checkLenient();
                    break;
                case ',':
                    break;
                default:
                    throw syntaxError("Unterminated array");
            }
        }

        int character = nextNonWhitespace(true);
        if (character == ']' && firstElement)
        {
            pop();
            return mToken = JsonToken.END_ARRAY;
        }

        if (character == ']' || character == ';' || character == ',')
        {
            checkLenient();
            mPosition--;
            mValue = "null";
            return mToken = JsonToken.NULL;
        }

        mPosition--;
        return nextValue();
    }

    private JsonToken nextInObject(boolean firstElement) throws IOException
    {
        int character = nextNonWhitespace(true);
        if (character == '}')
        {
            pop();
            return mToken = JsonToken.END_OBJECT;
        }

        if (firstElement)
        {
            mPosition--;
        }
        else if (character != ';' && character != ',')
        {
            throw syntaxError("Unterminated object");
        }

        int quote = nextNonWhitespace(true);
        if (quote == '\'' || quote == '"')
        {
            if (quote == '\'') checkLenient();
            mName = nextString((char)quote);
        }
        else
        {
            checkLenient();
            mPosition--;
            mName = nextLiteral(false);
            if (mName.isEmpty()) throw syntaxError("Expected name");
        }

        replaceTop(JsonScope.DANGLING_NAME);
        return mToken = JsonToken.NAME;
    }

    private JsonToken objectValue() throws IOException
    {
        switch (nextNonWhitespace(true))
        {
            case ':':
                break;
            case '=':
                checkLenient();
                if ((mPosition < mLimit || fillBuffer(1)) && mBuffer[mPosition] == '>') mPosition++;
                break;
            default:
                throw syntaxError("Expected ':'");
        }

        replaceTop(JsonScope.NONEMPTY_OBJECT);
        return nextValue();
    }

    private JsonToken nextValue() throws IOException
    {
        int character = nextNonWhitespace(true);
        switch (character)
        {
            case '{':
                push(JsonScope.EMPTY_OBJECT);
                return mToken = JsonToken.BEGIN_OBJECT;
            case '[':
                push(JsonScope.EMPTY_ARRAY);
                return mToken = JsonToken.BEGIN_ARRAY;
            case '\'':
                checkLenient();
                mValue = nextString((char)character);
                return mToken = JsonToken.STRING;
            case '"':
                mValue = nextString((char)character);
                return mToken = JsonToken.STRING;
            default:
                mPosition--;
                return readLiteral();
        }
    }

    private boolean fillBuffer(int minimum) throws IOException
    {
        for (int index = 0; index < mPosition; index++)
        {
            if (mBuffer[index] == '\n')
            {
                mBufferStartLine++;
                mBufferStartColumn = 1;
            }
            else
            {
                mBufferStartColumn++;
            }
        }

        if (mLimit != mPosition)
        {
            mLimit -= mPosition;
            System.arraycopy(mBuffer, mPosition, mBuffer, 0, mLimit);
        }
        else
        {
            mLimit = 0;
        }

        mPosition = 0;
        int total;
        while ((total = mIn.read(mBuffer, mLimit, mBuffer.length - mLimit)) != -1)
        {
            mLimit += total;

            if (mBufferStartLine == 1 && mBufferStartColumn == 1 && mLimit > 0 && mBuffer[0] == '﻿')
            {
                mPosition++;
                mBufferStartColumn--;
            }

            if (mLimit >= minimum) return true;
        }
        return false;
    }

    private int getLineNumber()
    {
        int result = mBufferStartLine;
        for (int index = 0; index < mPosition; index++)
        {
            if (mBuffer[index] == '\n') result++;
        }
        return result;
    }

    private int getColumnNumber()
    {
        int result = mBufferStartColumn;
        for (int index = 0; index < mPosition; index++)
        {
            if (mBuffer[index] == '\n') result = 1;
            else result++;
        }
        return result;
    }

    private int nextNonWhitespace(boolean throwOnEof) throws IOException
    {
        while (mPosition < mLimit || fillBuffer(1))
        {
            int character = mBuffer[mPosition++];
            switch (character)
            {
                case '\t':
                case ' ':
                case '\n':
                case '\r':
                    continue;
                case '/':
                    if (mPosition == mLimit && !fillBuffer(1)) return character;

                    checkLenient();
                    char next = mBuffer[mPosition];
                    if (next == '*')
                    {
                        mPosition++;
                        if (!skipTo("*/")) throw syntaxError("Unterminated comment");
                        mPosition += 2;
                        continue;
                    }
                    if (next == '/')
                    {
                        mPosition++;
                        skipToEndOfLine();
                        continue;
                    }
                    return character;
                case '#':
                    checkLenient();
                    skipToEndOfLine();
                    continue;
                default:
                    return character;
            }
        }

        if (throwOnEof) throw new EOFException("End of input");
        return -1;
    }

    private void checkLenient() throws IOException
    {
        if (!mLenient) throw syntaxError("Use JsonReader.setLenient(true) to accept malformed JSON");
    }

    private void skipToEndOfLine() throws IOException
    {
        while (mPosition < mLimit || fillBuffer(1))
        {
            char character = mBuffer[mPosition++];
            if (character == '\r' || character == '\n') break;
        }
    }

    private boolean skipTo(String toFind) throws IOException
    {
        for (; mPosition + toFind.length() <= mLimit || fillBuffer(toFind.length()); mPosition++)
        {
            boolean matched = true;
            for (int index = 0; index < toFind.length(); index++)
            {
                if (mBuffer[mPosition + index] != toFind.charAt(index))
                {
                    matched = false;
                    break;
                }
            }
            if (matched) return true;
        }
        return false;
    }

    private String nextString(char quote) throws IOException
    {
        StringBuilder builder = null;
        do
        {
            int start = mPosition;
            while (mPosition < mLimit)
            {
                int character = mBuffer[mPosition++];
                if (character == quote)
                {
                    if (mSkipping) return "skipped!";
                    if (builder == null) return new String(mBuffer, start, mPosition - start - 1);

                    builder.append(mBuffer, start, mPosition - start - 1);
                    return builder.toString();
                }

                if (character == '\\')
                {
                    if (builder == null) builder = new StringBuilder();
                    builder.append(mBuffer, start, mPosition - start - 1);
                    builder.append(readEscapeCharacter());
                    start = mPosition;
                }
            }

            if (builder == null) builder = new StringBuilder();
            builder.append(mBuffer, start, mPosition - start);
        }
        while (fillBuffer(1));

        throw syntaxError("Unterminated string");
    }

    private String nextLiteral(boolean assignOffsetsOnly) throws IOException
    {
        StringBuilder builder = null;
        mValuePosition = -1;
        mValueLength = 0;
        int index = 0;

        while (true)
        {
            boolean terminated = false;
            for (; mPosition + index < mLimit; index++)
            {
                char character = mBuffer[mPosition + index];
                if (character == '/' || character == '\\' || character == ';' || character == '#' || character == '=')
                {
                    checkLenient();
                    terminated = true;
                    break;
                }
                if (character == '{' || character == '}' || character == '[' || character == ']' || character == ':' || character == ','
                    || character == ' ' || character == '\t' || character == '\f' || character == '\r' || character == '\n')
                {
                    terminated = true;
                    break;
                }
            }
            if (terminated) break;

            if (index < mBuffer.length)
            {
                if (fillBuffer(index + 1)) continue;
                break;
            }

            if (builder == null) builder = new StringBuilder();
            builder.append(mBuffer, mPosition, index);
            mValueLength += index;
            mPosition += index;
            index = 0;
            if (!fillBuffer(1)) break;
        }

        String result;
        if (assignOffsetsOnly && builder == null)
        {
            mValuePosition = mPosition;
            result = null;
        }
        else if (mSkipping)
        {
            result = "skipped!";
        }
        else if (builder == null)
        {
            result = new String(mBuffer, mPosition, index);
        }
        else
        {
            builder.append(mBuffer, mPosition, index);
            result = builder.toString();
        }

        mValueLength += index;
        mPosition += index;
        return result;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " near " + getSnippet();
    }

    private char readEscapeCharacter() throws IOException
    {
        if (mPosition == mLimit && !fillBuffer(1)) throw syntaxError("Unterminated escape sequence");

        char escaped = mBuffer[mPosition++];
        switch (escaped)
        {
            case 'u':
                if (mPosition + 4 > mLimit && !fillBuffer(4)) throw syntaxError("Unterminated escape sequence");

                String hex = new String(mBuffer, mPosition, 4);
                mPosition += 4;
                return (char)Integer.parseInt(hex, 16);
            case 't':
                return '\t';
            case 'b':
                return '\b';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 'f':
                return '\f';
            default:
                return escaped;
        }
    }

    private JsonToken readLiteral() throws IOException
    {
        mValue = nextLiteral(true);
        if (mValueLength == 0) throw syntaxError("Expected literal value");

        mToken = decodeLiteral();
        if (mToken == JsonToken.STRING) checkLenient();
        return mToken;
    }

    private JsonToken decodeLiteral() throws IOException
    {
        if (mValuePosition == -1) return JsonToken.STRING;

        if (literalEquals("null"))
        {
            mValue = "null";
            return JsonToken.NULL;
        }

        if (literalEquals(TRUE))
        {
            mValue = TRUE;
            return JsonToken.BOOLEAN;
        }

        if (literalEquals(FALSE))
        {
            mValue = FALSE;
            return JsonToken.BOOLEAN;
        }

        mValue = new String(mBuffer, mValuePosition, mValueLength);
        return decodeNumber(mBuffer, mValuePosition, mValueLength);
    }

    private boolean literalEquals(String keyword)
    {
        if (mValueLength != keyword.length()) return false;

        for (int index = 0; index < mValueLength; index++)
        {
            if (Character.toLowerCase(mBuffer[mValuePosition + index]) != keyword.charAt(index)) return false;
        }
        return true;
    }

    private static JsonToken decodeNumber(char[] characters, int offset, int length)
    {
        int end = offset + length;
        int index = offset;
        int character = charAt(characters, index, end);

        if (character == '-') character = charAt(characters, ++index, end);

        if (character == '0')
        {
            character = charAt(characters, ++index, end);
        }
        else if (character >= '1' && character <= '9')
        {
            character = charAt(characters, ++index, end);
            while (character >= '0' && character <= '9') character = charAt(characters, ++index, end);
        }
        else
        {
            return JsonToken.STRING;
        }

        if (character == '.')
        {
            character = charAt(characters, ++index, end);
            while (character >= '0' && character <= '9') character = charAt(characters, ++index, end);
        }

        if (character == 'e' || character == 'E')
        {
            character = charAt(characters, ++index, end);
            if (character == '+' || character == '-') character = charAt(characters, ++index, end);

            if (character >= '0' && character <= '9')
            {
                character = charAt(characters, ++index, end);
                while (character >= '0' && character <= '9') character = charAt(characters, ++index, end);
            }
            else
            {
                return JsonToken.STRING;
            }
        }

        return index == end ? JsonToken.NUMBER : JsonToken.STRING;
    }

    private static int charAt(char[] characters, int index, int end)
    {
        return index < end ? characters[index] : -1;
    }

    private IOException syntaxError(String message) throws IOException
    {
        throw new MalformedJsonException(message + " at line " + getLineNumber() + " column " + getColumnNumber());
    }

    private CharSequence getSnippet()
    {
        StringBuilder snippet = new StringBuilder();
        int before = Math.min(mPosition, 20);
        snippet.append(mBuffer, mPosition - before, before);
        int after = Math.min(mLimit - mPosition, 20);
        snippet.append(mBuffer, mPosition, after);
        return snippet;
    }
}
