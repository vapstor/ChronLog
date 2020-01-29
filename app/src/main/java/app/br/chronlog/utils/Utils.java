package app.br.chronlog.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import app.br.chronlog.R;
import app.br.chronlog.activitys.MainActivity;
import app.br.chronlog.utils.bluetooth.BluetoothController;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialSocket;

import static android.view.View.GONE;

public class Utils {
    public final static String CONFIG_FILE = "0";
    public final static String TAG_LOG = "CHRONLOG";
    public static long mLastClickTime = 0;

    public static SerialSocket serialSocket;
    public static Connected isDeviceConnected;

    public enum Connected {False, Pending, True}

    public static BluetoothController myBluetoothController;
    public static BluetoothDevice bluetoothDeviceSelected;

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
    ) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton(positive, positiveListener);

        if (cancelableBtn) {
            builder.setNegativeButton(negative, negativeListener);
        }
        if (cancelableDismiss) {
            builder.setOnDismissListener(dismissListener);
        } else {
            builder.setCancelable(false);
        }
        return builder.create();
    }


    public static TextWatcher insertMask(final String mask, final EditText et) {
        return new TextWatcher() {
            boolean isUpdating;
            String oldTxt = "";
            int myCounter = 0;

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count < myCounter) {
                    String str = unmask(s.toString());
                    String maskCurrent = "";
                    if (isUpdating) {
                        oldTxt = str;
                        isUpdating = false;
                        return;
                    }
                    int i = 0;
                    for (char m : mask.toCharArray()) {
                        if (m != '#' && str.length() > oldTxt.length()) {
                            maskCurrent += m;
                            continue;
                        }
                        try {
                            maskCurrent += str.charAt(i);
                        } catch (Exception e) {
                            break;
                        }
                        i++;
                    }
                    isUpdating = true;
                    et.setText(maskCurrent);
                    et.setSelection(maskCurrent.length());
                }
            }

            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        };
    }

    private static String unmask(String s) {
        return s.replaceAll("[.]", "").replaceAll("[-]", "")
                .replaceAll("[/]", "").replaceAll("[(]", "")
                .replaceAll("[)]", "");
    }

    public static AlertDialog createLoadingDialog(Context context, String title, String body) {

        final LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        final View dialogAlertView = inflater.inflate(R.layout.dialog_loading_view, null);

        ((TextView) dialogAlertView.findViewById(R.id.textTitleAlert)).setText(title);
        ((TextView) dialogAlertView.findViewById(R.id.textBodyAlert)).setText(body);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setView(dialogAlertView).create();

    }

    public static void startActivityWithExplosion(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation((Activity) context);
            ((Activity) context).overridePendingTransition(0, 0);
            context.startActivity(intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    public static void destroyDialog(AlertDialog alert) {
        if (alert != null) {
            if (alert.isShowing()) {
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
            if (ActivityCompat.shouldShowRequestPermissionRationale(((Activity) context), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Snackbar.make(null, "Write permission is required to save image to gallery", Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, v -> ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0)).show();
            } else {
                Toast.makeText(context, "Permission Required!", Toast.LENGTH_SHORT).show();
                // 0 PERMISSION_STORAGE
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }

    }


    public static String getFileContents(final File file) throws IOException {
        final InputStream inputStream = new FileInputStream(file);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final StringBuilder stringBuilder = new StringBuilder();

        boolean done = false;

        while (!done) {
            final String line = reader.readLine();
            done = (line == null);

            if (line != null) {
                stringBuilder.append(line).append("\r\n");
            }
        }

        reader.close();
        inputStream.close();

        String fileAsString = stringBuilder.toString();
        return fileAsString;
    }

    /**
     * Send And Receive data dinamicamente
     */
    public static void send(String str, Activity activity, SerialListener listener) {
        if (isDeviceConnected != Connected.True) {
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Desconectado", Toast.LENGTH_SHORT).show();
                if (!(activity instanceof MainActivity)) {
                    activity.finishAndRemoveTask();
                    activity.startActivity(new Intent(activity, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            });
            return;
        }
        try {
            byte[] data = (str).getBytes();
            if (serialSocket != null) {
                serialSocket.write(data);
            }
        } catch (Exception e) {
            listener.onSerialIoError(e);
        }
    }

    public static String receive(byte[] data) {
        return new String(data);
    }


    /**
     * Métodos para atualizar status bar
     */

    public static void setStatus(String status, Context context) {
        TextView statusView = ((Activity) context).findViewById(R.id.status);
        statusView.setText(status);
    }

    public static void showProgressBar(Context context) {
        ProgressBar progressBar = ((Activity) context).findViewById(R.id.progressBarAppBar);
        if (progressBar != null) {
            if (progressBar.getVisibility() == GONE) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    public static void hideProgressBar(Context context) {
        ProgressBar progressBar = ((Activity) context).findViewById(R.id.progressBarAppBar);
        if (progressBar != null) {
            if (progressBar.getVisibility() != GONE) {
                progressBar.setVisibility(GONE);
            }
        }
    }

}

