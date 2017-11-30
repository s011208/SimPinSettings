package com.tinklabs.yhh.simpinsettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.widget.EditText;

public class CustomPinDialogFragment extends DialogFragment {

    public interface Callback {
        void onClick(String pin);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        EditText editText = new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        return new AlertDialog.Builder(getActivity()).setTitle("Set custom pin")
                .setView(editText).setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    if (getActivity() instanceof Callback) {
                        ((Callback) getActivity()).onClick(String.valueOf(editText.getText()));
                    }
                }).show();
    }
}
