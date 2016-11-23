package org.edx.mobile.interfaces;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * {@link android.app.Activity#onActivityResult(int, int, Intent)} is a protected function in the
 * {@link android.app.Activity} class, by implementing this interface the function becomes
 * publicly accessible.
 */
public interface OnActivityResultListener {
    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link android.app.Activity#RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);

    class Util {
        @Nullable
        public static OnActivityResultListener getListener(@NonNull Context context) {
            return context instanceof OnActivityResultListener ? (OnActivityResultListener) context : null;
        }
    }
}
