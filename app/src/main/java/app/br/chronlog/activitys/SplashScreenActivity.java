package app.br.chronlog.activitys;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import app.br.chronlog.R;

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
        new Thread(() -> {
            try {
                sleep(1500);
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                runOnUiThread(() -> startActivityWithExplosion(this, intent));

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
