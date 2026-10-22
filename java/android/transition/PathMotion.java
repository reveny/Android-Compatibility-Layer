package android.transition;

import android.content.Context;
import android.graphics.Path;
import android.util.AttributeSet;

public abstract class PathMotion
{
    public PathMotion()
    {
    }

    public PathMotion(Context context, AttributeSet attributes)
    {
    }

    public abstract Path getPath(float startX, float startY, float endX, float endY);
}
