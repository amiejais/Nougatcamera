package com.amiejais.nougatcamera.listeners;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.TextureView;

import com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment;


/**
 * Created by KNPX0678 on 16-Feb-17.
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class NCSurfaceTextureListener implements TextureView.SurfaceTextureListener {


    private NougatCameraFragment mFragment;
    public NCSurfaceTextureListener(NougatCameraFragment fragment) {
        mFragment = fragment;
    }

    /**
     * Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
     *
     * @param surface The surface returned by
     *                {@link TextureView#getSurfaceTexture()}
     * @param width   The width of the surface
     * @param height  The height of the surface
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mFragment.openCamera();
    }

    /**
     * Invoked when the {@link SurfaceTexture}'s buffers size changed.
     *
     * @param surface The surface returned by
     *                {@link TextureView#getSurfaceTexture()}
     * @param width   The new width of the surface
     * @param height  The new height of the surface
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mFragment.configureTransform(width, height,mFragment.mPreviewSize,mFragment.mTextureView);
    }

    /**
     * Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
     * If returns true, no rendering should happen inside the surface texture after this method
     * is invoked. If returns false, the client needs to call {@link SurfaceTexture#release()}.
     * Most applications should return true.
     *
     * @param surface The surface about to be destroyed
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        synchronized (mFragment.mCameraStateLock) {
            mFragment.mPreviewSize = null;
        }
        return true;
    }

    /**
     * Invoked when the specified {@link SurfaceTexture} is updated through
     * {@link SurfaceTexture#updateTexImage()}.
     *
     * @param surface The surface just updated
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
