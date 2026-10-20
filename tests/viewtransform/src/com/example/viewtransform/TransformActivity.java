package com.example.viewtransform;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class TransformActivity extends Activity
{
    private static final String TAG = "TransformFixture";

    private static final int BACKGROUND = 0xFF000000;

    private static final int PLAIN_COLOUR = 0xFFFF0000;
    private static final int ALPHA_COLOUR = 0xFF00FF00;
    private static final int ROTATED_COLOUR = 0xFF0000FF;
    private static final int SCALED_COLOUR = 0xFFFFFF00;
    private static final int SUBPIXEL_COLOUR = 0xFF00FFFF;
    private static final int SCROLLED_COLOUR = 0xFFFF00FF;

    private static final int BOX = 40;

    public static final class Box extends View
    {
        private final Paint mPaint = new Paint();

        public Box(Context context, int colour)
        {
            super(context);
            mPaint.setColor(colour);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
        }
    }

    public static final class Board extends ViewGroup
    {
        public Board(Context context)
        {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom)
        {
            for (int index = 0; index < getChildCount(); index++)
            {
                View child = getChildAt(index);
                int x = 20 + (index % 3) * 70;
                int y = 20 + (index / 3) * 70;

                child.layout(x, y, x + BOX, y + BOX);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            for (int index = 0; index < getChildCount(); index++)
            {
                getChildAt(index).measure(View.MeasureSpec.makeMeasureSpec(BOX, View.MeasureSpec.EXACTLY),
                                          View.MeasureSpec.makeMeasureSpec(BOX, View.MeasureSpec.EXACTLY));
            }

            setMeasuredDimension(resolveSize(240, widthMeasureSpec), resolveSize(180, heightMeasureSpec));
        }
    }

    private Box mScrolledHost;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);

        Board board = new Board(this);

        Box plain = new Box(this, PLAIN_COLOUR);

        Box faded = new Box(this, ALPHA_COLOUR);
        faded.setAlpha(0.5f);

        Box rotated = new Box(this, ROTATED_COLOUR);
        rotated.setRotation(30.0f);

        Box scaled = new Box(this, SCALED_COLOUR);
        scaled.setScaleX(1.5f);
        scaled.setScaleY(1.5f);

        Box shifted = new Box(this, SUBPIXEL_COLOUR);
        shifted.setTranslationX(12.5f);
        shifted.setTranslationY(6.5f);

        Box scrolled = new Box(this, SCROLLED_COLOUR);

        board.addView(plain);
        board.addView(faded);
        board.addView(rotated);
        board.addView(scaled);
        board.addView(shifted);
        board.addView(scrolled);

        root.addView(board);
        setContentView(root);

        report(plain, faded, rotated, scaled, shifted);
    }

    private void report(View plain, View faded, View rotated, View scaled, View shifted)
    {
        Log.i(TAG, "alpha is " + faded.getAlpha());
        Log.i(TAG, "rotation is " + rotated.getRotation());
        Log.i(TAG, "scale is " + scaled.getScaleX() + "x" + scaled.getScaleY());
        Log.i(TAG, "translation is " + shifted.getTranslationX() + "," + shifted.getTranslationY());
        Log.i(TAG, "transform fixture laid out");
    }
}
