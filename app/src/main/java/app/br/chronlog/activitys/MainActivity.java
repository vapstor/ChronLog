package app.br.chronlog.activitys;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import app.br.chronlog.R;
import app.br.chronlog.utils.bluetooth.BluetoothController;

import static com.github.mikephil.charting.charts.Chart.LOG_TAG;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    public static BluetoothController universalBtController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new MainFragment(), "main").commit();
        else
            onBackStackChanged();
        universalBtController = new BluetoothController(this);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }


    @Override
    public void onBackStackChanged() {
        Log.d(LOG_TAG, "onBackStackChanged + MainActivity");
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
//        if (fragment instanceof DevicesFragment) {
//            ((DevicesFragment) fragment).
//        }
        super.onAttachFragment(fragment);
    }
}
