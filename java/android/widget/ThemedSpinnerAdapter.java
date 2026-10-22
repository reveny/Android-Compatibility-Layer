package android.widget;

import android.content.res.Resources;

public interface ThemedSpinnerAdapter extends SpinnerAdapter
{
    void setDropDownViewTheme(Resources.Theme theme);

    Resources.Theme getDropDownViewTheme();
}
