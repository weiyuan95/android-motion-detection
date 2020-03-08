package com.example.motionsesnsorpoc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;

class MotionDetector {

    private final String TAG = "Image Processing";
    private double previousValue = Double.POSITIVE_INFINITY;
    private int[] currentPixelArray;

    boolean motionDetected(Image image) {

        process(image);

        if (previousValue == Double.POSITIVE_INFINITY) {
            previousValue = calculatePixelDifference();
            return false;
        }

        double newValue = calculatePixelDifference();
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        double pctDifference = (newValue - previousValue) / previousValue;
        Log.i(TAG, "% difference: " + decimalFormat.format(pctDifference));

        // override the old value
        previousValue = newValue;

        // 0.1 seems like a good value when there is motion on a PERFECTLY still camera
        return pctDifference > 0.02;
    }

    private void process(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        image.close();

        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        currentPixelArray = getBitmapPixels(bm);
    }

    private int[] getBitmapPixels(Bitmap bm) {
        int bmWidth = bm.getWidth();
        int bmHeight = bm.getHeight();

        int[] pixels = new int[bmWidth * bmHeight];
        bm.getPixels(pixels, 0, bmWidth, 0, 0, bmWidth, bmHeight);

        return pixels;
    }

    private double calculatePixelDifference() {
        double total = 0;

        for (int val : currentPixelArray) {
            total += val;
        }

        return total / currentPixelArray.length;
    }



}
