package com.soundcloud.android.crop.example;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;

import com.soundcloud.android.crop.CropImageView;
import com.soundcloud.android.crop.HighlightView;
import com.soundcloud.android.crop.ImageAreaPickerActivity;

/**
 * example form custom ImageAreaPickerActivity
 * Created by h46incon on 2014/9/2.
 */
public class TextPlacerActivity extends ImageAreaPickerActivity{
    private CropImageView imageView;
    private HighlightView pickerView;
    private final String text = "Text-g";
    private float initTextSize;
    private Rect initTextBounds;
    private float initTextAspectRatio;
    private Resources resources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placer);
        initViews();
        getInitTextBounds();

        // Check if the background is available
        if (rotateBitmap == null) {
            finish();
            return;
        }

        float width = 1;
        float height = width * initTextAspectRatio;

        if (height > 1) {
            height = 1;
            width = height / initTextAspectRatio;
        }

        final float part = 0.4f;
        width *= part;
        height *= part;
        RectF area = new RectF(0, 0, width, height);
        area.offset(
                0.5f - area.centerX(),
                0.5f - area.centerY()
        );


        setupPickerView(imageView, area);
        // Configure highlight view
        pickerView = getPickerView();
        pickerView.setNeedCenterBaseOnThis(false);
        pickerView.setShowThirds(false);

        final Paint paint = new Paint();
        paint.setColor(Color.RED);
//        paint.setAlpha(100);
        final Rect targetRect = new Rect();

        pickerView.setOnDrawFinshed(new HighlightView.OnDrawFinished() {
            @Override
            public void onDrawFinished(HighlightView v, Canvas canvas) {
                RectF vArea = v.getCropRectOnScreen();

                double yRation = (double)vArea.height() / (double) initTextBounds.height();
                double newTextSize = initTextSize * yRation;
                paint.setTextSize((float)newTextSize);
//                paint.setTextScaleX(1f);
//                paint.getTextBounds(text, 0, text.length(), targetRect);
                double xTemp = initTextBounds.width() * yRation;
//                Log.d("temp", String.format("xC:%f,xB:%d", (float)xTemp, targetRect.width()));
//                paint.setTextScaleX(1f);
//                paint.getTextBounds(text, 0, text.length(), targetRect);
//                int height = targetRect.height();
//                int width = targetRect.width();
//                Log.d("testUnscaleWidth",
//                        String.format("h:%d,w:%d,r:%f", height, width, (float) height / width));

                double xScale = vArea.width() / xTemp;

                //Log.d("testSize", "tb left" + targetRect.left);
                // Log.d("testSize", String.format("H: %f, s %f, xs %f", vArea.height(), newTextSize, xScale));

                paint.setTextScaleX((float)xScale);
                paint.getTextBounds(text, 0, text.length(), targetRect);
                Log.d("test", String.format("wG:%d, wT:%f, wC:%f", targetRect.width(), vArea.width(), xTemp * xScale));
                int desent = targetRect.bottom;
//                int baseline = targetRect.top + (targetRect.bottom - targetRect.top - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
//                canvas.drawRect(vArea, paint);
                canvas.drawText(text, vArea.left, vArea.bottom - desent, paint);
            }
        });

        // start picker
        startPicker();
    }

    private void getInitTextBounds() {
        initTextSize = 1024f;
        Paint paint = new Paint();
        paint.setTextSize(initTextSize);
        initTextBounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), initTextBounds);
        Log.d("test", "tb h " + initTextBounds.height());
        initTextAspectRatio = initTextBounds.height() / (float) initTextBounds.width();
    }

    void initViews() {
        resources = getResources();
        imageView = (CropImageView) findViewById(R.id.crop_image_view);
    }

    @Override
    protected void setResultException(Throwable throwable) {

    }

    @Override
    protected boolean respondTouchEvent() {
        return true;
    }
}
