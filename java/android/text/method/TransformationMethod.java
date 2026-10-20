package android.text.method;

import android.graphics.Rect;
import android.view.View;

public interface TransformationMethod
{
    CharSequence getTransformation(CharSequence source, View view);

    void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect);
}
