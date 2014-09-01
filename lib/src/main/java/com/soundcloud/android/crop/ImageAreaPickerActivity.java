package com.soundcloud.android.crop;

import android.graphics.Bitmap;
import android.view.View;

/**
 * Created by h46incon on 2014/9/1.
 */
public abstract class ImageAreaPickerActivity extends MonitoredActivity {
    protected CropImageView imageView;

    protected void initViews() {
        imageView = (CropImageView) findViewById(R.id.crop_image);
        imageView.context = this;
        imageView.setRecycler(new ImageViewTouchBase.Recycler() {
            @Override
            public void recycle(Bitmap b) {
                b.recycle();
                System.gc();
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onDone();
            }
        });
    }

    /**
     *  This method will be called when DONE button is clicked.
     */
    protected void onDone(){
        return;
    }
}
