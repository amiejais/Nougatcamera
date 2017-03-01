package com.amiejais.nougatcamera.utils;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by KNPX0678 on 18-Feb-17.
 */

public class CameraAnnotation {


    public static CameraAnnotation instance;

    public static CameraAnnotation getInstance() {
        if (instance == null) {
            instance = new CameraAnnotation();
        }
        return instance;
    }

    public static final int CAMERA_POSITION_UNKNOWN = 0;
    public static final int CAMERA_POSITION_FRONT = 1;
    public static final int CAMERA_POSITION_BACK = 2;

    @IntDef({CAMERA_POSITION_UNKNOWN,CAMERA_POSITION_FRONT, CAMERA_POSITION_BACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraPosition {
    }

    public static final int FLASH_MODE_OFF = 0;
    public static final int FLASH_MODE_ALWAYS_ON = 1;
    public static final int FLASH_MODE_AUTO = 2;
    public static final int FLASH_MODE_TORCH = 3;


    @IntDef({FLASH_MODE_OFF, FLASH_MODE_ALWAYS_ON,FLASH_MODE_AUTO,FLASH_MODE_TORCH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FlashMode {
    }


    public static final int FLASH = 1;
    public static final int EXPOSURE = 2;

    @IntDef({FLASH, EXPOSURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraPreference {
    }

    int currentMode= FLASH;

    public int getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(int currentMode) {
        this.currentMode = currentMode;
    }
}
