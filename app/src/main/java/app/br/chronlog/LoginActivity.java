package app.br.chronlog;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import static app.br.chronlog.utils.Utils.CONFIG_FILE;
import static app.br.chronlog.utils.Utils.destroyDialog;
import static app.br.chronlog.utils.Utils.startActivityWithExplosion;

public class LoginActivity extends AppCompatActivity {
    AlertDialog alert;
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        boolean verificado = sharedPreferences.getBoolean("aparelho_verificado", false);
        if(verificado) {
            Toast.makeText(this, "Bem vindo novamente!", Toast.LENGTH_SHORT).show();
            overridePendingTransition(0,0);
            startActivity(new Intent(this, ChartViewActivity.class));
        } else {
            Toast.makeText(this, "Aparelho não verificado!", Toast.LENGTH_SHORT).show();
            setContentView(R.layout.activity_login);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);


            TextView serialTV = findViewById(R.id.serial_input);
            Button btnLogin = findViewById(R.id.login);

            TextWatcher tw = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String serial = serialTV.getText().toString();
                    if(!serial.equals("")) {
                        btnLogin.setEnabled(true);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            };

            serialTV.addTextChangedListener(tw);

            btnLogin.setOnClickListener((v) -> {
                sharedPreferences.edit().putBoolean("aparelho_verificado", true).apply();
                Toast.makeText(this, "Configurando aparelho...", Toast.LENGTH_SHORT).show();
//                alert = createLoadingDialog(this, "Efetuando Login", "Por Favor aguarde enquanto efetuamos a autenticação!");
//                alert.show();
                findViewById(R.id.serial_input).setVisibility(View.INVISIBLE);
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                new Thread(() -> {
                    try {
                        Thread.sleep(1200);
                        runOnUiThread(() -> {
                            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                            TextView status = findViewById(R.id.status);
                            status.setVisibility(View.VISIBLE);
                            status.setText(R.string.serial_configurado);
                        });
                        Thread.sleep(500);
                        Intent intent = new Intent(this, ChartViewActivity.class);

                        runOnUiThread(() -> {
//                            overridePendingTransition(0, 0);
                            startActivityWithExplosion(this, intent);
//                            destroyDialog(alert);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            });
        }



    }

    @Override
    protected void onDestroy() {
        destroyDialog(alert);
        super.onDestroy();
    }
}