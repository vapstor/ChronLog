package app.br.chronlog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import static app.br.chronlog.utils.Utils.CONFIG_FILE;
import static app.br.chronlog.utils.Utils.createDialog;
import static app.br.chronlog.utils.Utils.destroyDialog;
import static app.br.chronlog.utils.Utils.startActivityWithExplosion;
import static java.lang.Thread.sleep;

public class SplashScreenActivity extends AppCompatActivity {
    AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
    }

    @Override
    protected void onResume() {
        SharedPreferences sharedPreferences = getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        boolean unlocked = sharedPreferences.getBoolean("aparelho_verificado", false);
        new Thread(() -> {
            try {
                sleep(1500);
                if(!unlocked) {
                    //cria intent para prosseguir mas não executa ate que clique no botao
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    //seta botao positivo
                    DialogInterface.OnClickListener positiveListener = (dialog, which) -> startActivityWithExplosion(this, intent);

                    //cria dialogo
                    runOnUiThread(() -> {
                        alert = createDialog(this, "Olá!", "Bem vindo ao seu mais novo aplicativo!", "Avançar", "", false, false, null, positiveListener, null);
                        alert.show();
                    });
                } else {
                    runOnUiThread(()-> {
                        Intent intent = new Intent(this, ChartViewActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivityWithExplosion(this, intent);
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        destroyDialog(alert);
        super.onDestroy();
    }
}
