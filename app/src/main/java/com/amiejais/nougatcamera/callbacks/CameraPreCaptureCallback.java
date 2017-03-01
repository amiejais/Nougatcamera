package com.amiejais.nougatcamera.callbacks;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment;

import static com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment.STATE_PREVIEW;
import static com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment.STATE_WAITING_FOR_3A_CONVERGENCE;

/**
 * Created by KNPX0678 on 16-Feb-17.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraPreCaptureCallback extends CameraCaptureSession.CaptureCallback {

    private static final String TAG = "CameraPreCaptureCallback";
    private NougatCameraFragment mNougatCameraFragment;

    public CameraPreCaptureCallback(NougatCameraFragment nougatCameraFragment) {
        mNougatCameraFragment = nougatCameraFragment;
    }

    @Override
    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureResult partialResult) {
        process(partialResult);
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                   TotalCaptureResult result) {
        process(result);
    }

    private void process(CaptureResult result) {
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            switch (mNougatCameraFragment.mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is running normally.
                    break;
                }
                case STATE_WAITING_FOR_3A_CONVERGENCE: {
                    boolean readyToCapture = true;
                    if (!mNougatCameraFragment.mNoAFRun) {
                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        if (afState == null) {
                            break;
                        }

                        // If auto-focus has reached locked state, we are ready to capture
                        readyToCapture =
                                (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                        afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
                    }

                    // If we are running on an non-legacy device, we should also wait until
                    // auto-exposure and auto-white-balance have converged as well before
                    // taking a picture.
                    if (!mNougatCameraFragment.isLegacyLocked()) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
                        if (aeState == null || awbState == null) {
                            break;
                        }

                        readyToCapture = readyToCapture &&
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED &&
                                awbState == CaptureResult.CONTROL_AWB_STATE_CONVERGED;
                    }

                    // If we haven't finished the pre-capture sequence but have hit our maximum
                    // wait timeout, too bad! Begin capture anyway.
                    if (!readyToCapture && mNougatCameraFragment.hitTimeoutLocked()) {
                        readyToCapture = true;
                    }

                    if (readyToCapture && mNougatCameraFragment.mPendingUserCaptures > 0) {
                        // Capture once for each user tap of the "Picture" button.
                        while (mNougatCameraFragment.mPendingUserCaptures > 0) {
                            mNougatCameraFragment.captureStillPictureLocked();
                            mNougatCameraFragment.mPendingUserCaptures--;
                        }
                        // After this, the camera will go back to the normal state of preview.
                        mNougatCameraFragment.mState = STATE_PREVIEW;
                    }
                }
            }
        }
    }
}
