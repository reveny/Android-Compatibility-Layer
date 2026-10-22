package android.view;

public interface ScrollFeedbackProvider
{
    static ScrollFeedbackProvider createProvider(View view)
    {
        return new ScrollFeedbackProvider()
        {
            @Override
            public void onSnapToItem(int inputDeviceId, int source, int axis)
            {
            }

            @Override
            public void onScrollLimit(int inputDeviceId, int source, int axis, boolean isStart)
            {
            }

            @Override
            public void onScrollProgress(int inputDeviceId, int source, int axis, int deltaInPixels)
            {
            }
        };
    }

    void onSnapToItem(int inputDeviceId, int source, int axis);

    void onScrollLimit(int inputDeviceId, int source, int axis, boolean isStart);

    void onScrollProgress(int inputDeviceId, int source, int axis, int deltaInPixels);
}
