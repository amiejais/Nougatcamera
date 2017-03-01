package com.amiejais.nougatcamera.camerautils;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.amiejais.nougatcamera.R;
import com.amiejais.nougatcamera.app.CameraApp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


/**
 * Collection of image utility functions used in this package.
 */
public class ImageUtil {

    private static final int THUMB_SIZE = 64;
    private static final String TAG = "ImageUtil";

    private ImageUtil() {
    }

    /**
     * Method to clip the bitmap for thumbnail.
     *
     * @param absolutePath image path
     * @return image
     */
    public static Bitmap getClip(@NonNull String absolutePath) {
        Bitmap bitmap;

        bitmap = getThumbnailFromImage(absolutePath);

        if (null != bitmap) {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final Paint paint = new Paint();
            final Paint stroke = new Paint();

            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            paint.setFilterBitmap(true);
            stroke.setFilterBitmap(true);
            paint.setDither(true);
            stroke.setDither(true);

            paint.setAntiAlias(true);
            stroke.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);

            stroke.setColor(Color.parseColor("#95EF7900"));
            stroke.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                    bitmap.getWidth() / 2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                    bitmap.getWidth() / 2, stroke);

            final int rotationInDegrees = getExifDegreesFromJpeg(absolutePath);
            Matrix matrix = new Matrix();

                if (rotationInDegrees != 0) {
                    matrix.preRotate(rotationInDegrees);
                }
            return Bitmap.createBitmap(output, 0, 0, output.getWidth(),
                    output.getHeight(), matrix, true);
        }
        return BitmapFactory.decodeResource(CameraApp.getContext().getResources(), R.drawable.thumnail_img_bg);
    }

    /**
     * Method to resize the thumbnail using THUMB_SIZE.
     *
     * @param path image path.
     * @return bitmap.
     */
    private static Bitmap getThumbnailFromImage(String path) {
        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(
                BitmapFactory.decodeFile(path.replaceAll(CameraApp.getContext().
                        getString(R.string.file_prefix), "")), THUMB_SIZE, THUMB_SIZE);
        if (null == thumbImage) {
            return null;
        }
        return thumbImage;
    }

    /**
     * Method to create bitmap from video clip.
     *
     * @param filePath image path
     * @return bitmap
     */
    private static Bitmap getThumbnailFromVideo(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        String imageFilePath = filePath.replaceAll("file://", "");
        if (new File(imageFilePath).exists()) {
            return ThumbnailUtils.createVideoThumbnail(imageFilePath, MediaStore.Images.Thumbnails.MICRO_KIND);
        }
        return null;
    }






    /**
     * Returns rotation value for image took from camera
     *
     * @param inputFile input file
     * @return int orientation
     */
    private static int getExifDegreesFromJpeg(String inputFile) {
        try {
            final ExifInterface exif = new ExifInterface(inputFile);
            final int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
        } catch (IOException e) {
            Log.e("exif", "ErrorUtils when trying to get exif data from : " + inputFile, e);
        }
        return 0;
    }

    /**
     * Returns last image captured from the orange camera
     *
     * @return image path
     */
    @SuppressWarnings("deprecation")
    public static String getLastImageFromGallery() {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        Cursor cursor = null;
        try {
            cursor = CameraApp.getContext().getContentResolver()
                    .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, MediaStore.Images.Media.DATA + " like ? ",
                            new String[]{"%OrangeCamera%"}, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

            if (null != cursor && cursor.moveToFirst() && cursor.getCount() > 0) {
                return cursor.getString(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "ErrorUtils: " + e);
        } finally {
            try {
                if (null != cursor) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "ErrorUtils: " + e);
            }
        }

        return "";
    }

    /**
     * Returns last video captured from the orange camera
     *
     * @return path of a video file
     */
    public static String getLastVideoFromGallery() {
        File fl = new File(Environment.getExternalStorageDirectory(), "OrangeCamera");
        File[] files = fl.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (null == files) {
            return null;
        }
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod && file.getAbsolutePath().endsWith(".mp4")) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        if (null == choice) {
            return null;
        }
        return choice.getAbsolutePath();
    }

    /**
     * Method to set bitmap on image view
     *
     * @param imageView view
     * @param bitmap    bitmap
     * @param url       path
     */
    public static void setBitMap(ImageView imageView, Bitmap bitmap, String url) {
        try {
            ExifInterface exif = new ExifInterface(url);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (rotation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    break;
                default:
                    break;
            }
            if (bitmap == null)
                return;
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            imageView.setImageBitmap(rotatedBitmap);

        } catch (IOException ex) {
//            OrangeCameraLogger.error(TAG, "Failed to get Exif data", ex);
        }
    }


    /**
     * Method to refresh the gallery to load the latest image
     */
    public static void refreshGallery(String editedImageUri) {
        if (editedImageUri != null)
            MediaScannerConnection.scanFile(CameraApp.getContext(),
                    new String[]{editedImageUri.replaceAll("file://", "")}, null, null);
    }


    /**
     * Method to get Scaled Bitmap of required height width
     */
    public static Bitmap getScaledBitmap(Bitmap source, int newWidth, int newHeight) {

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    /**
     * Returns circular bitmap from image.
     *
     * @param input bitmap
     * @return bitmap
     */
    public static Bitmap generateCircularBitmap(Bitmap input) {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Path path = new Path();
        path.addCircle(
                (float) (width / 2)
                , (float) (height / 2)
                , (float) Math.min(width, height / 2)
                , Path.Direction.CCW
        );

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(input, 0, 0, null);
        return outputBitmap;
    }

    /**
     * Returns get image from assets folder by image type
     *
     * @param name folder name
     * @return image drawable
     */
    public static Drawable getImageFromAssert(String name) {
        try {
            InputStream ims = CameraApp.getContext().getAssets().open(name);
            return Drawable.createFromStream(ims, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method to return the images from assets
     *
     * @param type given folder name
     * @return all the image from given folder
     */
    public static List<String> getAssetsByType(String type) throws IOException {
        String[] files = CameraApp.getContext().getAssets().list(type);
        return Arrays.asList(files);
    }

    public static int getSoftButtonsBarSizePort(Activity activity) {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return (realHeight - usableHeight) + 10;
            else
                return 0;
        }
        return 0;
    }
}

