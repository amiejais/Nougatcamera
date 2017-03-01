package com.amiejais.nougatcamera.callbacks;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.os.Environment;

import com.amiejais.nougatcamera.camerautils.ImageSaverBuilder;
import com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment;

import java.io.File;

import static com.amiejais.nougatcamera.utils.CommonUtils.showToast;


/**
 * Created by KNPX0678 on 16-Feb-17.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraCaptureCallback extends CameraCaptureSession.CaptureCallback {

    private NougatCameraFragment mNougatCameraFragment;

    public CameraCaptureCallback(NougatCameraFragment nougatCameraFragment) {
        mNougatCameraFragment = nougatCameraFragment;
    }

    @Override
    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                 long timestamp, long frameNumber) {
        String currentDateTime = mNougatCameraFragment.generateTimestamp();
        File jpegFile = new File(Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "JPEG_" + currentDateTime + ".jpg");

        // Look up the ImageSaverBuilder for this request and update it with the file name
        // based on the capture start time.
        ImageSaverBuilder jpegBuilder;
        if (null == request || null == request.getTag()) {
            return;
        }
        int requestId = (int) request.getTag();
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            jpegBuilder = mNougatCameraFragment.mJpegResultQueue.get(requestId);
        }

        if (jpegBuilder != null) jpegBuilder.setFile(jpegFile);
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                   TotalCaptureResult result) {

        int requestId = (int) request.getTag();
        ImageSaverBuilder jpegBuilder;
        StringBuilder sb = new StringBuilder();

        // Look up the ImageSaverBuilder for this request and update it with the CaptureResult
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            jpegBuilder = mNougatCameraFragment.mJpegResultQueue.get(requestId);

            // If we have all the results necessary, save the image to a file in the background.
            mNougatCameraFragment.handleCompletionLocked(requestId, jpegBuilder, mNougatCameraFragment.mJpegResultQueue);
            mNougatCameraFragment.finishedCaptureLocked();
        }

        showToast(sb.toString());
    }

    @Override
    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                CaptureFailure failure) {
        int requestId = (int) request.getTag();
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            mNougatCameraFragment.mJpegResultQueue.remove(requestId);
            mNougatCameraFragment.finishedCaptureLocked();
        }
        showToast("Capture failed!");
    }
}
