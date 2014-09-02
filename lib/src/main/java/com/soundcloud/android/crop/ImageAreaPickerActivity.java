package com.soundcloud.android.crop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Bundle;
import android.os.Handler;

import com.soundcloud.android.crop.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Created by h46incon on 2014/9/1.
 */
public abstract class ImageAreaPickerActivity extends MonitoredActivity {
    private static final int SIZE_DEFAULT = 2048;
    private static final int SIZE_LIMIT = 4096;
    private final Handler handler = new Handler();
    private CropImageView imageView;
    private HighlightView pickerView;

    private boolean isPickerViewSetup = false;

//    private int aspectX;
//    private int aspectY;
    private Float aspectRatio;      // height : width
    private int sampleSize;

    /* those method is init when onCreate */
    protected RotateBitmap rotateBitmap;
    protected int exifRotation;
    protected Uri sourceUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupFromIntent();
    }

    /**
     * get selected area in Image.
     * @return null is not starting crop
     */
    protected Rect getImageSelectedArea() {
        if (pickerView == null) {
            return null;
        }
        return pickerView.getScaledCropRect(sampleSize);
    }

    protected int getSampleSize() {
        return sampleSize;
    }

    protected float getScaleFromImageToScreen() {
        if (imageView == null) {
            return sampleSize;
        } else {
            return sampleSize * imageView.getScale();
        }
    }

    protected HighlightView getPickerView() {
        return pickerView;
    }

    protected boolean isPickerViewSetup() {
        return isPickerViewSetup;
    }

    protected void setupPickerView(CropImageView cropImageView) {
        setupPickerView(cropImageView, Float.NaN, null);
    }

    protected void setupPickerView(CropImageView cropImageView, Rect initImageArea) {
        setupPickerView(cropImageView, initImageArea, false);
    }

    protected void setupPickerView(CropImageView cropImageView,
                                   Rect initImageArea, boolean maintainAspectRatio) {
        Float ratio = Float.NaN;
        if (maintainAspectRatio) {
            ratio = (float)initImageArea.height() / initImageArea.width();
        }

        setupPickerView(cropImageView, ratio, initImageArea);
    }

    protected void setupPickerView(CropImageView cropImageView, float aspectRatio) {
        setupPickerView(cropImageView, aspectRatio, null);
    }

    /**
     *  @param aspectRatio if equals to Float.NaN, it will not maintain aspect ratio
     *  @param initImageArea if equals to null, it will use default area
     *  Note: if aspectRatio != Float.NaN and initImageArea != null, it will maintain aspect ratio,
     *                  but use aspect ratio values in INITAREA, and IGNORE aspectRatio
     */
    private void setupPickerView(CropImageView cropImageView,
                                   float aspectRatio, Rect initImageArea) {
        if (isFinishing()) {
            return;
        }

        this.aspectRatio = aspectRatio;
        imageView = cropImageView;
        imageView.context = this;
        imageView.setRecycler(new ImageViewTouchBase.Recycler() {
            @Override
            public void recycle(Bitmap b) {
                b.recycle();
                System.gc();
            }
        });

        this.imageView.setImageRotateBitmapResetBase(rotateBitmap, true);
        pickerView = setupDefaultPickerView(initImageArea);
        isPickerViewSetup = true;
    }

    public void startPicker() {
        if (!isPickerViewSetup) {
            Log.e("The picker view has not been setup, call 'setupPickerView' first");
            return;
        }

        CropUtil.startBackgroundJob(this, null, getResources().getString(R.string.crop__wait),
                new Runnable() {
                    public void run() {
                        final CountDownLatch latch = new CountDownLatch(1);
                        handler.post(new Runnable() {
                            public void run() {
                                if (imageView.getScale() == 1F) {
                                    imageView.center(true, true);
                                }
                                latch.countDown();
                            }
                        });
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        new AreaPicker().start();
                    }
                }, handler
        );
    }

    /**
     * When an exception occurs, it will call this method to put message in result
     *
     * @param throwable exception occurred.
     */
    protected abstract void setResultException(Throwable throwable);

    protected abstract boolean respondTouchEvent();

    private static int getMaxImageSize() {
        int textureLimit = getMaxTextureSize();
        if (textureLimit == 0) {
            return SIZE_DEFAULT;
        } else {
            return Math.min(textureLimit, SIZE_LIMIT);
        }
    }

    private static int getMaxTextureSize() {
        // The OpenGL texture size is the maximum size that can be drawn in an ImageView
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        return maxSize[0];
    }

    private int calculateBitmapSampleSize(Uri bitmapUri) throws IOException {
        InputStream is = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = getContentResolver().openInputStream(bitmapUri);
            BitmapFactory.decodeStream(is, null, options); // Just get image size
        } finally {
            CropUtil.closeSilently(is);
        }

        int maxSize = getMaxImageSize();
        int sampleSize = 1;
        while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize << 1;
        }
        return sampleSize;
    }

    private void setupFromIntent() {
        Intent intent = getIntent();

        sourceUri = intent.getData();
        if (sourceUri != null) {
            exifRotation = CropUtil.getExifRotation(CropUtil.getFromMediaUri(getContentResolver(), sourceUri));

            InputStream is = null;
            try {
                sampleSize = calculateBitmapSampleSize(sourceUri);
                is = getContentResolver().openInputStream(sourceUri);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = sampleSize;
                rotateBitmap = new RotateBitmap(BitmapFactory.decodeStream(is, null, option), exifRotation);
            } catch (IOException e) {
                Log.e("Error reading image: " + e.getMessage(), e);
                setResultException(e);
            } catch (OutOfMemoryError e) {
                Log.e("OOM reading image: " + e.getMessage(), e);
                setResultException(e);
            } finally {
                CropUtil.closeSilently(is);
            }
        }
    }

    private HighlightView setupDefaultPickerView(Rect initCropRect) {
        if (rotateBitmap == null) {
            return null;
        }

        HighlightView hv = new HighlightView(imageView);
        final int width = rotateBitmap.getWidth();
        final int height = rotateBitmap.getHeight();

        Rect imageRect = new Rect(0, 0, width, height);
        Rect cropRect = initCropRect;

        if (cropRect == null) {
            // Make the default size about 4/5 of the width or height
            int cropWidth = Math.min(width, height) * 4 / 5;
            @SuppressWarnings("SuspiciousNameCombination")
            int cropHeight = cropWidth;

            if (!aspectRatio.isNaN()) {
                if (aspectRatio < 1) {
                    cropHeight = (int) (cropWidth * aspectRatio);
                } else {
                    cropWidth = (int) (cropHeight / aspectRatio);
                }

            }

            int x = (width - cropWidth) / 2;
            int y = (height - cropHeight) / 2;

            cropRect = new Rect(x, y, x + cropWidth, y + cropHeight);
        }
        hv.setup(imageView.getUnrotatedMatrix(), imageRect, cropRect, !aspectRatio.isNaN());
        return hv;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rotateBitmap != null) {
            rotateBitmap.recycle();
        }
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    private class AreaPicker {


        public void start() {
            handler.post(new Runnable() {
                public void run() {
                    imageView.highlightViews.clear();
                    imageView.add(pickerView);
                    imageView.invalidate();
                    if (imageView.highlightViews.size() == 1) {
                        pickerView = imageView.highlightViews.get(0);
                        pickerView.setFocus(true);
                    }
                }
            });
        }
    }
}
