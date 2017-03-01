package com.amiejais.nougatcamera.framwork;


import com.amiejais.nougatcamera.utils.CameraAnnotation;

import java.util.List;

/**
 * Created by KNPX0678 on 18-Feb-17.
 */

public interface NCCameraControlCalls {

    void toggleFlashMode();

    @CameraAnnotation.FlashMode
    int getFlashMode();

    void setFlashModes(List<Integer> modes);

    void setFlashMode(int mode);

    void setCurrentCamPos(int position);

    @CameraAnnotation.CameraPosition
    int getCurrentCamPos();

    void toggleCamePos();

    Object getCurrentCamId();

    void setFrontCam(Object cameraId);

    void setBackCam(Object cameraId);

    Object getFrontCam();

    Object getBackCam();
}
