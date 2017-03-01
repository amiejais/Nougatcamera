package com.amiejais.nougatcamera.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.Toast;

/**
 * Created by KNPX0678 on 16-Feb-17.
 */

public class CommonUtils {

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show.
     */
    public static  void showToast(String text) {
       // Toast.makeText(CameraApp.getContext(), text, Toast.LENGTH_SHORT).show();
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

    /**
     * Method to adjust alpha values while setting up the image on view.
     *
     * @param color  color
     * @param factor factor
     * @return color
     */
    public static int adjustAlpha(int color, @SuppressWarnings("SameParameterValue") float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

}
