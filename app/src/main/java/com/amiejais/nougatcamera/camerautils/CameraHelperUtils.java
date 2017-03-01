package com.amiejais.nougatcamera.camerautils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.view.Surface;

import com.amiejais.nougatcamera.app.CameraApp;

import java.util.ArrayList;
import java.util.List;

import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_ALWAYS_ON;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_AUTO;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_OFF;

/**
 * Created by KNPX0678 on 16-Feb-17.
 */

public class CameraHelperUtils {


    /**
     * Configure the necessary {@link Matrix} transformation to `mTextureView`,
     * and start/restart the preview capture session if necessary.
     * <p/>
     * This method should be called after the camera state has been initialized in
     * setUpCameraOutputs.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void configureTransform(Activity activity, AutoFitTextureView textureView, android.util.Size previewSize, int viewWidth, int viewHeight) {

        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<Integer> getSupportedFlashModes(CameraCharacteristics characteristics) {
        ArrayList<Integer> flashModes = new ArrayList<>();
        if (CameraApp.getContext().getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {

            int[] modes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            if (modes == null || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AE_MODE_OFF))
                return flashModes;

            for (int mode : modes) {
                switch (mode) {
                    case CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH:
                        flashModes.add(FLASH_MODE_AUTO);
                        break;
                    case CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH:
                        flashModes.add(FLASH_MODE_ALWAYS_ON);
                        break;
                    case CameraCharacteristics.CONTROL_AE_MODE_ON:
                        flashModes.add(FLASH_MODE_OFF);
                        break;
                    default:
                        break;
                }
            }
        }
        return flashModes;
    }
}
