package com.amiejais.nougatcamera.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

/**
 * A dialog fragment for displaying non-recoverable errors; this {@ling Activity} will be
 * finished once the dialog has been acknowledged by the user.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public  class ErrorDialog extends android.app.DialogFragment {

    private String mErrorMessage;

    public ErrorDialog() {
        mErrorMessage = "Unknown error occurred!";
    }

    // Build a dialog with a custom message (Fragments require default constructor).
    public static ErrorDialog buildErrorDialog(String errorMessage) {
        ErrorDialog dialog = new ErrorDialog();
        dialog.mErrorMessage = errorMessage;
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        return new AlertDialog.Builder(activity)
                .setMessage(mErrorMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        activity.finish();
                    }
                })
                .create();
    }
}