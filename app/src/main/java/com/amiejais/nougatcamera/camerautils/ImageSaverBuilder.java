package com.amiejais.nougatcamera.camerautils;

import android.media.Image;
import android.media.ImageReader;

import com.amiejais.nougatcamera.ui.fragment.NougatCameraFragment;

import java.io.File;

/**
 * Builder class for constructing {@link ImageSaver}s.
 * <p/>
 * This class is thread safe.
 */
public class ImageSaverBuilder {
    private Image mImage;
    private File mFile;
    private RefCountedAutoCloseable<ImageReader> mReader;

    public synchronized ImageSaverBuilder setRefCountedReader(
            RefCountedAutoCloseable<ImageReader> reader) {
        if (reader == null) throw new NullPointerException();

        mReader = reader;
        return this;
    }

    public synchronized ImageSaverBuilder setImage(final Image image) {
        if (image == null) throw new NullPointerException();
        mImage = image;
        return this;
    }

    public synchronized ImageSaverBuilder setFile(final File file) {
        if (file == null) throw new NullPointerException();
        mFile = file;
        return this;
    }

    public synchronized ImageSaver buildIfComplete(NougatCameraFragment activity) {
        if (!isComplete()) {
            return null;
        }
        return new ImageSaver(activity,mImage, mFile, mReader);
    }


    private boolean isComplete() {
        return mImage != null && mFile != null;
    }
}