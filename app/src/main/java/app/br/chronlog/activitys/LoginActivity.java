package app.br.chronlog.activitys;


import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import app.br.chronlog.R;

import static app.br.chronlog.utils.Utils.CONFIG_FILE;
import static app.br.chronlog.utils.Utils.destroyDialog;
import static app.br.chronlog.utils.Utils.startActivityWithExplosion;

public class LoginActivity extends AppCompatActivity {
    AlertDialog alert;
    SharedPreferences sharedPreferences;
    private View dialogLoginLogista;

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

            findViewById(R.id.serial_input).setVisibility(View.GONE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            new Thread(() -> {
                final TextView[] status = new TextView[1];
                try {
                    Thread.sleep(500);
                    runOnUiThread(() -> {
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        status[0] = findViewById(R.id.status);
                        status[0].setVisibility(View.VISIBLE);
                        status[0].setText(R.string.serial_configurado);
                    });

                    Thread.sleep(750);

                    runOnUiThread(() -> {
                        runOnUiThread(() -> status[0].setVisibility(View.INVISIBLE));
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
}