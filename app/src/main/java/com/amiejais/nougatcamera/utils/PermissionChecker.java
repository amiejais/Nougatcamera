package com.amiejais.nougatcamera.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.amiejais.nougatcamera.app.CameraApp;
import com.amiejais.nougatcamera.app.ConstantValues;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to check the application permission for above lollipop versions
 */
public class PermissionChecker {

    private static PermissionChecker mPermissionChecker;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    public static final int REQUEST_ID_LOCATION_PERMISSION = 2;
    public static final int REQUEST_ID_CAMERA_AUDIO_PERMISSION = 3;
    public static final int REQUEST_ID_CAMERA_PERMISSION = 3;

    public static PermissionChecker getInstance() {
        if (mPermissionChecker == null) {
            mPermissionChecker = new PermissionChecker();
        }
        return mPermissionChecker;
    }

    /**
     * Method to check all the permission are granted or not.
     *
     * @return true if all the permission is granted nor false.
     */
    public boolean isAllPermissionGranted() {
        return isCameraStoragePermissionGranted()
                && ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns boolean for permission granted for camera and storage.
     *
     * @return boolean
     */
    private boolean isCameraStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Method used to ask for all the required permission from user
     *
     * @param activity activity
     * @return boolean
     */
    public boolean requestAllPermissions(Activity activity) {
        if (ConstantValues.CURRENT_API_VERSION > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int cameraPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.CAMERA);
            int readPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int recordAudioPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.RECORD_AUDIO);
            int locationPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (readPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.CAMERA);
            }
            if (locationPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (recordAudioPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(activity,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                        REQUEST_ID_MULTIPLE_PERMISSIONS);
                return false;
            }
            return true;
        }
        return true;
    }


    /**
     * Method used to check Location Update Permission.
     */
    public boolean checkLocationUpdatePermission(Activity activity) {
        if (ConstantValues.CURRENT_API_VERSION >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_ID_LOCATION_PERMISSION);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Method used to checking the camera permission.
     */
    public boolean isCameraPermissionGranted(Activity activity) {
        if (ConstantValues.CURRENT_API_VERSION > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int cameraPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.CAMERA);
            int readPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (readPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.CAMERA);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(activity,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                        REQUEST_ID_CAMERA_PERMISSION);
                return false;
            }
            return true;
        }
        return true;
    }

    /**
     * Method used to checking the camera permission.
     */
    public boolean isCameraAndAudioPermissionGranted(Activity activity) {
        if (ConstantValues.CURRENT_API_VERSION > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int cameraPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.CAMERA);
            int readPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int recordAudioPermission = ContextCompat.checkSelfPermission(CameraApp.getContext(), Manifest.permission.RECORD_AUDIO);
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (readPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.CAMERA);
            }
            if (recordAudioPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(activity,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                        REQUEST_ID_CAMERA_AUDIO_PERMISSION);
                return false;
            }
            return true;
        }
        return true;
    }

}
