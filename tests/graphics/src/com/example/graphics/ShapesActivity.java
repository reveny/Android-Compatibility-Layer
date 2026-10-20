package com.example.graphics;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ShapesActivity extends Activity
{
    private static final String TAG = "Shapes";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "ShapesActivity.onCreate package=" + getPackageName());

        setContentView(new ShapesView(this));
    }

    private static final class ShapesView extends View
    {
        private final Paint mPaint = new Paint();
        private final Path mPath = new Path();

        ShapesView(Activity activity)
        {
            super(activity);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            int width = getWidth();
            int height = getHeight();
            int entrySaveCount = canvas.getSaveCount();

            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0xFF101828);
            canvas.drawRect(0, 0, width, height, mPaint);

            drawShadowAndDashes(canvas, width, height);
            drawTriangle(canvas, width, height);
            drawRoundRect(canvas, width, height);
            drawGradient(canvas, width, height);
            drawClipped(canvas, width, height);
            drawLayer(canvas, width, height);
            drawStroked(canvas, width, height);
            drawThroughRenderNode(canvas, width, height);
            drawBitmaps(canvas, width, height);

            Log.i(TAG, "ShapesView.onDraw " + width + "x" + height);
            Log.i(TAG, "saveCount balanced=" + (canvas.getSaveCount() == entrySaveCount));
        }

        private void drawTriangle(Canvas canvas, int width, int height)
        {
            mPath.reset();
            mPath.moveTo(width * 0.25f, height * 0.06f);
            mPath.lineTo(width * 0.40f, height * 0.30f);
            mPath.lineTo(width * 0.10f, height * 0.30f);
            mPath.close();

            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0xFFE04050);
            canvas.drawPath(mPath, mPaint);
        }

        private void drawRoundRect(Canvas canvas, int width, int height)
        {
            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0xFF40C060);
            canvas.drawRoundRect(width * 0.55f, height * 0.06f, width * 0.92f, height * 0.30f, 18, 18, mPaint);
        }

        private void drawGradient(Canvas canvas, int width, int height)
        {
            LinearGradient gradient = new LinearGradient(width * 0.10f, 0, width * 0.45f, 0, 0xFF102080, 0xFF60D0FF,
                                                         Shader.TileMode.CLAMP);

            mPaint.setShader(gradient);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(width * 0.10f, height * 0.36f, width * 0.45f, height * 0.56f, mPaint);
            mPaint.setShader(null);
        }

        private void drawClipped(Canvas canvas, int width, int height)
        {
            canvas.save();

            Path clip = new Path();
            clip.addOval(width * 0.55f, height * 0.36f, width * 0.92f, height * 0.56f, Path.Direction.CW);
            canvas.clipPath(clip);

            RadialGradient radial = new RadialGradient(width * 0.73f, height * 0.46f, width * 0.20f, 0xFFFFC020,
                                                       0xFF803000, Shader.TileMode.CLAMP);
            mPaint.setShader(radial);
            canvas.drawRect(width * 0.50f, height * 0.32f, width * 0.98f, height * 0.60f, mPaint);
            mPaint.setShader(null);

            canvas.restore();
        }

        private void drawLayer(Canvas canvas, int width, int height)
        {
            int before = canvas.getSaveCount();

            canvas.saveLayerAlpha(width * 0.10f, height * 0.62f, width * 0.45f, height * 0.92f, 128);

            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0xFFFFFFFF);
            canvas.drawRect(width * 0.10f, height * 0.62f, width * 0.45f, height * 0.92f, mPaint);

            canvas.restore();

            Log.i(TAG, "layer balanced=" + (canvas.getSaveCount() == before));
        }

        private void drawBitmaps(Canvas canvas, int width, int height)
        {
            Bitmap tile = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < 8; y++)
            {
                for (int x = 0; x < 8; x++)
                {
                    boolean light = ((x / 4) + (y / 4)) % 2 == 0;
                    tile.setPixel(x, y, light ? 0xFF5A6478 : 0xFF2A3040);
                }
            }

            Log.i(TAG, "bitmap made " + tile.getWidth() + "x" + tile.getHeight() + " alpha=" + tile.hasAlpha());
            Log.i(TAG, "bitmap pixel readback=" + Integer.toHexString(tile.getPixel(0, 0)));

            Paint tiled = new Paint();
            tiled.setShader(new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
            canvas.drawRect(width * 0.10f, height * 0.62f, width * 0.28f, height * 0.92f, tiled);

            Bitmap scaled = Bitmap.createScaledBitmap(tile, 32, 32, false);
            Log.i(TAG, "bitmap scaled to " + scaled.getWidth() + "x" + scaled.getHeight());

            canvas.drawBitmap(scaled, null, new RectF(width * 0.30f, height * 0.62f, width * 0.45f, height * 0.80f), null);

            byte[] encoded = readAsset("badge.png");
            Log.i(TAG, "png asset bytes=" + (encoded == null ? -1 : encoded.length));

            Bitmap decoded = BitmapFactory.decodeByteArray(encoded, 0, encoded == null ? 0 : encoded.length);
            Log.i(TAG, "png decoded=" + (decoded != null));

            if (decoded != null)
            {
                Log.i(TAG, "png size " + decoded.getWidth() + "x" + decoded.getHeight() + " alpha=" + decoded.hasAlpha());
                canvas.drawBitmap(decoded, null, new RectF(width * 0.30f, height * 0.82f, width * 0.45f, height * 0.92f),
                                  null);
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(encoded, 0, encoded.length, bounds);
            Log.i(TAG, "png measured " + bounds.outWidth + "x" + bounds.outHeight + " type=" + bounds.outMimeType);
        }

        private byte[] readAsset(String name)
        {
            try (java.io.InputStream stream = getContext().getAssets().open(name))
            {
                java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[4096];

                int read;
                while ((read = stream.read(chunk)) > 0) collected.write(chunk, 0, read);

                return collected.toByteArray();
            }
            catch (java.io.IOException failure)
            {
                Log.i(TAG, "asset " + name + " unreadable: " + failure);
                return null;
            }
        }

        private void drawShadowAndDashes(Canvas canvas, int width, int height)
        {
            Paint shadow = new Paint();
            shadow.setColor(0xFF000000);
            shadow.setMaskFilter(new BlurMaskFilter(6, BlurMaskFilter.Blur.NORMAL));
            canvas.drawRoundRect(width * 0.50f, height * 0.06f, width * 0.87f, height * 0.30f, 18, 18, shadow);

            Paint dashes = new Paint();
            dashes.setColor(0xFFB0B8C8);
            dashes.setStyle(Paint.Style.STROKE);
            dashes.setStrokeWidth(2);
            dashes.setPathEffect(new DashPathEffect(new float[] {8, 6}, 0));

            Path line = new Path();
            line.moveTo(width * 0.06f, height * 0.335f);
            line.lineTo(width * 0.94f, height * 0.335f);
            canvas.drawPath(line, dashes);

            Log.i(TAG, "shadow and dashes drawn");
        }

        private void drawThroughRenderNode(Canvas canvas, int width, int height)
        {
            RenderNode node = new RenderNode("badge");
            node.setPosition(0, 0, 60, 60);

            RecordingCanvas recording = node.beginRecording(60, 60);

            Paint paint = new Paint();
            paint.setColor(0xFFFF40A0);
            recording.drawOval(0, 0, 60, 60, paint);

            paint.setColor(0xFF201030);
            recording.drawRect(24, 10, 36, 50, paint);

            node.endRecording();

            Log.i(TAG, "renderNode recorded=" + node.hasDisplayList());

            node.setTranslationX(width * 0.46f);
            node.setTranslationY(height * 0.70f);
            node.setScaleX(0.9f);
            node.setScaleY(0.9f);
            node.setRotationZ(15);
            node.setAlpha(0.85f);

            canvas.drawRenderNode(node);

            Matrix nodeMatrix = new Matrix();
            node.getMatrix(nodeMatrix);
            Log.i(TAG, "renderNode identity=" + node.hasIdentityMatrix() + " mapped="
                       + (nodeMatrix.isIdentity() ? "identity" : "transformed"));
        }

        private void drawStroked(Canvas canvas, int width, int height)
        {
            canvas.save();

            Matrix spin = new Matrix();
            spin.setRotate(20, width * 0.73f, height * 0.77f);
            canvas.concat(spin);

            mPath.reset();
            mPath.addRect(new RectF(width * 0.60f, height * 0.66f, width * 0.86f, height * 0.88f), Path.Direction.CW);

            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(6);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setColor(0xFF80E0FF);
            canvas.drawPath(mPath, mPaint);

            canvas.restore();
            mPaint.setStyle(Paint.Style.FILL);
        }
    }
}
