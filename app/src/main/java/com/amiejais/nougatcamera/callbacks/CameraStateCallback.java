package com.amiejais.nougatcamera.callbacks;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.util.Log;

import com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment;

import static com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment.STATE_CLOSED;
import static com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment.STATE_OPENED;

/**
 * Created by KNPX0678 on 16-Feb-17.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraStateCallback extends CameraDevice.StateCallback {

    private static final String TAG = "CameraStateCallback";
    NougatCameraFragment mNougatCameraFragment;

    public CameraStateCallback(NougatCameraFragment nougatCameraFragment) {
        mNougatCameraFragment = nougatCameraFragment;
    }

    /**
     * The method called when a camera device has finished opening.
     * <p>
     * <p>At this point, the camera device is ready to use, and
     * {@link CameraDevice#createCaptureSession} can be called to set up the first capture
     * session.</p>
     *
     * @param camera the camera device that has become opened
     */
    @Override
    public void onOpened(CameraDevice camera) {
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            mNougatCameraFragment.mState = STATE_OPENED;
            mNougatCameraFragment.mCameraOpenCloseLock.release();
            mNougatCameraFragment.mCameraDevice  = camera;

            // Start the preview session if the TextureView has been set up already.
            if (mNougatCameraFragment.mPreviewSize != null && mNougatCameraFragment.mTextureView.isAvailable()) {
                mNougatCameraFragment.startPreview();
            }
        }
    }

    /**
     * The method called when a camera device is no longer available for
     * use.
     * <p>
     * <p>This callback may be called instead of {@link #onOpened}
     * if opening the camera fails.</p>
     * <p>
     * <p>Any attempt to call methods on this CameraDevice will throw a
     * change in security policy or permissions; the physical disconnection
     * of a removable camera device; or the camera being needed for a
     * higher-priority camera API client.</p>
     * <p>
     * <p>There may still be capture callbacks that are invoked
     * after this method is called, or new image buffers that are delivered
     * to active outputs.</p>
     * <p>
     * <p>The default implementation logs a notice to the system log
     * about the disconnection.</p>
     * <p>
     * <p>You should clean up the camera with {@link CameraDevice#close} after
     * this happens, as it is not recoverable until the camera can be opened
     * again. For most use cases, this will be when the camera again becomes
     * </p>
     *
     * @param camera the device that has been disconnected
     */
    @Override
    public void onDisconnected(CameraDevice camera) {
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            mNougatCameraFragment.mState = STATE_CLOSED;
            mNougatCameraFragment.mCameraOpenCloseLock.release();
            mNougatCameraFragment.mCameraDevice = null;
            camera.close();
        }
    }

    /**
     * The method called when a camera device has encountered a serious error.
     * <p>
     * <p>This callback may be called instead of {@link #onOpened}
     * if opening the camera fails.</p>
     * <p>
     * <p>This indicates a failure of the camera device or camera service in
     * some way. Any attempt to call methods on this CameraDevice in the
     * </p>
     * <p>
     * <p>There may still be capture completion or camera stream callbacks
     * that will be called after this error is received.</p>
     * <p>
     * <p>You should clean up the camera with {@link CameraDevice#close} after
     * this happens. Further attempts at recovery are error-code specific.</p>
     *
     * @param camera The device reporting the error
     * @param error  The error code, one of the
     *               {@code StateCallback.ERROR_*} values.
     * @see #ERROR_CAMERA_IN_USE
     * @see #ERROR_MAX_CAMERAS_IN_USE
     * @see #ERROR_CAMERA_DISABLED
     * @see #ERROR_CAMERA_DEVICE
     * @see #ERROR_CAMERA_SERVICE
     */
    @Override
    public void onError(CameraDevice camera, int error) {
        Log.e(TAG, "Received camera device error: " + error);
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            mNougatCameraFragment.mState = STATE_CLOSED;
            mNougatCameraFragment.mCameraOpenCloseLock.release();
            camera.close();
            mNougatCameraFragment.mCameraDevice = null;
        }

    }
}
