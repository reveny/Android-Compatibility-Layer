package android.content;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class UriMatcher
{
    public static final int NO_MATCH = -1;

    private static final int EXACT = 0;
    private static final int NUMBER = 1;
    private static final int TEXT = 2;

    private int mCode;
    private final int mWhich;
    private final String mText;
    private final ArrayList<UriMatcher> mChildren;

    public UriMatcher(int code)
    {
        mCode = code;
        mWhich = -1;
        mChildren = new ArrayList<>();
        mText = null;
    }

    private UriMatcher(int which, String text)
    {
        mCode = NO_MATCH;
        mWhich = which;
        mChildren = new ArrayList<>();
        mText = text;
    }

    public void addURI(String authority, String path, int code)
    {
        if (code < 0) throw new IllegalArgumentException("code " + code + " is invalid: it must be positive");

        String[] tokens = null;
        if (path != null)
        {
            String newPath = path;
            if (path.length() > 1 && path.charAt(0) == '/') newPath = path.substring(1);
            tokens = newPath.split("/");
        }

        int numTokens = tokens != null ? tokens.length : 0;
        UriMatcher node = this;

        for (int index = -1; index < numTokens; index++)
        {
            String token = index < 0 ? authority : tokens[index];
            ArrayList<UriMatcher> children = node.mChildren;
            int numChildren = children.size();
            UriMatcher child;
            int childIndex;

            for (childIndex = 0; childIndex < numChildren; childIndex++)
            {
                child = children.get(childIndex);
                if (token.equals(child.mText))
                {
                    node = child;
                    break;
                }
            }

            if (childIndex == numChildren)
            {
                child = createChild(token);
                node.mChildren.add(child);
                node = child;
            }
        }

        node.mCode = code;
    }

    private static UriMatcher createChild(String token)
    {
        switch (token)
        {
            case "#":
                return new UriMatcher(NUMBER, "#");
            case "*":
                return new UriMatcher(TEXT, "*");
            default:
                return new UriMatcher(EXACT, token);
        }
    }

    public int match(Uri uri)
    {
        List<String> pathSegments = uri.getPathSegments();
        int length = pathSegments.size();

        UriMatcher node = this;

        if (length == 0 && uri.getAuthority() == null) return mCode;

        for (int index = -1; index < length; index++)
        {
            String part = index < 0 ? uri.getAuthority() : pathSegments.get(index);
            ArrayList<UriMatcher> list = node.mChildren;
            if (list == null) break;

            node = null;
            int listSize = list.size();

            for (int childIndex = 0; childIndex < listSize; childIndex++)
            {
                UriMatcher candidate = list.get(childIndex);

                matching:
                switch (candidate.mWhich)
                {
                    case EXACT:
                        if (candidate.mText.equals(part)) node = candidate;
                        break;
                    case NUMBER:
                        int partLength = part.length();
                        for (int position = 0; position < partLength; position++)
                        {
                            char character = part.charAt(position);
                            if (character < '0' || character > '9') break matching;
                        }
                        node = candidate;
                        break;
                    case TEXT:
                        node = candidate;
                        break;
                    default:
                        break;
                }

                if (node != null) break;
            }

            if (node == null) return NO_MATCH;
        }

        return node.mCode;
    }
}
