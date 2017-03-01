/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amiejais.nougatcamera.ui.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.amiejais.nougatcamera.R;
import com.amiejais.nougatcamera.app.CameraApp;
import com.amiejais.nougatcamera.app.ConstantValues;
import com.amiejais.nougatcamera.callbacks.CameraCaptureCallback;
import com.amiejais.nougatcamera.callbacks.CameraPreCaptureCallback;
import com.amiejais.nougatcamera.callbacks.CameraStateCallback;
import com.amiejais.nougatcamera.camerautils.AutoFitTextureView;
import com.amiejais.nougatcamera.camerautils.CameraHelperUtils;
import com.amiejais.nougatcamera.camerautils.CompareSizesByArea;
import com.amiejais.nougatcamera.camerautils.ImageSaver;
import com.amiejais.nougatcamera.camerautils.ImageSaverBuilder;
import com.amiejais.nougatcamera.camerautils.RefCountedAutoCloseable;
import com.amiejais.nougatcamera.listeners.NCImageAvailableListener;
import com.amiejais.nougatcamera.listeners.NCSurfaceTextureListener;
import com.amiejais.nougatcamera.utils.CameraAnnotation;
import com.amiejais.nougatcamera.utils.ErrorDialog;
import com.amiejais.nougatcamera.utils.PermissionChecker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.amiejais.nougatcamera.utils.CameraAnnotation.CAMERA_POSITION_BACK;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.CAMERA_POSITION_FRONT;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.CAMERA_POSITION_UNKNOWN;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.EXPOSURE;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_ALWAYS_ON;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_AUTO;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_OFF;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_TORCH;
import static com.amiejais.nougatcamera.utils.CommonUtils.showToast;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NougatCameraFragment extends NougatCameraBaseFragment
        implements View.OnClickListener {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    /**
     * Timeout for the pre-capture sequence.
     */
    private static final long PRECAPTURE_TIMEOUT_MS = 1000;

    /**
     * Tolerance when comparing aspect ratios.
     */
    private static final double ASPECT_RATIO_TOLERANCE = 0.005;


    /**
     * Camera state: Device is closed.
     */
    public static final int STATE_CLOSED = 0;

    /**
     * Camera state: Device is opened, but is not capturing.
     */
    public static final int STATE_OPENED = 1;

    /**
     * Camera state: Showing camera preview.
     */
    public static final int STATE_PREVIEW = 2;

    /**
     * Camera state: Waiting for 3A convergence before capturing a photo.
     */
    public static final int STATE_WAITING_FOR_3A_CONVERGENCE = 3;

    /**
     * An {@link OrientationEventListener} used to determine when device rotation has occurred.
     * This is mainly necessary for when the device is rotated by 180 degrees, in which case
     * onCreate or onConfigurationChanged is not called as the view dimensions remain the same,
     * but the orientation of the has changed, and thus the preview rotation must be updated.
     */
    private OrientationEventListener mOrientationListener;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    public AutoFitTextureView mTextureView;

    /**
     * An additional thread for running tasks that shouldn't block the UI.  This is used for all
     * callbacks from the {@link CameraDevice} and {@link CameraCaptureSession}s.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A counter for tracking corresponding {@link CaptureRequest}s and {@link CaptureResult}s
     * across the {@link CameraCaptureSession} capture callbacks.
     */
    private final AtomicInteger mRequestCounter = new AtomicInteger();

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    public final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * A lock protecting camera state.
     */
    public final Object mCameraStateLock = new Object();


    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the open {@link CameraDevice}.
     */
    public CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    public Size mPreviewSize;

    /**
     * The {@link CameraCharacteristics} for the currently configured camera device.
     */
    private CameraCharacteristics mCharacteristics;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A reference counted holder wrapping the {@link ImageReader} that handles JPEG image
     * captures. This is used to allow us to clean up the {@link ImageReader} when all background
     * tasks using its {@link Image}s have completed.
     */
    public RefCountedAutoCloseable<ImageReader> mJpegImageReader;

    /**
     * Whether or not the currently configured camera device is fixed-focus.
     */
    public boolean mNoAFRun = false;

    /**
     * Number of pending user requests to capture a photo.
     */
    public int mPendingUserCaptures = 0;

    /**
     * Request ID to {@link ImageSaverBuilder} mapping for in-progress JPEG captures.
     */
    public final TreeMap<Integer, ImageSaverBuilder> mJpegResultQueue = new TreeMap<>();

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CaptureRequest mPreviewRequest;

    /**
     * The state of the camera device.
     *
     * @see #mPreCaptureCallback
     */
    public int mState = STATE_CLOSED;

    /**
     * Timer to use with pre-capture sequence to ensure a timely capture if 3A convergence is
     * taking too long.
     */
    private long mCaptureTimer;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events of a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new NCSurfaceTextureListener(this);

    /**
     * {@link CameraDevice.StateCallback} is called when the currently active {@link CameraDevice}
     * changes its state.`
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraStateCallback(this);

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events for the preview and
     * pre-capture sequence.
     */
    private CameraCaptureSession.CaptureCallback mPreCaptureCallback = new CameraPreCaptureCallback(this);

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles the still JPEG and RAW capture
     * request.
     */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureCallback(this);


    public static NougatCameraFragment newInstance() {
        return new NougatCameraFragment();
    }


    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);

        mOrientationListener = new OrientationEventListener(getActivity(),
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (mTextureView != null && mTextureView.isAvailable()) {
                    configureTransform(mTextureView.getWidth(), mTextureView.getHeight(), mPreviewSize, mTextureView);
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView != null) {
            if (mTextureView.isAvailable()) {
                openCamera();
            } else {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we should
        // configure the preview bounds here (otherwise, we wait until the surface is ready in
        // the NCSurfaceTextureListener).

        if (mOrientationListener != null && mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }

    @Override
    public void onPause() {
        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    /**
     * Sets up state related to camera that is needed before opening a {@link CameraDevice}.
     */
    private void setupCameraPreviewSize(CameraCharacteristics characteristics) {

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            ErrorDialog.buildErrorDialog("This device doesn't support Camera2 API.").
                    show(getActivity().getFragmentManager(), "dialog");
            return;
        }
        try {
            int viewWidth = mTextureView.getWidth();
            int viewHeight = mTextureView.getHeight();

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

            int totalRotation = sensorToDeviceRotation(characteristics, deviceRotation);

            boolean swappedDimensions = totalRotation == 90 || totalRotation == 270;

            int rotatedViewWidth = viewWidth;
            int rotatedViewHeight = viewHeight;

            if (swappedDimensions) {
                rotatedViewWidth = viewHeight;
                rotatedViewHeight = viewWidth;
            }

            // Find the best preview size for these view dimensions and configured JPEG size.
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedViewWidth, rotatedViewHeight);
            if (swappedDimensions) {
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            } else {
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }

            if (mJpegImageReader == null || mJpegImageReader.getAndRetain() == null) {
                Size mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedViewWidth, rotatedViewHeight);
                mJpegImageReader = new RefCountedAutoCloseable<>(ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, /*maxImages*/5));
                mJpegImageReader.get().setOnImageAvailableListener(new NCImageAvailableListener(this), mBackgroundHandler);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Opens the camera specified by {@link }.
     */
    @Override
    public void openCamera() {

        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            // Wait for any previously running session to finish.
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            synchronized (mCameraStateLock) {
                if (null != mTextureView && mTextureView.isAvailable()) {

                    configureCameraPosition(manager);
                    resetCameraPosition();
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics((String) CameraApp.getInstance().getCameraControls().getCurrentCamId());
                    mCharacteristics = characteristics;
                    setupCameraPreviewSize(characteristics);
                    configureTransform(mTextureView.getWidth(), mTextureView.getHeight(), mPreviewSize, mTextureView);
                    CameraApp.getInstance().getCameraControls().setFlashModes(CameraHelperUtils.getSupportedFlashModes(characteristics));
                    onFlashModesLoaded();
                }
            }
            if (PermissionChecker.getInstance().isCameraPermissionGranted(getActivity())) {
                manager.openCamera((String) CameraApp.getInstance().getCameraControls().getCurrentCamId(), mStateCallback, null);
            }

        } catch (Exception e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }

    }

    private void onFlashModesLoaded() {
        toggleFlashMode(false);
    }

    private void resetCameraPosition() {
        switch (CameraApp.getInstance().getCameraControls().getCurrentCamPos()) {
            case CAMERA_POSITION_FRONT:
                setImageRes(mCameraSwitchRearView, ConstantValues.ICON_REAR_CAMERA);
                break;
            case CAMERA_POSITION_BACK:
                setImageRes(mCameraSwitchFrontView, ConstantValues.ICON_FRONT_CAMERA);
                break;
            case CAMERA_POSITION_UNKNOWN:
            default:
                setCameraPosition();
                break;
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    @Override
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            synchronized (mCameraStateLock) {

                // Reset state and clean up resources used by the camera.
                // Note: After calling this, the ImageReaders will be closed after any background
                // tasks saving Images from these readers have been completed.
                mPendingUserCaptures = 0;
                mState = STATE_CLOSED;
                if (null != mCaptureSession) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                if (null != mJpegImageReader) {
                    mJpegImageReader.close();
                    mJpegImageReader = null;
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        synchronized (mCameraStateLock) {
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            synchronized (mCameraStateLock) {
                mBackgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    public void startPreview() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface,
                    mJpegImageReader.get().getSurface()
                    ), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            synchronized (mCameraStateLock) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }

                                try {
                                    setup3AControlsLocked(mPreviewRequestBuilder);
                                    // Finally, we start displaying the camera preview.
                                    cameraCaptureSession.setRepeatingRequest(
                                            mPreviewRequestBuilder.build(),
                                            mPreCaptureCallback, mBackgroundHandler);
                                    mState = STATE_PREVIEW;
                                } catch (CameraAccessException | IllegalStateException e) {
                                    e.printStackTrace();
                                    return;
                                }
                                // When the session is ready, we start displaying the preview.
                                mCaptureSession = cameraCaptureSession;
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed to configure camera.");
                        }
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configure the given {@link CaptureRequest.Builder} to use auto-focus, auto-exposure, and
     * auto-white-balance controls if available.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @param builder the builder to configure.
     */
    private void setup3AControlsLocked(CaptureRequest.Builder builder) {
        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO);

        Float minFocusDist =
                mCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        mNoAFRun = (minFocusDist == null || minFocusDist == 0);

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO);
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
        }

        // If there is an auto-magical white balance control mode available, use it.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
        setFlashMode(builder);
    }

    /**
     * Configure the necessary {@link Matrix} transformation to `mTextureView`,
     * and start/restart the preview capture session if necessary.
     * <p/>
     * This method should be called after the camera state has been initialized in
     * setupCameraPreviewSize.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    public void configureTransform(int viewWidth, int viewHeight, Size pPreviewSize, AutoFitTextureView pAutoFitTextureView) {
        Activity activity = getActivity();
        synchronized (mCameraStateLock) {
            if (null == pAutoFitTextureView || null == activity || null == mCharacteristics) {
                return;
            }
            // Find the rotation of the device relative to the native device orientation.
            int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();


            // Find rotation of device in degrees (reverse device orientation for front-facing
            // cameras).
            int rotation = (mCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT) ?
                    (360 + ORIENTATIONS.get(deviceRotation)) % 360 :
                    (360 - ORIENTATIONS.get(deviceRotation)) % 360;

            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, pPreviewSize.getHeight(), pPreviewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / pPreviewSize.getHeight(),
                        (float) viewWidth / pPreviewSize.getWidth());
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            } else if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(180, centerX, centerY);
            }
            pAutoFitTextureView.setTransform(matrix);

            // Start or restart the active capture session if the preview was initialized or
            // if its aspect ratio changed significantly.
            if (mPreviewSize == null || !checkAspectsEqual(mPreviewSize, pPreviewSize)) {
                mPreviewSize = pPreviewSize;
                if (mState != STATE_CLOSED) {
                    startPreview();
                }
            }
        }
    }

    /**
     * Initiate a still image capture.
     * <p/>
     * This function sends a capture request that initiates a pre-capture sequence in our state
     * machine that waits for auto-focus to finish, ending in a "locked" state where the lens is no
     * longer moving, waits for auto-exposure to choose a good exposure value, and waits for
     * auto-white-balance to converge.
     */
    @Override
    public void takeStillShot() {
        synchronized (mCameraStateLock) {
            mPendingUserCaptures++;

            // If we already triggered a pre-capture sequence, or are in a state where we cannot
            // do this, return immediately.
            if (mState != STATE_PREVIEW) {
                return;
            }

            try {
                // Trigger an auto-focus run if camera is capable. If the camera is already focused,
                // this should do nothing.
                if (!mNoAFRun) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                            CameraMetadata.CONTROL_AF_TRIGGER_START);
                }

                // If this is not a legacy device, we can also trigger an auto-exposure metering
                // run.
                if (!isLegacyLocked()) {
                    // Tell the camera to lock focus.
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                }

                // Update state machine to wait for auto-focus, auto-exposure, and
                // auto-white-balance (aka. "3A") to converge.
                mState = STATE_WAITING_FOR_3A_CONVERGENCE;

                // Start a timer for the pre-capture sequence.
                startTimerLocked();
                setFlashMode(mPreviewRequestBuilder);
                // Replace the existing repeating request with one with updated 3A triggers.
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mPreCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCameraSettingChange(int preference) {
        switch (CameraAnnotation.getInstance().getCurrentMode()) {
            case FLASH:
                setFlashMode(mPreviewRequestBuilder);
                break;
            case EXPOSURE:
                break;
        }
        updateCameraPreviewSession();
    }

    /**
     * Method to update camera preview session.
     */
    private void updateCameraPreviewSession() {
        try {
            mPreviewRequest = mPreviewRequestBuilder.build();
            //  mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        } catch (Exception e) {

        }
    }

    /**
     * Send a capture request to the camera device that initiates a capture targeting the JPEG and
     * RAW outputs.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    public void captureStillPictureLocked() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(mJpegImageReader.get().getSurface());

            // Use the same AE and AF modes as the preview.
            setup3AControlsLocked(captureBuilder);

            // Set orientation.
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    sensorToDeviceRotation(mCharacteristics, rotation));

            // Set request tag to easily track results in callbacks.
            captureBuilder.setTag(mRequestCounter.getAndIncrement());

            CaptureRequest request = captureBuilder.build();

            // Create an ImageSaverBuilder in which to collect results, and add it to the queue
            // of active requests.
            ImageSaverBuilder jpegBuilder = new ImageSaverBuilder();

            mJpegResultQueue.put((int) request.getTag(), jpegBuilder);

            mCaptureSession.capture(request, mCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setFlashMode(CaptureRequest.Builder requestBuilder) {
        int aeMode;
        int flashMode;
        switch (CameraApp.getInstance().getCameraControls().getFlashMode()) {
            case FLASH_MODE_AUTO:
                aeMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
                flashMode = CameraMetadata.FLASH_MODE_SINGLE;
                break;
            case FLASH_MODE_ALWAYS_ON:
                aeMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
                flashMode = CameraMetadata.FLASH_MODE_TORCH;
                break;
            case FLASH_MODE_TORCH:
                aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
                flashMode = CameraMetadata.FLASH_MODE_TORCH;
                break;
            case FLASH_MODE_OFF:
            default:
                aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
                flashMode = CameraMetadata.FLASH_MODE_OFF;
                break;
        }
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, aeMode);
        requestBuilder.set(CaptureRequest.FLASH_MODE, flashMode);
    }

    /**
     * Called after a RAW/JPEG capture has completed; resets the AF trigger state for the
     * pre-capture sequence.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    public void finishedCaptureLocked() {
        try {
            // Reset the auto-focus trigger in case AF didn't run quickly enough.
            if (!mNoAFRun) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                setFlashMode(mPreviewRequestBuilder);
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mPreCaptureCallback,
                        mBackgroundHandler);

                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices   The list of sizes that the camera supports for the intended output
     *                  class
     * @param maxWidth  The width of the texture view relative to sensor coordinate
     * @param maxHeight The height of the texture view relative to sensor coordinate
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int maxWidth, int maxHeight) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * maxHeight / maxWidth &&
                    option.getWidth() >= maxWidth && option.getHeight() >= maxHeight) {
                bigEnough.add(option);
            }
        }
        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    /**
     * Generate a string containing a formatted timestamp with the current date and time.
     *
     * @return a {@link String} representing a time.
     */
    public static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return sdf.format(new Date());
    }


    /**
     * Return true if the given array contains the given integer.
     *
     * @param modes array to check.
     * @param mode  integer to get for.
     * @return true if the array contains the given integer, otherwise false.
     */
    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if the two given {@link Size}s have the same aspect ratio.
     *
     * @param a first {@link Size} to compare.
     * @param b second {@link Size} to compare.
     * @return true if the sizes have the same aspect ratio, otherwise false.
     */
    private static boolean checkAspectsEqual(Size a, Size b) {
        double aAspect = a.getWidth() / (double) a.getHeight();
        double bAspect = b.getWidth() / (double) b.getHeight();
        return Math.abs(aAspect - bAspect) <= ASPECT_RATIO_TOLERANCE;
    }

    /**
     * Rotation need to transform from the camera sensor orientation to the device's current
     * orientation.
     *
     * @param c                 the {@link CameraCharacteristics} to query for the camera sensor
     *                          orientation.
     * @param deviceOrientation the current device orientation relative to the native device
     *                          orientation.
     * @return the total rotation from the sensor orientation to the current device orientation.
     */
    private static int sensorToDeviceRotation(CameraCharacteristics c, int deviceOrientation) {
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Get device orientation in degrees
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);

        // Reverse device orientation for front-facing cameras
        if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    /**
     * If the given request has been completed, remove it from the queue of active requests and
     * send an {@link ImageSaver} with the results from this request to a background thread to
     * save a file.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @param requestId the ID of the {@link CaptureRequest} to handle.
     * @param builder   the {@link ImageSaverBuilder} for this request.
     * @param queue     the queue to remove this request from, if completed.
     */
    public void handleCompletionLocked(int requestId, ImageSaverBuilder builder,
                                       TreeMap<Integer, ImageSaverBuilder> queue) {
        if (builder == null) return;
        ImageSaver saver = builder.buildIfComplete(this);
        if (saver != null) {
            queue.remove(requestId);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(saver);
        }
    }

    /**
     * Check if we are using a device that only supports the LEGACY hardware level.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @return true if this is a legacy device.
     */
    public boolean isLegacyLocked() {
        return mCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    /**
     * Start the timer for the pre-capture sequence.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void startTimerLocked() {
        mCaptureTimer = SystemClock.elapsedRealtime();
    }

    /**
     * Check if the timer for the pre-capture sequence has been hit.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @return true if the timeout occurred.
     */
    public boolean hitTimeoutLocked() {
        return (SystemClock.elapsedRealtime() - mCaptureTimer) > PRECAPTURE_TIMEOUT_MS;
    }


    /**
     * Method to set camera position.
     */
    private void setCameraPosition() {
        if (CameraApp.getInstance().getCameraControls().getFrontCam() != null) {
            setImageRes(mCameraSwitchFrontView, ConstantValues.ICON_REAR_CAMERA);
            CameraApp.getInstance().getCameraControls().setCurrentCamPos(CAMERA_POSITION_FRONT);
        } else {
            setImageRes(mCameraSwitchRearView, ConstantValues.ICON_FRONT_CAMERA);
            if (CameraApp.getInstance().getCameraControls().getBackCam() != null)
                CameraApp.getInstance().getCameraControls().setCurrentCamPos(CAMERA_POSITION_BACK);
            else
                CameraApp.getInstance().getCameraControls().setCurrentCamPos(CAMERA_POSITION_UNKNOWN);
        }
    }

    /**
     * Method to configure camera position
     *
     * @param manager camera manager
     * @throws CameraAccessException exception
     */
    @SuppressWarnings("ConstantConditions")
    private void configureCameraPosition(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            if (cameraId == null)
                continue;
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing == CameraCharacteristics.LENS_FACING_FRONT)
                CameraApp.getInstance().getCameraControls().setFrontCam(cameraId);
            else if (facing == CameraCharacteristics.LENS_FACING_BACK)
                CameraApp.getInstance().getCameraControls().setBackCam(cameraId);
        }
    }
}
