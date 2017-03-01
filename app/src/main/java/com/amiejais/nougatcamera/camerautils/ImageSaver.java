package com.amiejais.nougatcamera.camerautils;

import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;

import com.amiejais.nougatcamera.app.CameraApp;
import com.amiejais.nougatcamera.ui.fragment.NougatCameraBaseFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Runnable that saves an {@link Image} into the specified {@link File}, and updates
 * {@link android.provider.MediaStore} to include the resulting file.
 * <p/>
 * This can be constructed through an {@link ImageSaver} as the necessary image and
 * result information becomes available.
 */
public class ImageSaver implements Runnable {


    private NougatCameraBaseFragment mNougatFragment;
    /**
     * The image to save.
     */
    private final Image mImage;
    /**
     * The file we save the image into.
     */
    private final File mFile;

    /**
     * A reference counted wrapper for the ImageReader that owns the given image.
     */
    private final RefCountedAutoCloseable<ImageReader> mReader;

    public ImageSaver(NougatCameraBaseFragment mBaseFragment, Image image, File file,
                      RefCountedAutoCloseable<ImageReader> reader) {
        mNougatFragment=mBaseFragment;
        mImage = image;
        mFile = file;
        mReader = reader;
    }

    @Override
    public void run() {
        boolean success = false;
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mReader.close();
        if (success) {
            MediaScannerConnection.scanFile(CameraApp.getContext(),
                    new String[]{mFile.getAbsolutePath().replaceAll("file://", "")}, null, null);

            mNougatFragment.updateThumbnail(mFile.getAbsolutePath().replaceAll("file://", ""));
        }
    }
}