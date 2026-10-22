package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

public class ImageSwitcher extends ViewSwitcher
{
    public ImageSwitcher(Context context)
    {
        super(context);
    }

    public ImageSwitcher(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public void setImageResource(int resourceId)
    {
        ImageView image = (ImageView)getNextView();
        if (image == null) return;

        image.setImageResource(resourceId);
        showNext();
    }

    public void setImageURI(Uri uri)
    {
        ImageView image = (ImageView)getNextView();
        if (image == null) return;

        image.setImageURI(uri);
        showNext();
    }

    public void setImageDrawable(Drawable drawable)
    {
        ImageView image = (ImageView)getNextView();
        if (image == null) return;

        image.setImageDrawable(drawable);
        showNext();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ImageSwitcher.class.getName();
    }
}
