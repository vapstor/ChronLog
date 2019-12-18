package app.br.chronlog.activitys;


import android.Manifest;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import app.br.chronlog.R;

import static app.br.chronlog.utils.Utils.CONFIG_FILE;
import static app.br.chronlog.utils.Utils.destroyDialog;
import static app.br.chronlog.utils.Utils.startActivityWithExplosion;

public class LoginActivity extends AppCompatActivity {
    AlertDialog alert;
    SharedPreferences sharedPreferences;
    private View dialogLoginLogista;
    private static final int REQUEST_USE_BLUETOOTH_PRIVILEGED = 1;
    private static final int REQUEST_USE_COARS_LOCATION = 2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        Toast.makeText(this, "Aparelho não verificado!", Toast.LENGTH_SHORT).show();
        setContentView(R.layout.activity_login);

        dialogLoginLogista = findViewById(R.id.dialog);
        ((ViewGroup) dialogLoginLogista).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        TextView serialTV = findViewById(R.id.serial_input);
        Button btnLogin = findViewById(R.id.login);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String serial = serialTV.getText().toString();
                if (!serial.equals("")) {
                    btnLogin.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        serialTV.addTextChangedListener(tw);

        btnLogin.setOnClickListener((v) -> {
            v.setEnabled(false);
            sharedPreferences.edit().putBoolean("aparelho_verificado", true).apply();
            Toast.makeText(this, "Configurando aparelho...", Toast.LENGTH_SHORT).show();
//                alert = createLoadingDialog(this, "Efetuando Login", "Por Favor aguarde enquanto efetuamos a autenticação!");
//                alert.show();

            findViewById(R.id.serial_input).setVisibility(View.INVISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            new Thread(() -> {
                final TextView[] status = new TextView[1];
                try {
                    Thread.sleep(500);
                    runOnUiThread(() -> {
                        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        status[0] = findViewById(R.id.status);
                        status[0].setVisibility(View.VISIBLE);
                        status[0].setText(R.string.serial_configurado);
                    });

                    Thread.sleep(500);

                    runOnUiThread(() -> {
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivityWithExplosion(this, intent);
                        v.setEnabled(true);
                    });


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        });
    }

    @Override
    protected void onDestroy() {
        destroyDialog(alert);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_USE_COARS_LOCATION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_PRIVILEGED}, REQUEST_USE_BLUETOOTH_PRIVILEGED);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                // ANDROID 6.0 AND UP!
                boolean accessCoarseLocationAllowed = false;
                boolean bluetoothPrivileged = false;
                try {
                    // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
                    java.lang.reflect.Method methodCheckPermission = Activity.class.getMethod("checkSelfPermission", java.lang.String.class);
                    Object resultObj = methodCheckPermission.invoke(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    int result = 0;
                    if (resultObj != null) {
                        result = Integer.parseInt(resultObj.toString());
                    }
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        accessCoarseLocationAllowed = true;
                    }
                } catch (Exception ignored) {
                }
                if (accessCoarseLocationAllowed) {
                    Toast.makeText(this, "Localização concedida!", Toast.LENGTH_SHORT).show();
                }
                try {
                    // We have to invoke the method "void requestPermissions (Activity activity, String[] permissions, int requestCode) "
                    // from android 6
                    java.lang.reflect.Method methodRequestPermission = Activity.class.getMethod("requestPermissions", java.lang.String[].class, int.class);
                    methodRequestPermission.invoke(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0x12345);
                } catch (Exception ignored) {
                }
            }
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_PRIVILEGED}, REQUEST_USE_BLUETOOTH_PRIVILEGED);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                // ANDROID 6.0 AND UP!
                boolean bluetoothPrivileged = false;
                try {
                    // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
                    java.lang.reflect.Method methodCheckPermission = Activity.class.getMethod("checkSelfPermission", java.lang.String.class);
                    Object resultObj = methodCheckPermission.invoke(this, Manifest.permission.BLUETOOTH_PRIVILEGED);
                    int result = 0;
                    if (resultObj != null) {
                        result = Integer.parseInt(resultObj.toString());
                    }
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        bluetoothPrivileged = true;
                    }
                } catch (Exception ignored) {
                }
                try {
                    // We have to invoke the method "void requestPermissions (Activity activity, String[] permissions, int requestCode) "
                    // from android 6
                    java.lang.reflect.Method methodRequestPermission = Activity.class.getMethod("requestPermissions", java.lang.String[].class, int.class);
                    methodRequestPermission.invoke(this, new String[]{Manifest.permission.BLUETOOTH_PRIVILEGED}, 0x12345);
                } catch (Exception ignored) {
                }
                if (bluetoothPrivileged) {
                    Toast.makeText(this, "Permissões Necessárias Concedidas!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_USE_COARS_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "Permissão Negada! :(", Toast.LENGTH_SHORT).show();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }
}