package com.kircherelectronics.compassexplorer.gauge;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;

import android.view.View;

import com.kircherelectronics.compassexplorer.R;

/**
 * Draws an analog calibration gauge for displaying magnetic field measurements
 * from device sensors.
 *
 * @author Kaleb
 */
public final class GaugeCalibration extends View {

    private static final String TAG = GaugeCalibration.class
            .getSimpleName();

    private static final float POINT_RADIUS = 0.01f;

    private BitmapFactory.Options rimBitmapOptions;
    private BitmapFactory.Options scaleBitmapOptions;

    private RectF faceRect;

    // drawing tools
    private RectF rimRect;
    private Paint rimPaint;
    private Bitmap rimBitmap;
    private Matrix rimMatrix;
    private float rimScale;

    private Paint scalePaint;
    private Bitmap scaleBitmap;
    private Matrix scaleMatrix;
    private float scaleScale;

    private Paint backgroundPaint;
    // end drawing tools

    private Bitmap background; // holds the cached static part

    private Paint pointPaint;

    private float x;
    private float y;
    private float xOld = 0;
    private float yOld = 0;

    private LinkedList<Float> xList;
    private LinkedList<Float> yList;
    private LinkedList<Integer> colorList;

    private String title = "Magnetic Field";

    /**
     * Create a new instance.
     *
     * @param context
     */
    public GaugeCalibration(Context context) {
        super(context);
        init();
    }

    /**
     * Create a new instance.
     *
     * @param context
     * @param attrs
     */
    public GaugeCalibration(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Create a new instance.
     *
     * @param context
     * @param attrs
     * @param defStyle
     */
    public GaugeCalibration(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setValue(float x, float y, int color) {
        if ((x > xOld + 1 || x < xOld - 1) || (y > yOld + 1 || y < yOld - 1)) {
            xOld = x;
            yOld = y;

            this.x = ((faceRect.right - faceRect.left) / 200) * x
                    + faceRect.centerX();
            this.y = ((faceRect.bottom - faceRect.top) / 200) * y
                    + faceRect.centerY();

            xList.addFirst(this.x);
            yList.addFirst(this.y);
            colorList.addFirst(color);

            if (xList.size() > 10000) {
                xList.removeLast();
                yList.removeLast();
                colorList.removeLast();
            }

            invalidate();
        }


    }

    /**
     * Run the instance. This can be thought of as onDraw().
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Important! Keeps lines crisp and ensures the canvas is cleared.
        canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);

        drawBackground(canvas);

        float scale = (float) getWidth();
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.scale(scale, scale);

        drawPoint(canvas);

        canvas.restore();
    }

    public void clearAllPoints() {
        xList.removeAll(xList);
        yList.removeAll(yList);
        colorList.removeAll(colorList);
    }

    public void drawPoint(Canvas canvas) {
        for (int i = 0; i < xList.size(); i++) {
            float tempX = xList.get(i);
            float tempY = yList.get(i);
            pointPaint.setColor(colorList.get(i));

            canvas.drawCircle(tempX, tempY, POINT_RADIUS, pointPaint);
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        Parcelable superState = bundle.getParcelable("superState");
        super.onRestoreInstanceState(superState);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        Bundle state = new Bundle();
        state.putParcelable("superState", superState);

        return state;
    }

    protected void init() {
        xList = new LinkedList<>();
        yList = new LinkedList<>();
        colorList = new LinkedList<>();

        initDrawingTools();
    }

    protected void initDrawingTools() {
        rimBitmapOptions = new BitmapFactory.Options();

        rimBitmapOptions.inBitmap = rimBitmap;
        rimBitmapOptions.inMutable = true;
        rimBitmapOptions.inSampleSize = 1;
        rimBitmapOptions.inPurgeable = true;

        scaleBitmapOptions = new BitmapFactory.Options();

        scaleBitmapOptions.inBitmap = scaleBitmap;
        scaleBitmapOptions.inMutable = true;
        scaleBitmapOptions.inSampleSize = 1;
        scaleBitmapOptions.inPurgeable = true;

        rimRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);

        rimPaint = new Paint();
        rimPaint.setFilterBitmap(true);
        rimPaint.setFlags(Paint.FILTER_BITMAP_FLAG);
        rimPaint.setAntiAlias(true);
        rimBitmap = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.gauge_compass_ring_holo, rimBitmapOptions);
        rimMatrix = new Matrix();
        rimScale = (1.0f / rimBitmap.getWidth()) * .8f;
        rimMatrix.setScale(rimScale, rimScale);

        scalePaint = new Paint();
        scalePaint.setFilterBitmap(true);
        scalePaint.setFlags(Paint.FILTER_BITMAP_FLAG);
        scalePaint.setAntiAlias(true);
        scaleBitmap = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.gauge_compass_calibrate_guide_holo,
                scaleBitmapOptions);
        scaleMatrix = new Matrix();
        scaleScale = (1.0f / scaleBitmap.getWidth()) * .7f;
        scaleMatrix.setScale(scaleScale, scaleScale);

        float rimSize = 0.02f;
        faceRect = new RectF();
        faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize,
                rimRect.right - rimSize, rimRect.bottom - rimSize);

        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setFlags(Paint.FILTER_BITMAP_FLAG);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setFilterBitmap(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);

        int chosenDimension = Math.min(chosenWidth, chosenHeight);

        setMeasuredDimension(chosenDimension, chosenDimension);
    }

    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredSize();
        }
    }

    // in case there is no size specified
    private int getPreferredSize() {
        return 300;
    }

    /**
     * Draw the rim of the gauge.
     *
     * @param canvas
     */
    private void drawRim(Canvas canvas) {
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.translate(0.5f - rimBitmap.getWidth() * rimScale / 2.0f, 0.5f
                - rimBitmap.getHeight() * rimScale / 2.0f);

        canvas.drawBitmap(rimBitmap, rimMatrix, rimPaint);
        canvas.restore();
    }

    private void drawScale(Canvas canvas) {
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.translate(0.5f - scaleBitmap.getWidth() * scaleScale / 2.0f,
                0.5f - scaleBitmap.getHeight() * scaleScale / 2.0f);
        canvas.drawBitmap(scaleBitmap, scaleMatrix, scalePaint);
        canvas.restore();
    }

    private void drawBackground(Canvas canvas) {
        if (background == null) {
            Log.w(TAG, "Background not created");
        } else {
            canvas.drawBitmap(background, 0, 0, backgroundPaint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "Size changed to " + w + "x" + h);

        regenerateBackground();
    }

    private void regenerateBackground() {
        // free the old bitmap
        if (background != null) {
            background.recycle();
        }

        background = Bitmap.createBitmap(getWidth(), getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas backgroundCanvas = new Canvas(background);
        float scale = (float) getWidth();
        backgroundCanvas.scale(scale, scale);

        drawRim(backgroundCanvas);
        drawScale(backgroundCanvas);
    }

}
