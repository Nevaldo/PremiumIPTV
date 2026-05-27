package com.player.iptv.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.player.iptv.R;

public class DialogUtils {

    public interface OnDialogAction {
        void onConfirm();
    }

    public static void showExitPlayerDialog(Context context, OnDialogAction onConfirm) {

        showPremiumDialog(context, R.drawable.ic_exit_player,
                "Sair do Player",
                "Tem certeza que deseja sair do player?",
                "Sair",
                "Cancelar",
                onConfirm
        );
    }

    public static void showExitAppDialog(Context context, OnDialogAction onConfirm) {

        showPremiumDialog(context, R.drawable.ic_exit_app,
                "Sair do Aplicativo",
                "Tem certeza que deseja sair do aplicativo?",
                "Sair",
                "Cancelar",
                onConfirm
        );
    }

    public static void showPremiumDialog(Context context, @DrawableRes int icon, String title, String message, String positiveText, String negativeText, OnDialogAction onConfirm) {

        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_premium, null);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView iconView = view.findViewById(R.id.dialogIcon);
        TextView titleView = view.findViewById(R.id.dialogTitle);
        TextView messageView = view.findViewById(R.id.dialogMessage);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        iconView.setImageResource(icon);

        titleView.setText(title);
        messageView.setText(message);
        btnConfirm.setText(positiveText);
        btnCancel.setText(negativeText);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();

            if (onConfirm != null) {
                onConfirm.onConfirm();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}