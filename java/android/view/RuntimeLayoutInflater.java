package android.view;

import android.content.Context;
import android.util.AttributeSet;

public class RuntimeLayoutInflater extends LayoutInflater
{
    private static final String[] CLASS_PREFIXES = {"android.widget.", "android.webkit.", "android.app."};

    public RuntimeLayoutInflater(Context context)
    {
        super(context);
    }

    protected RuntimeLayoutInflater(LayoutInflater original, Context newContext)
    {
        super(original, newContext);
    }

    @Override
    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException
    {
        for (String prefix : CLASS_PREFIXES)
        {
            try
            {
                View view = createView(name, prefix, attrs);
                if (view != null) return view;
            }
            catch (ClassNotFoundException absent)
            {
            }
        }

        return super.onCreateView(name, attrs);
    }

    @Override
    public LayoutInflater cloneInContext(Context newContext)
    {
        return new RuntimeLayoutInflater(this, newContext);
    }
}
