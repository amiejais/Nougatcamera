package com.amiejais.nougatcamera.app;

import android.os.Build;

import com.amiejais.nougatcamera.R;


/**
 * Collection of constant values used in application
 */
public class ConstantValues {

    public static final int CURRENT_API_VERSION = Build.VERSION.SDK_INT;

    public static final boolean APP_DEBUG = true;

    public static final int ICON_STILL_SHOT = R.drawable.camera;

    public static final int ICON_FLASH_AUTO = R.drawable.mcam_action_flash_auto;
    public static final int ICON_FLASH_ON = R.drawable.mcam_action_flash;
    public static final int ICON_FLASH_OFF = R.drawable.mcam_action_flash_off;

    public static final int ICON_REAR_CAMERA = R.drawable.mcam_camera_rear;
    public static final int ICON_FRONT_CAMERA = R.drawable.mcam_camera_front;

    private ConstantValues() {

    }

}
