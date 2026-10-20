package android.view;

import android.content.Context;

public class RuntimeLayoutInflater extends LayoutInflater
{
    public RuntimeLayoutInflater(Context context)
    {
        super(context);
    }

    @Override
    public LayoutInflater cloneInContext(Context newContext)
    {
        RuntimeLayoutInflater clone = new RuntimeLayoutInflater(newContext);
        if (getFactory2() != null) clone.setFactory2(getFactory2());
        else if (getFactory() != null) clone.setFactory(getFactory());
        clone.setFilter(getFilter());
        return clone;
    }
}
