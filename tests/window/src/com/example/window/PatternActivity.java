package com.example.window;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class PatternActivity extends Activity
{
    private static final String TAG = "Pattern";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "PatternActivity.onCreate package=" + getPackageName());

        PatternView view = new PatternView(this);
        view.setId(0x7f000001);
        setContentView(view);

        View found = findViewById(0x7f000001);
        Log.i(TAG, "findViewById=" + (found == view));
        Log.i(TAG, "decorView=" + (getWindow().getDecorView() != null));
    }

    private static final class PatternView extends View
    {
        private final Paint mPaint = new Paint();

        PatternView(Activity activity)
        {
            super(activity);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            int width = getWidth();
            int height = getHeight();

            mPaint.setColor(0xFF101828);
            canvas.drawRect(0, 0, width, height, mPaint);

            mPaint.setColor(0xFFE04050);
            canvas.drawRect(width / 8, height / 8, width / 2, height / 2, mPaint);

            mPaint.setColor(0xFF40C060);
            canvas.drawRect(width / 2, height / 2, width - width / 8, height - height / 8, mPaint);

            mPaint.setColor(0xFF4080F0);
            canvas.drawRect(width / 4, height - height / 4, width - width / 4, height - height / 8, mPaint);

            Log.i(TAG, "PatternView.onDraw " + width + "x" + height);
        }
    }
}
