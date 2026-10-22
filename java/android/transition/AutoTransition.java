package android.transition;

import android.content.Context;
import android.util.AttributeSet;

public class AutoTransition extends TransitionSet
{
    public AutoTransition()
    {
        initialize();
    }

    public AutoTransition(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        initialize();
    }

    private void initialize()
    {
        setOrdering(ORDERING_SEQUENTIAL);
        addTransition(new Fade(Fade.OUT)).addTransition(new ChangeBounds()).addTransition(new Fade(Fade.IN));
    }
}
