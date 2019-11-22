package app.br.chronlog.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import app.br.chronlog.R;

public class Utils {
    public final static String CONFIG_FILE = "0";

    public static AlertDialog createDialog(
            Context context,
            String title,
            String body,
            String positive,
            String negative,
            boolean cancelableBtn,
            boolean cancelableDismiss,
            DialogInterface.OnClickListener negativeListener,
            DialogInterface.OnClickListener positiveListener,
            DialogInterface.OnDismissListener dismissListener
    ){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton(positive, positiveListener);
        if(cancelableBtn) {
            builder.setNegativeButton(negative, negativeListener);
        }
        if(cancelableDismiss) {
            builder.setOnDismissListener(dismissListener);
        }
        builder.setBackground(context.getDrawable(R.drawable.shape_gradient));

        return builder.create();
    }

    public static AlertDialog createLoadingDialog(Context context, String title, String body){

        final LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        final View dialogAlertView = inflater.inflate(R.layout.dialog_loading_view, null);

        ((TextView) dialogAlertView.findViewById(R.id.textTitleAlert)).setText(title);
        ((TextView) dialogAlertView.findViewById(R.id.textBodyAlert)).setText(body);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setView(dialogAlertView).create();

    }

    public static void startActivityWithExplosion(Context context, Intent intent) {
        final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation((Activity)context);
        ((Activity) context).overridePendingTransition(0,0);
        context.startActivity(intent, options.toBundle());
    }

    public static void destroyDialog(AlertDialog alert) {
        if(alert != null) {
            if(alert.isShowing()) {
                alert.dismiss();
            }
            alert = null;
        }
    }


    public static void saveToMyGallery(Context context, Chart chart, String name) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            if (chart.saveToGallery(name + "_" + System.currentTimeMillis(), 70))
                Toast.makeText(context, "Saving SUCCESSFUL!",
                        Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, "Saving FAILED!", Toast.LENGTH_SHORT)
                        .show();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(((Activity)context), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Snackbar.make(null, "Write permission is required to save image to gallery", Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, v -> ActivityCompat.requestPermissions((Activity)context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0)).show();
            } else {
                Toast.makeText(context, "Permission Required!", Toast.LENGTH_SHORT).show();
                // 0 PERMISSION_STORAGE
                ActivityCompat.requestPermissions((Activity)context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }

    }


    public static void addSmoothEffectActivit(Activity activity){
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}

