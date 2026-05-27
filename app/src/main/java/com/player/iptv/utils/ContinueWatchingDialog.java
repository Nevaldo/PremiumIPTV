package com.player.iptv.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.player.iptv.R;


public class ContinueWatchingDialog {

    public interface Listener {
        void onContinue();
        void onStartOver();
    }

    public static void show(Context context, String remainingTime, Listener listener) {

        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_continue_watching, null);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView txtRemaining = view.findViewById(R.id.txtRemaining);
        View btnContinue = view.findViewById(R.id.btnContinue);
        View btnStartOver = view.findViewById(R.id.btnStartOver);
        txtRemaining.setText(remainingTime);

        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();

            if (listener != null) {
                listener.onContinue();
            }
        });

        btnStartOver.setOnClickListener(v -> {

            dialog.dismiss();

            if (listener != null) {
                listener.onStartOver();
            }
        });

        dialog.show();
    }
}