package android.transition;

import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class TransitionValues
{
    public final Map<String, Object> values = new HashMap<>();
    public View view;

    public TransitionValues()
    {
    }

    public TransitionValues(View view)
    {
        this.view = view;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof TransitionValues)) return false;

        TransitionValues values = (TransitionValues)other;
        return view == values.view && this.values.equals(values.values);
    }

    @Override
    public int hashCode()
    {
        return 31 * (view == null ? 0 : view.hashCode()) + values.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("TransitionValues@").append(Integer.toHexString(System.identityHashCode(this))).append(":\n");
        builder.append("    view = ").append(view).append("\n");
        builder.append("    values:");

        for (Map.Entry<String, Object> entry : values.entrySet())
        {
            builder.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return builder.toString();
    }
}
