package com.amiejais.nougatcamera.listeners;

import android.annotation.TargetApi;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;

import com.amiejais.nougatcamera.camerautils.ImageSaverBuilder;
import com.amiejais.nougatcamera.camerautils.RefCountedAutoCloseable;
import com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by KNPX0678 on 16-Feb-17.
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NCImageAvailableListener implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "NCImageAvailableListener";
    private NougatCameraFragment mNougatCameraFragment;

    public NCImageAvailableListener(NougatCameraFragment pNougatCameraFragment) {
        mNougatCameraFragment = pNougatCameraFragment;
    }

    /**
     * Callback that is called when a new image is available from ImageReader.
     *
     * @param reader the ImageReader the callback is associated with.
     * @see ImageReader
     */
    @Override
    public void onImageAvailable(ImageReader reader) {
        dequeueAndSaveImage(mNougatCameraFragment.mJpegResultQueue, mNougatCameraFragment.mJpegImageReader);
    }


    /**
     * Retrieve the next {@link Image} from a reference counted {@link ImageReader}, retaining
     * that {@link ImageReader} until that {@link Image} is no longer in use, and set this
     * {@link Image} as the result for the next request in the queue of pending requests.  If
     * all necessary information is available, begin saving the image to a file in a background
     * thread.
     *
     * @param pendingQueue the currently active requests.
     * @param reader       a reference counted wrapper containing an {@link ImageReader} from which
     *                     to acquire an image.
     */
    private void dequeueAndSaveImage(TreeMap<Integer, ImageSaverBuilder> pendingQueue,
                                     RefCountedAutoCloseable<ImageReader> reader) {
        synchronized (mNougatCameraFragment.mCameraStateLock) {
            Map.Entry<Integer, ImageSaverBuilder> entry =
                    pendingQueue.firstEntry();
            ImageSaverBuilder builder = entry.getValue();

            // Increment reference count to prevent ImageReader from being closed while we
            // are saving its Images in a background thread (otherwise their resources may
            // be freed while we are writing to a file).
            if (reader == null || reader.getAndRetain() == null) {
                pendingQueue.remove(entry.getKey());
                return;
            }

            Image image;
            try {
                image = reader.get().acquireNextImage();
            } catch (IllegalStateException e) {
                pendingQueue.remove(entry.getKey());
                return;
            }

            builder.setRefCountedReader(reader).setImage(image);

            mNougatCameraFragment.handleCompletionLocked(entry.getKey(), builder, pendingQueue);
        }
    }
}
