package org.apache.http.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamEntity extends AbstractHttpEntity
{
    private static final int BUFFER_SIZE = 2048;

    private final InputStream content;
    private final long length;
    private boolean consumed = false;

    public InputStreamEntity(InputStream instream, long length)
    {
        if (instream == null) throw new IllegalArgumentException("Source input stream may not be null");

        this.content = instream;
        this.length = length;
    }

    @Override
    public boolean isRepeatable()
    {
        return false;
    }

    @Override
    public long getContentLength()
    {
        return length;
    }

    @Override
    public InputStream getContent() throws IOException
    {
        return content;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException
    {
        if (outstream == null) throw new IllegalArgumentException("Output stream may not be null");

        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream instream = content)
        {
            if (length < 0)
            {
                int read;
                while ((read = instream.read(buffer)) != -1) outstream.write(buffer, 0, read);
            }
            else
            {
                long remaining = length;
                while (remaining > 0)
                {
                    int read = instream.read(buffer, 0, (int)Math.min(BUFFER_SIZE, remaining));
                    if (read == -1) break;

                    outstream.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        }

        consumed = true;
    }

    @Override
    public boolean isStreaming()
    {
        return !consumed;
    }

    @Override
    public void consumeContent() throws IOException
    {
        consumed = true;
        content.close();
    }
}
