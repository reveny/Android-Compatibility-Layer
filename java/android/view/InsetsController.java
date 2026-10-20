package android.view;

public final class InsetsController implements WindowInsetsController
{
    private int mAppearance;
    private int mBehavior = BEHAVIOR_DEFAULT;

    @Override
    public void setSystemBarsAppearance(int appearance, int mask)
    {
        mAppearance = (mAppearance & ~mask) | (appearance & mask);
    }

    @Override
    public int getSystemBarsAppearance()
    {
        return mAppearance;
    }

    @Override
    public void setSystemBarsBehavior(int behavior)
    {
        mBehavior = behavior;
    }

    @Override
    public int getSystemBarsBehavior()
    {
        return mBehavior;
    }

    @Override
    public void show(int types)
    {
    }

    @Override
    public void hide(int types)
    {
    }
}
