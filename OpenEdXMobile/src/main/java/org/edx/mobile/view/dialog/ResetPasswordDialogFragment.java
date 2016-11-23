package org.edx.mobile.view.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.authentication.LoginService;
import org.edx.mobile.http.CallTrigger;
import org.edx.mobile.http.ErrorHandlingCallback;
import org.edx.mobile.interfaces.OnActivityResultListener;
import org.edx.mobile.model.api.ResetPasswordResponse;
import org.edx.mobile.util.InputValidationUtil;
import org.edx.mobile.util.NetworkUtil;
import org.edx.mobile.util.SoftKeyboardUtil;
import org.edx.mobile.util.images.ErrorUtils;

import retrofit2.Call;
import roboguice.fragment.RoboDialogFragment;

public class ResetPasswordDialogFragment extends RoboDialogFragment {
    @Inject
    private LoginService loginService;

    private static final String EXTRA_LOGIN_EMAIL = "login_email";

    public static int RESULT_OK = 3333;
    public static int RESULT_FAILURE = 2222;

    @NonNull
    private TextInputEditText editText;

    @NonNull
    private TextInputLayout textInputLayout;

    @NonNull
    private View loadingIndicator;

    @Nullable
    private Call<ResetPasswordResponse> resetCall;

    public static ResetPasswordDialogFragment newInstance(@Nullable String email) {
        ResetPasswordDialogFragment fragment = new ResetPasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_LOGIN_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.reset_dialog_title)
                .setMessage(R.string.confirm_dialog_message_help)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.setCanceledOnTouchOutside(false);

        final ViewGroup layout = (ViewGroup) getActivity().getLayoutInflater()
                .inflate(R.layout.reset_dialog_email_input, null);
        textInputLayout = (TextInputLayout) layout.findViewById(R.id.input_layout);
        editText = (TextInputEditText) layout.findViewById(R.id.edit_text);
        loadingIndicator = layout.findViewById(R.id.loading_indicator);
        editText.setText(getArguments().getString(EXTRA_LOGIN_EMAIL));
        alertDialog.setView(layout);

        return alertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        /*
         * There are two OnClickListeners associated with an alert dialog button:
         * DialogInterface.OnClickListener and View.OnClickListener.
         *
         * DialogInterface.OnClickListener defines what clicking the button
         * does before the normal flow takes over (before the dialog is auto-dismissed)
         * This is what is generally set in AlertDialog.Builder.
         *
         * View.OnClickListener is the listener for the complete behavior of the button.
         * This has to be set by finding the button (after it has been created),
         * and overriding it. If you want to override the normal button click flow,
         * do so by overriding View.OnClickListener.
         *
         * We want to prevent the dialog from automatically closing on positive button click,
         * and letting submit() control the logic so, we are overriding View.OnClickListener here.
         */
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submit(editText.getText().toString().trim());
                        SoftKeyboardUtil.hide(textInputLayout);
                    }
                });
    }

    public void showError(@NonNull String error) {
        textInputLayout.setError(error);
        textInputLayout.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }

    private void submit(@Nullable String email) {
        if (!NetworkUtil.isConnected(getContext())) {
            showError(getString(R.string.network_not_connected_short));
        } else if (!InputValidationUtil.isValidEmail(email)) {
            showError(getString(R.string.error_invalid_email));
        } else {
            setUiForInteraction(false);
            loadingIndicator.setVisibility(View.VISIBLE);
            resetCall = loginService.resetPassword(email);
            resetCall.enqueue(new ErrorHandlingCallback<ResetPasswordResponse>(
                    getContext(), CallTrigger.USER_ACTION) {
                @Override
                protected void onResponse(@NonNull final ResetPasswordResponse result) {
                    setUiForInteraction(true);
                    final OnActivityResultListener listener =
                            OnActivityResultListener.Util.getListener(getContext());
                    if (listener != null) {
                        if (result.isSuccess()) {
                            listener.onActivityResult(0, RESULT_OK, null);
                            dismiss();
                        } else {
                            final String errorMsg = result.getPrimaryReason();
                            final Intent data = new Intent();
                            data.putExtra("error", errorMsg);
                            listener.onActivityResult(0, RESULT_FAILURE, data);
                            showError(errorMsg);
                        }
                    }
                }

                @Override
                protected void onFailure(@NonNull Throwable error) {
                    setUiForInteraction(true);
                    final OnActivityResultListener listener =
                            OnActivityResultListener.Util.getListener(getContext());
                    if (listener != null) {
                        final String errorMsg = ErrorUtils.getErrorMessage(error, getContext());
                        final Intent data = new Intent();
                        data.putExtra("error", errorMsg);
                        listener.onActivityResult(0, RESULT_FAILURE, data);
                        showError(errorMsg);
                    }
                }
            });
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (resetCall != null) {
            resetCall.cancel();
        }
    }

    private void setUiForInteraction(boolean enabled) {
        if (getDialog() != null) {
            editText.setEnabled(enabled);
            loadingIndicator.setVisibility(enabled ? View.GONE : View.VISIBLE);
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
        }
    }
}
