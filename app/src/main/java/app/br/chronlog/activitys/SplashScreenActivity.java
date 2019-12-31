package app.br.chronlog.activitys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import app.br.chronlog.R;

import static app.br.chronlog.utils.Utils.CONFIG_FILE;
import static app.br.chronlog.utils.Utils.startActivityWithExplosion;
import static java.lang.Thread.sleep;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean verificado = getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE).getBoolean("aparelho_verificado", false);
        new Thread(() -> {
            try {
                sleep(1500);
                if (!verificado) {
                    //cria intent para prosseguir mas não executa ate que clique no botao
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    runOnUiThread(() -> startActivityWithExplosion(this, intent));
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    runOnUiThread(() -> startActivityWithExplosion(this, intent));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
