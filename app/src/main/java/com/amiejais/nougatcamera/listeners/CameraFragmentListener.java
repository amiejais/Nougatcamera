package com.amiejais.nougatcamera.listeners;

import android.graphics.Bitmap;


/**
 * Listener interface that has to be implemented by activities using
 * {@link } instances.
 *
 * @author Sebastian Kaspari <sebastian@androidzeitgeist.com>
 */
public interface CameraFragmentListener {
    /**
     * A non-recoverable camera error has happened.
     */
    public void onCameraError();

    /**
     * A picture has been taken.
     *
     * @param bitmap
     */
    public void onPictureTaken(Bitmap bitmap);
}
