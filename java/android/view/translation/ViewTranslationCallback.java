package android.view.translation;

import android.view.View;

public interface ViewTranslationCallback
{
    boolean onShowTranslation(View view);

    boolean onHideTranslation(View view);

    boolean onClearTranslation(View view);
}
