package com.example.motionsesnsorpoc;

import android.os.Process;
import android.util.Log;

import com.example.motionsesnsorpoc.MotionDetector;

public class MotionDetectionThread implements Runnable {

    private final String TAG = "Detection Thread";
    private byte[] bytes;

    public MotionDetectionThread(byte[] bytes) {this.bytes = bytes;}

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        MotionDetector md = new MotionDetector();

        if (md.motionDetected(bytes)) {
            Log.e(TAG, "motion detected!!");
        }
    }
}
