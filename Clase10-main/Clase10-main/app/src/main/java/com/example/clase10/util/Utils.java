package com.example.clase10.util;

import android.content.Context;
import android.util.Log;

import com.example.clase10.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Utils {

    public static void mostrarResultado(Context context, String resultado) {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setTitle("Buscar");
        dialogBuilder.setMessage(resultado);
        dialogBuilder.setPositiveButton(R.string.ok, (dialogInterface, i) -> Log.d("msg-test","btn positivo"));
        dialogBuilder.show();
    }
}
