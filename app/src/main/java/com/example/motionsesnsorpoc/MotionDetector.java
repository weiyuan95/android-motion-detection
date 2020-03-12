package com.example.motionsesnsorpoc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;

class MotionDetector {

    private final String TAG = "Image Processing";
    private double previousValue = Double.POSITIVE_INFINITY;
    private int[] currentPixelArray;
    private Bitmap bm;
    private int bmWidth;
    private int bmHeight;
    private static final int MIDDLE_PIXEL_WIDTH = 1;

    boolean motionDetected(byte[] bytes) {

        /*
        Processes an image into an integer array, and calculates the average value.
        If there is a significant difference between the old and new value,
        there is some motion happening
        */

        process(bytes);

        if (previousValue == Double.POSITIVE_INFINITY) {
            previousValue = calculatePixelAverage();
            return false;
        }

        double newValue = calculatePixelAverage();
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        double pctDifference = Math.abs((newValue - previousValue) / previousValue);
        Timestamp ts = new Timestamp(new Date().getTime());
        Log.i(TAG, ts + ">>> % difference: " + decimalFormat.format(pctDifference));

        previousValue = newValue;

        return pctDifference > 0.1;
    }

    private void process(byte[] bytes) {

        bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        bmWidth = bm.getWidth();
        bmHeight = bm.getHeight();

        // override the original bitmap with the correctly rotated bitmap
        bm = getRotatedBitmap();

        // holds the reference to the pixel array of the current frame
        currentPixelArray = getImageMiddlePixels();

        // to get pixels of entire image
//        currentPixelArray = getBitmapPixels();
    }


    private int[] getBitmapPixels() {

        /*
        deprecated, since we are only interested in motion in
        the middle instead of the entire picture
         */

        Log.i(TAG, "Number of pixels: " + bmWidth * bmHeight);

        // still need a full array of width * height
        // values that we do not want will be set to 0
        int[] pixels = new int[bmWidth * bmHeight];
        bm.getPixels(pixels, 0, bmWidth, 0, 0, bmWidth, bmHeight);

        return pixels;
    }

    private double calculatePixelAverage() {
        double total = 0;

        for (int val : currentPixelArray) {
            total += val;
        }

        return total / currentPixelArray.length;
    }

    private int[] getImageMiddlePixels() {
        /*
            the pixels we need is the middle "chunk" of the image
            so we just take the full image height * the width of the middle chunk
            that will be the number of needed pixels
         */
        int numberOfPixels = MIDDLE_PIXEL_WIDTH * bmHeight;
        int[] middlePixels = new int[numberOfPixels];
        int arrayStart = 0;
        int chunkColStartCoordinate = getMiddleChunkStartCoordinate();

        for (int colNum=0; colNum < MIDDLE_PIXEL_WIDTH; colNum++) {
            int colCoordinate = chunkColStartCoordinate + colNum;

            for (int rowCoordinate=0; rowCoordinate < bmHeight; rowCoordinate++ ) {
                middlePixels[arrayStart] = bm.getPixel(colCoordinate, rowCoordinate);
            }

            arrayStart++;
        }


        return middlePixels;
    }

    private int getMiddleChunkStartCoordinate() {
        int midPointCoordinate = Math.floorDiv(bmWidth, 2);

        if (MIDDLE_PIXEL_WIDTH == 1) {
            return midPointCoordinate;
        }

        return midPointCoordinate - Math.floorDiv(MIDDLE_PIXEL_WIDTH, 2);
    }

    private Bitmap getRotatedBitmap() {
        Matrix mat = new Matrix();

        // hardcoded - the sensor rotation seems to be 270
        mat.setRotate(270);

        return Bitmap.createBitmap(bm, 0, 0, bmWidth, bmHeight, mat, true);
    }

}
