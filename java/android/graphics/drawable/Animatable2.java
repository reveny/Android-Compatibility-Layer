package android.graphics.drawable;

public interface Animatable2 extends Animatable
{
    void registerAnimationCallback(AnimationCallback callback);

    boolean unregisterAnimationCallback(AnimationCallback callback);

    void clearAnimationCallbacks();

    abstract class AnimationCallback
    {
        public AnimationCallback()
        {
        }

        public void onAnimationStart(Drawable drawable)
        {
        }

        public void onAnimationEnd(Drawable drawable)
        {
        }
    }
}
