package com.amiejais.nougatcamera.ui.fragment;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.amiejais.nougatcamera.R;
import com.amiejais.nougatcamera.app.CameraApp;
import com.amiejais.nougatcamera.app.ConstantValues;
import com.amiejais.nougatcamera.framwork.NCCameraControlTemplate;
import com.amiejais.nougatcamera.utils.CameraAnnotation;
import com.amiejais.nougatcamera.utils.CommonUtils;

import static com.amiejais.nougatcamera.utils.CameraAnnotation.CAMERA_POSITION_BACK;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_ALWAYS_ON;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_AUTO;
import static com.amiejais.nougatcamera.utils.CameraAnnotation.FLASH_MODE_OFF;


public abstract class NougatCameraBaseFragment extends NCBaseFragment implements View.OnClickListener {

    public ImageView mFlashView;
    public ImageView mCameraSwitchRearView;
    public ImageView mCameraSwitchFrontView;
    private FrameLayout mCameraSwitch;
    private AnimatorSet mSetRightOut;
    private AnimatorSet mSetLeftIn;
    private MediaActionSound sound;
    private RecyclerView mPhotoAdapter;


    /**
     * @return returns layout id of the fragment.
     */
    @Override
    protected int initializeLayoutId() {
        return R.layout.fragment_camera2_basic;
    }

    /**
     * Initialize fragment views.
     *
     * @param savedInstanceState it contain saved values oof bundle
     */
    @Override
    protected void initializeViews(Bundle savedInstanceState) {

        getFragmentView().findViewById(R.id.picture).setOnClickListener(this);
        mFlashView = (ImageView) getFragmentView().findViewById(R.id.flash);
        mCameraSwitchRearView = (ImageView) getFragmentView().findViewById(R.id.btn_switch_rear);
        mCameraSwitchFrontView = (ImageView) getFragmentView().findViewById(R.id.btn_switch_front);
        mCameraSwitch = (FrameLayout) getFragmentView().findViewById(R.id.switch_camera);
        mPhotoAdapter = (RecyclerView) getFragmentView().findViewById(R.id.rl_camera_pics);
        mFlashView.setOnClickListener(this);
        mCameraSwitch.setOnClickListener(this);
        fullScreenSetting();
        loadAnimations();
        changeCameraDistance();
        CameraApp.getInstance().setCameraControls(NCCameraControlTemplate.getInstance());
        updateThumbnail("");
    }


    private void fullScreenSetting() {
        int bottomNavigationHeight = CommonUtils.getSoftButtonsBarSizePort(getActivity());
        DisplayMetrics mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        int screenCell = (int) ((mMetrics.widthPixels) / 6.5);
        RelativeLayout control = (RelativeLayout) getFragmentView().findViewById(R.id.camera_control);
        control.setPadding(screenCell, bottomNavigationHeight, screenCell, bottomNavigationHeight);
    }

    /**
     * Load initial content of the fragment.
     */
    @Override
    protected void loadContent() {

    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_camera:
                toggleCamera();
                break;
            case R.id.picture:
                takePicture();
                break;
            case R.id.flash:
                toggleFlashMode(true);
                break;
        }
    }


    private void takePicture() {
        takeStillShot();
    }

    private void toggleCamera() {
        flipCard();
        CameraApp.getInstance().getCameraControls().toggleCamePos();
        toggleFlashMode(false);
        closeCamera();
        openCamera();
    }

    /**
     * Method to invalidate flash mode.
     *
     * @param toggle true if you want to change the icon else false
     */
    public void toggleFlashMode(boolean toggle) {
        if (toggle)
            CameraApp.getInstance().getCameraControls().toggleFlashMode();
        setupFlashMode();
        if (CameraApp.getInstance().getCameraControls().getCurrentCamPos() == CAMERA_POSITION_BACK) {
            onCameraSettingChange(CameraAnnotation.FLASH);
        }
    }


    /**
     * Method to update and setup the flash mode.
     */
    protected void setupFlashMode() {
        final int res;
        if (CameraApp.getInstance().getCameraControls().getCurrentCamPos() == CAMERA_POSITION_BACK) {
            mFlashView.setEnabled(true);
        } else {
            CameraApp.getInstance().getCameraControls().setFlashMode(FLASH_MODE_OFF);
            mFlashView.setEnabled(false);
        }
        switch (CameraApp.getInstance().getCameraControls().getFlashMode()) {
            case FLASH_MODE_AUTO:
                res = ConstantValues.ICON_FLASH_AUTO;
                break;
            case FLASH_MODE_ALWAYS_ON:
                res = ConstantValues.ICON_FLASH_ON;
                break;
            case FLASH_MODE_OFF:
            default:
                res = ConstantValues.ICON_FLASH_OFF;
        }
        setImageRes(mFlashView, res);
    }


    /**
     * Method to animate to toggle camera image
     */
    private void toggleCameraSwitchImage() {
        RotateAnimation rotate = new RotateAnimation(180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(300);
        rotate.setInterpolator(new LinearInterpolator());
//        mCameraSwitchView.startAnimation(rotate);
//        if (CameraApp.getInstance().getCameraControls().getCurrentCamPos() == CameraAnnotation.CAMERA_POSITION_BACK) {
//            setImageRes(mCameraSwitchView, ConstantValues.ICON_REAR_CAMERA);
//        } else {
//            setImageRes(mCameraSwitchView, ConstantValues.ICON_FRONT_CAMERA);
//        }
    }

    public void flipCard() {

        if (CameraApp.getInstance().getCameraControls().getCurrentCamPos() == CAMERA_POSITION_BACK) {
            mSetRightOut.setTarget(mCameraSwitchRearView);
            mSetLeftIn.setTarget(mCameraSwitchFrontView);
        } else {
            mSetRightOut.setTarget(mCameraSwitchFrontView);
            mSetLeftIn.setTarget(mCameraSwitchRearView);
        }
        mSetRightOut.start();
        mSetLeftIn.start();

    }


    /**
     * Method to set the image on view.
     *
     * @param iv  image
     * @param res resource id.
     */
    protected void setImageRes(ImageView iv, @DrawableRes int res) {
        if (iv != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && iv.getBackground() instanceof RippleDrawable) {
                RippleDrawable rd = (RippleDrawable) iv.getBackground();
                rd.setColor(ColorStateList.valueOf(CommonUtils.
                        adjustAlpha(ContextCompat.getColor(getActivity(), R.color.mcam_color_light), 0.3f)));
            }
            Drawable d = ContextCompat.getDrawable(iv.getContext(), res);
            d = DrawableCompat.wrap(d.mutate());
            iv.setImageDrawable(d);
        }
    }


    private void changeCameraDistance() {
        int distance = 1000;
        float scale = getResources().getDisplayMetrics().density * distance;
        mCameraSwitchRearView.setCameraDistance(scale);
        mCameraSwitchRearView.setCameraDistance(scale);
    }

    private void loadAnimations() {
        mSetRightOut = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.camera_out);
        mSetLeftIn = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.camera_in);
    }

    /**
     * Method to start playing sound on click.
     *
     * @param resId resource id
     */
    public void startPlaying(int resId) {
        try {
            if (sound != null) {
                sound.release();
            }
            sound = new MediaActionSound();
            sound.play(resId);
        } catch (Exception e) {
        }
    }

    /**
     * Method to update thumbnail when image captured ot update.
     *
     * @param absolutePath path
     */
    public void updateThumbnail(final String absolutePath) {
        //mThumbnail.setVisibility(View.VISIBLE);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(absolutePath)) {
                    CameraApp.getInstance().setClickedImageUrl(absolutePath);
                    //mThumbnail.setImageBitmap(ImageUtil.getClip(absolutePath));
                    startPlaying(MediaActionSound.SHUTTER_CLICK);
                } else if (!TextUtils.isEmpty(CameraApp.getInstance().getClickedImageUrl())) {
                    // mThumbnail.setImageBitmap(ImageUtil.getClip(CameraApp.getInstance().getClickedImageUrl()));
                }
            }
        });
    }

    public abstract void openCamera();

    public abstract void closeCamera();

    public abstract void takeStillShot();

    public abstract void onCameraSettingChange(int preference);
}
