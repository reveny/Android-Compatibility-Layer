package android.view;

public interface WindowInsetsAnimationControlListener
{
    void onReady(WindowInsetsAnimationController controller, int types);

    void onFinished(WindowInsetsAnimationController controller);

    void onCancelled(WindowInsetsAnimationController controller);
}
