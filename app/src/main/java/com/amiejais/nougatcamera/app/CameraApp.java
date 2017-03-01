package com.amiejais.nougatcamera.app;

import android.app.Application;
import android.content.Context;

import com.amiejais.nougatcamera.framwork.NCCameraControlTemplate;


/**
 * Application class to handle application level functions
 */
public class CameraApp extends Application {

    private static CameraApp mCameraApp;
    private NCCameraControlTemplate cameraControls;

    public String getClickedImageUrl() {
        return clickedImageUrl;
    }

    public void setClickedImageUrl(String clickedImageUrl) {
        this.clickedImageUrl = clickedImageUrl;
    }

    private String clickedImageUrl;

    public NCCameraControlTemplate getCameraControls() {
        return cameraControls;
    }

    public void setCameraControls(NCCameraControlTemplate cameraControls) {
        this.cameraControls = cameraControls;
    }



    public static Context getContext() {
        return getInstance().getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCameraApp = this;
    }

    public static CameraApp getInstance(){
        if(mCameraApp==null){
            mCameraApp=new CameraApp();
        }
        return mCameraApp;
    }

}
