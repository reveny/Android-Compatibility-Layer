package com.example.graphics;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class DrawablesActivity extends Activity
{
    private static final String TAG = "Drawables";
    private static final int PRESSED_STATE = 0x010100a7;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "DrawablesActivity.onCreate package=" + getPackageName());

        setContentView(new DrawablesView(this));
    }

    private static final class DrawablesView extends View
    {
        private final Paint mPaint = new Paint();

        DrawablesView(Activity activity)
        {
            super(activity);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            int width = getWidth();
            int height = getHeight();

            mPaint.setColor(0xFF141A26);
            canvas.drawRect(0, 0, width, height, mPaint);

            drawVector(canvas, R.drawable.icon_check, "check", 20, 20, 100);
            drawVector(canvas, R.drawable.icon_ring, "ring", 140, 20, 100);

            drawShape(canvas, R.drawable.panel, "panel", 20, 140, 160, 60);
            drawLayers(canvas, R.drawable.stack, "stack", 200, 140, 80, 80);
            drawSelector(canvas, R.drawable.toggle, "toggle", 300, 140, 80, 80);
        }

        private void drawVector(Canvas canvas, int id, String label, int x, int y, int size)
        {
            Drawable drawable = getResources().getDrawable(id);

            Log.i(TAG, label + " is " + (drawable == null ? "null" : drawable.getClass().getSimpleName()));
            if (drawable == null) return;

            Log.i(TAG, label + " intrinsic " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());

            drawable.setBounds(x, y, x + size, y + size);
            drawable.draw(canvas);
        }

        private void drawShape(Canvas canvas, int id, String label, int x, int y, int width, int height)
        {
            Drawable drawable = getResources().getDrawable(id);

            Log.i(TAG, label + " is " + (drawable == null ? "null" : drawable.getClass().getSimpleName()));
            if (!(drawable instanceof GradientDrawable)) return;

            drawable.setBounds(x, y, x + width, y + height);
            drawable.draw(canvas);
        }

        private void drawLayers(Canvas canvas, int id, String label, int x, int y, int width, int height)
        {
            Drawable drawable = getResources().getDrawable(id);

            Log.i(TAG, label + " is " + (drawable == null ? "null" : drawable.getClass().getSimpleName()));
            if (!(drawable instanceof LayerDrawable)) return;

            Log.i(TAG, label + " has " + ((LayerDrawable) drawable).getNumberOfLayers() + " layers");

            drawable.setBounds(x, y, x + width, y + height);
            drawable.draw(canvas);
        }

        private void drawSelector(Canvas canvas, int id, String label, int x, int y, int width, int height)
        {
            Drawable drawable = getResources().getDrawable(id);

            Log.i(TAG, label + " is " + (drawable == null ? "null" : drawable.getClass().getSimpleName()));
            if (!(drawable instanceof StateListDrawable)) return;

            StateListDrawable selector = (StateListDrawable) drawable;
            Log.i(TAG, label + " has " + selector.getStateCount() + " states, stateful=" + selector.isStateful());

            selector.setState(new int[0]);
            Drawable resting = selector.getCurrent();

            selector.setState(new int[] {PRESSED_STATE});
            Drawable pressed = selector.getCurrent();

            Log.i(TAG, label + " picks a different drawable when pressed=" + (resting != pressed));

            selector.setState(new int[0]);
            drawable.setBounds(x, y, x + width, y + height / 2 - 2);
            drawable.draw(canvas);

            selector.setState(new int[] {PRESSED_STATE});
            drawable.setBounds(x, y + height / 2 + 2, x + width, y + height);
            drawable.draw(canvas);
        }
    }
}
