package android.content;

import android.net.Uri;

import java.util.List;

public class ContentUris
{
    public ContentUris()
    {
    }

    public static long parseId(Uri contentUri)
    {
        String last = contentUri.getLastPathSegment();
        return last == null ? -1 : Long.parseLong(last);
    }

    public static Uri.Builder appendId(Uri.Builder builder, long id)
    {
        return builder.appendEncodedPath(String.valueOf(id));
    }

    public static Uri withAppendedId(Uri contentUri, long id)
    {
        return appendId(contentUri.buildUpon(), id).build();
    }

    public static Uri removeId(Uri contentUri)
    {
        String last = contentUri.getLastPathSegment();
        if (last == null) throw new IllegalArgumentException("No path segments to remove");

        Long.parseLong(last);

        List<String> segments = contentUri.getPathSegments();
        Uri.Builder builder = contentUri.buildUpon();
        builder.path(null);

        for (int index = 0; index < segments.size() - 1; index++)
        {
            builder.appendPath(segments.get(index));
        }

        return builder.build();
    }
}
