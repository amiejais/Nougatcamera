package com.amiejais.nougatcamera.framwork;


import com.amiejais.nougatcamera.utils.CameraAnnotation;

import java.util.List;

import static com.amiejais.nougatcamera.utils.CameraAnnotation.CAMERA_POSITION_BACK;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.CAMERA_POSITION_FRONT;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.CAMERA_POSITION_UNKNOWN;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_OFF;

/**
 * Created by amiejais on 18-Feb-17.
 */

public class NCCameraControlTemplate implements NCCameraControlCalls {

    private int mCameraPosition = CAMERA_POSITION_UNKNOWN;

    private Object mFrontCameraId;

    private Object mBackCameraId;

    private int mFlashMode = FLASH_MODE_OFF;

    private List<Integer> mFlashModes;

    private static NCCameraControlTemplate instance;

    public static NCCameraControlTemplate getInstance() {
        if (null == instance) {
            instance = new NCCameraControlTemplate();
        }
        return instance;
    }

    @Override
    public void setFlashModes(List<Integer> modes) {
        mFlashModes = modes;
    }

    @Override
    public void setFlashMode(int mode) {
        mFlashMode=mode;
    }

    @CameraAnnotation.FlashMode
    @Override
    public int getFlashMode() {
        return mFlashMode;
    }

    @Override
    public void toggleFlashMode() {
        if (mFlashModes != null) {
            mFlashMode = mFlashModes.get((mFlashModes.indexOf(mFlashMode) + 1) % mFlashModes.size());
        }
    }

    @Override
    public void setCurrentCamPos(int position) {
        mCameraPosition = position;
    }

    /**
     * Returns current position of a camera.
     *
     * @return camera position.
     */
    @CameraAnnotation.CameraPosition
    @Override
    public int getCurrentCamPos() {
        return mCameraPosition;
    }

    @Override
    public void toggleCamePos() {
        if (getCurrentCamPos() == CAMERA_POSITION_FRONT) {
            if (getBackCam() != null)
                setCurrentCamPos(CAMERA_POSITION_BACK);
        } else {
            if (getFrontCam() != null)
                setCurrentCamPos(CAMERA_POSITION_FRONT);
        }
    }

    @Override
    public Object getCurrentCamId() {
        if (getCurrentCamPos() == CAMERA_POSITION_FRONT)
            return getFrontCam();
        else return getBackCam();
    }

    @Override
    public void setFrontCam(Object cameraId) {
        mFrontCameraId = cameraId;
    }

    @Override
    public void setBackCam(Object cameraId) {
        mBackCameraId = cameraId;
    }

    @Override
    public Object getFrontCam() {
        return mFrontCameraId;
    }

    @Override
    public Object getBackCam() {
        return mBackCameraId;
    }
}
