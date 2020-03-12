package com.example.motionsesnsorpoc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;

import java.nio.ByteBuffer;

class ImageProcessor {

    byte[] processImage(Image image) {
        // converts an image to a byte array

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);

        return bytes;
    }
}
