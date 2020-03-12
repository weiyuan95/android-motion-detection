package com.example.motionsesnsorpoc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MotionDetectionActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private CameraDevice cameraDevice;
    private String cameraId;
    private Size imageDimension;
    private TextureView textureView;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSessions;
    private CameraCharacteristics cameraCharacteristics;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Handler imReaderBackgroundHandler;
    private int imageWidth;
    private int imageHeight;
    private Surface imageReaderSurface;
    private MotionDetector motionDetector;
    private ImageReader imageReader;
    private ExecutorService threadPool;

    private final CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            //The camera is already closed
            if (null == cameraDevice) {
                return;
            }
            // When the session is ready, we start displaying the preview.
            Log.i(TAG, "Session is ready");
            cameraCaptureSessions = cameraCaptureSession;
            motionDetector = new MotionDetector();
            updatePreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //This is called when the camera is open
            cameraDevice = camera;
            Toast.makeText(getApplicationContext(), "Camera opened", Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBackgroundThread();
        /*
        need to find the right thread pool to use
        newSingleThreadExecutor
            -> causes memory issues. So many images are being sent that there's not enough RAM
        newCachedThreadPool
            -> works for a while, then just dies. No idea why, but maybe because too many threads
            are being spawned
        newFixedThreadPool
            -> same as the cached thread pool

            lowering the pixel density to 2880x2160 (index 7) didnt work as well
            1440x1080 (index 11)
            trying lower densities...
        */
        threadPool = Executors.newFixedThreadPool(5);
        textureView = findViewById(R.id.preview);
        textureView.setSurfaceTextureListener(textureListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        stopBackgroundThread();
        super.onPause();
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        if (manager == null) {
            Log.e(TAG, "Couldn't get the camera manager");
            return;
        }

        Log.i(TAG, "trying to open camera...");

        try {
            cameraId = manager.getCameraIdList()[0];
            cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
            }

            // maybe need to change ImageFormat to something else?
            // save the dimensions of the image preview to a instance variable
            Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            Log.i(TAG, Arrays.toString(jpegSizes));
//            2880x2160 (index 7)
//            1440x1080 (index 12)
            imageWidth = jpegSizes[12].getWidth();
            imageHeight = jpegSizes[12].getHeight();

            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                String[] requiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(MainActivity.this, requiredPermissions , REQUEST_CAMERA_PERMISSION);
            }

            manager.openCamera(cameraId, stateCallback, null);

        } catch (CameraAccessException e) {
            Log.e(TAG, "something went wrong");
            e.printStackTrace();
        }

    }

    private void createCameraPreview() {
        Log.i(TAG, "creating camera preview...");
        setupImageReaderListener();

        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            // Create a request suitable for a camera preview window.
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // Add a surface to the list of targets for this request
            captureRequestBuilder.addTarget(surface);
            // setup image reader listener before adding the image reader surface as a target
            captureRequestBuilder.addTarget(imageReaderSurface);

            ArrayList<OutputConfiguration> outputConfigs = new ArrayList<>();
            outputConfigs.add(new OutputConfiguration(surface));
            outputConfigs.add(new OutputConfiguration(imageReaderSurface));

            // create the session config
            SessionConfiguration config = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                    outputConfigs,
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    captureSessionStateCallback);

            cameraDevice.createCaptureSession(config);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        // this should be 90, maybe we can feed this to motion detector instead of hardcoding the necessary
        // 270 degrees rotation
        Log.i(TAG, "orientation>>>> " + cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));

        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation());
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupImageReaderListener() {

        // maxImages to 2 for real time processing
        imageReader = ImageReader.newInstance(imageWidth, imageHeight, ImageFormat.JPEG, 1);
        imageReaderSurface = imageReader.getSurface();

        ImageReader.OnImageAvailableListener imageReaderListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();

                if (image != null) {
                    byte[] bytes = new ImageProcessor().processImage(image);
                    image.close();

                    // threading works now, but non threaded works just as well
//                    threadPool.execute(new MotionDetectionThread(bytes));

                    if (motionDetector.motionDetected(bytes)) {
                        Log.e(TAG, "motion detected!!");
                    }

                }

            }
        };

        imageReader.setOnImageAvailableListener(imageReaderListener, imReaderBackgroundHandler);
    }

    private int getJpegOrientation() {
        int deviceOrientation = this.getWindowManager().getDefaultDisplay().getRotation();

        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0;
        }

        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;

        if (facingFront) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        HandlerThread imReaderBackgroundThread = new HandlerThread("Image Reader handler");
        imReaderBackgroundThread.start();
        imReaderBackgroundHandler = new Handler(imReaderBackgroundThread.getLooper());

    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
