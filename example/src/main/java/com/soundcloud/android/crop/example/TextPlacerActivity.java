package com.soundcloud.android.crop.example;

import android.graphics.Canvas;
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
    CropImageView imageView;
    HighlightView pickerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placer);
        initViews();

        // Check if the background is available
        if (rotateBitmap == null) {
            finish();
            return;
        }

        setupPickerView(imageView);
        // Configure highlight view
        pickerView = getPickerView();
        pickerView.setNeedCenterBaseOnThis(false);
        pickerView.setShowThirds(false);
        pickerView.setOnDrawFinshed(new HighlightView.OnDrawFinished() {
            @Override
            public void onDrawFinished(HighlightView v, Canvas canvas) {
                Log.d("TempTag", "on draw finished.");
            }
        });

        // start picker
        startPicker();
    }

    void initViews() {
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
