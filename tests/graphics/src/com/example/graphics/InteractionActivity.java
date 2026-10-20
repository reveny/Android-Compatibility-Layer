package com.example.graphics;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class InteractionActivity extends Activity
{
    private static final String TAG = "Interaction";

    private Panel mPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "InteractionActivity.onCreate package=" + getPackageName());

        mPanel = new Panel(this);
        setContentView(mPanel);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                runScript();
            }
        }, 120);
    }

    private void runScript()
    {
        Log.i(TAG, "draws before any input: " + mPanel.drawCount());

        mPanel.expectIdle();

        press(mPanel.first(), 30, 30);
        Log.i(TAG, "first target touched=" + mPanel.first().touched() + " focused=" + mPanel.first().isFocused());

        press(mPanel.second(), 30, 30);
        Log.i(TAG, "second target focused=" + mPanel.second().isFocused()
                   + " first still focused=" + mPanel.first().isFocused());

        twoFingers();
        Log.i(TAG, "the panel saw " + mPanel.mostPointers() + " pointers at once");

        cancel();
        Log.i(TAG, "the panel saw a cancel=" + mPanel.sawCancel());

        typeIntoField();
        runAnimation();

        int before = mPanel.drawCount();
        mPanel.invalidate();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                Log.i(TAG, "an invalidate produced " + (mPanel.drawCount() - before) + " more draw");
                Log.i(TAG, "final draw count " + mPanel.drawCount());

                finish();
            }
        }, 200);
    }

    private void typeIntoField()
    {
        EditText field = mPanel.field();
        InputMethodManager input = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        field.requestFocus();
        Log.i(TAG, "the field is a text editor=" + field.onCheckIsTextEditor() + " focused=" + field.isFocused());

        input.showSoftInput(field, 0);
        Log.i(TAG, "input started for the field=" + input.isActive(field));

        type(KeyEvent.KEYCODE_H);
        type(KeyEvent.KEYCODE_I);
        type(KeyEvent.KEYCODE_SPACE);
        type(KeyEvent.KEYCODE_5);

        Log.i(TAG, "the field now reads '" + field.getText() + "'");

        type(KeyEvent.KEYCODE_DEL);
        Log.i(TAG, "after a delete it reads '" + field.getText() + "'");

        mPanel.second().requestFocus();
        Log.i(TAG, "moving focus away left the field focused=" + field.isFocused());
    }

    private void type(int keyCode)
    {
        KeyEvent down = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        dispatchKeyEvent(down);
    }

    private void runAnimation()
    {
        final float[] seen = new float[2];
        final int[] updates = new int[1];

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 100f);
        animator.setDuration(60);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                updates[0]++;
                seen[1] = ((Float) animation.getAnimatedValue()).floatValue();
                if (updates[0] == 1) seen[0] = seen[1];
            }
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                Log.i(TAG, "the animation stepped more than once=" + (updates[0] > 1) + " and finished at "
                           + Math.round(seen[1]));
                Log.i(TAG, "the animation is still running=" + animation.isRunning());
            }
        });

        animator.start();
        Log.i(TAG, "the animation started running=" + animator.isRunning());
    }

    private void press(Target target, float localX, float localY)
    {
        float x = target.getLeft() + localX;
        float y = target.getTop() + localY;

        send(MotionEvent.ACTION_DOWN, x, y);
        send(MotionEvent.ACTION_UP, x, y);
    }

    private void send(int action, float x, float y)
    {
        MotionEvent event = MotionEvent.obtain(0, 0, action, x, y, 0);
        dispatchTouchEvent(event);
        event.recycle();
    }

    private void twoFingers()
    {
        float baseX = mPanel.first().getLeft() + 10;
        float baseY = mPanel.first().getTop() + 10;

        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[2];

        for (int index = 0; index < 2; index++)
        {
            properties[index] = new MotionEvent.PointerProperties();
            properties[index].id = index;
            properties[index].toolType = MotionEvent.TOOL_TYPE_FINGER;

            coordinates[index] = new MotionEvent.PointerCoords();
            coordinates[index].x = baseX + index * 10;
            coordinates[index].y = baseY + index * 10;
            coordinates[index].pressure = 1.0f;
        }

        send(MotionEvent.ACTION_DOWN, properties, coordinates, 1);
        send(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), properties, coordinates, 2);
        send(MotionEvent.ACTION_MOVE, properties, coordinates, 2);
        send(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), properties, coordinates, 2);
        send(MotionEvent.ACTION_UP, properties, coordinates, 1);
    }

    private void send(int action, MotionEvent.PointerProperties[] properties, MotionEvent.PointerCoords[] coordinates,
                      int pointerCount)
    {
        MotionEvent event = MotionEvent.obtain(0, 0, action, pointerCount, properties, coordinates, 0, 0, 1.0f, 1.0f, 0, 0,
                                               0x1002, 0);
        dispatchTouchEvent(event);
        event.recycle();
    }

    private void cancel()
    {
        float x = mPanel.first().getLeft() + 5;
        float y = mPanel.first().getTop() + 5;

        send(MotionEvent.ACTION_DOWN, x, y);

        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, x, y, 0);
        dispatchTouchEvent(event);
        event.recycle();
    }

    private static final class Panel extends ViewGroup
    {
        private final Paint mPaint = new Paint();
        private final Target mFirst;
        private final Target mSecond;
        private final EditText mField;

        private int mDrawCount;
        private int mMostPointers;
        private boolean mSawCancel;
        private int mIdleDrawCount = -1;

        Panel(Activity activity)
        {
            super(activity);

            mFirst = new Target(activity, 0xFF3F7FD0);
            mSecond = new Target(activity, 0xFFD07F3F);
            mField = new EditText(activity);

            addView(mFirst);
            addView(mSecond);
            addView(mField);
        }

        EditText field()
        {
            return mField;
        }

        Target first()
        {
            return mFirst;
        }

        Target second()
        {
            return mSecond;
        }

        int drawCount()
        {
            return mDrawCount;
        }

        int mostPointers()
        {
            return mMostPointers;
        }

        boolean sawCancel()
        {
            return mSawCancel;
        }

        void expectIdle()
        {
            mIdleDrawCount = mDrawCount;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom)
        {
            mFirst.layout(20, 20, 120, 120);
            mSecond.layout(160, 20, 260, 120);
            mField.layout(20, 160, 300, 200);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event)
        {
            if (event.getPointerCount() > mMostPointers) mMostPointers = event.getPointerCount();
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) mSawCancel = true;

            return super.dispatchTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            mDrawCount++;

            mPaint.setColor(0xFF16202E);
            canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);

            if (mDrawCount < 3) invalidate();
        }
    }

    private static final class Target extends View
    {
        private final Paint mPaint = new Paint();
        private final int mColor;

        private boolean mTouched;

        Target(Activity activity, int color)
        {
            super(activity);

            mColor = color;
            setFocusable(true);
        }

        boolean touched()
        {
            return mTouched;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
            {
                mTouched = true;
                requestFocus();
            }

            return true;
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            mPaint.setColor(isFocused() ? 0xFFFFFFFF : mColor);
            canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
        }
    }
}
