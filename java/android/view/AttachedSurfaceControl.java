package android.view;

import android.graphics.Rect;
import android.graphics.Region;

import java.util.concurrent.Executor;

public interface AttachedSurfaceControl
{
    SurfaceControl.Transaction buildReparentTransaction(SurfaceControl child);

    boolean applyTransactionOnDraw(SurfaceControl.Transaction transaction);

    default int getBufferTransformHint()
    {
        return SurfaceControl.BUFFER_TRANSFORM_IDENTITY;
    }

    default void addOnBufferTransformHintChangedListener(OnBufferTransformHintChangedListener listener)
    {
    }

    default void removeOnBufferTransformHintChangedListener(OnBufferTransformHintChangedListener listener)
    {
    }

    default void setTouchableRegion(Region region)
    {
    }

    default void setChildBoundingInsets(Rect insets)
    {
    }

    default SurfaceControl.OnJankDataListenerRegistration registerOnJankDataListener(Executor executor,
                                                                                    SurfaceControl.OnJankDataListener listener)
    {
        return new SurfaceControl.OnJankDataListenerRegistration();
    }

    interface OnBufferTransformHintChangedListener
    {
        void onBufferTransformHintChanged(int hint);
    }
}
