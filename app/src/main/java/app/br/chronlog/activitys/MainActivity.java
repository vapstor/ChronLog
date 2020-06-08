package app.br.chronlog.activitys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import app.br.chronlog.R;
import app.br.chronlog.utils.Utils.Connected;
import app.br.chronlog.utils.bluetooth.BluetoothController;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static app.br.chronlog.BuildConfig.VERSION_NAME;
import static app.br.chronlog.activitys.DevicesActivity.deviceName;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.mLastClickTime;
import static app.br.chronlog.utils.Utils.myBluetoothController;
import static app.br.chronlog.utils.Utils.serialSocket;
import static app.br.chronlog.utils.Utils.setStatus;
import static app.br.chronlog.utils.bluetooth.Constants.CONECTANDO_;
import static app.br.chronlog.utils.bluetooth.Constants.CONEXAO_FALHOU;
import static app.br.chronlog.utils.bluetooth.Constants.CONEXAO_PERDIDA;
import static app.br.chronlog.utils.bluetooth.Constants.JA_CONECTADO;

public class MainActivity extends AppCompatActivity implements ServiceConnection, SerialListener, OnClickListener {
    private ProgressBar progressBar;
    private ImageButton syncButton;
    private Button configuraDeviceBtn, analisaDadosBtn, gerenciarDadosBtn;
    private TextView statusView;
    private SerialService service;
    private Button sobreBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (myBluetoothController == null) {
            myBluetoothController = new BluetoothController(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, SerialService.class));
        } else {
            startService(new Intent(this, SerialService.class));
        }

        overridePendingTransition(android.R.animator.fade_in, android.R.animator.fade_out);

        findViewById(R.id.titleBar).setOnClickListener(v -> Toast.makeText(service, "Chronlog v" + VERSION_NAME, Toast.LENGTH_SHORT).show());

        sobreBtn = findViewById(R.id.sobreBtn);
        sobreBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                    .setView(R.layout.dialog_sobre)
                    .setPositiveButton("Fechar", (dialog, which) -> {
                    });
            builder.create().show();
        });
    }

    public void setButtonsEnabledDisabled() {
        getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
        syncButton.setEnabled(true);
        progressBar.setVisibility(GONE);

        if (isDeviceConnected == null) isDeviceConnected = Connected.False;

        if (isDeviceConnected == Connected.False) {
            if (!statusView.getText().equals(CONEXAO_FALHOU) && !statusView.getText().equals(CONEXAO_PERDIDA))
                statusView.setText("");
            progressBar.setVisibility(GONE);
            configuraDeviceBtn.setEnabled(false);
            gerenciarDadosBtn.setEnabled(false);
            configuraDeviceBtn.setText(R.string.configurar);
            gerenciarDadosBtn.setText(R.string.gerenciar_dados);
        } else if (isDeviceConnected == Connected.Pending) {
            statusView.setText(CONECTANDO_);
            progressBar.setVisibility(VISIBLE);
            configuraDeviceBtn.setEnabled(false);
            gerenciarDadosBtn.setEnabled(false);
            configuraDeviceBtn.setText(R.string.aguarde);
            gerenciarDadosBtn.setText(R.string.aguarde);
        } else {
            statusView.setText(deviceName);
            progressBar.setVisibility(GONE);
            configuraDeviceBtn.setText(R.string.configurar);
            gerenciarDadosBtn.setText(R.string.gerenciar_dados);
            configuraDeviceBtn.setEnabled(true);
            gerenciarDadosBtn.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setButtonsEnabledDisabled();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.configDeviceBtn:
                startActivity(new Intent(this, ConfigDeviceActivity.class));
                break;
            case R.id.iconBar:
                startActivity(new Intent(this, DevicesActivity.class));
                break;
            case R.id.gerenciarDadosBtn:
                startActivity(new Intent(this, ReadTermoparDataActivity.class));
                break;
            case R.id.analiseDeDadosBtn:
                final CharSequence[] items = {"CTL0104A", "CTL0104B", "CVL0101A", "CVL0102A"};

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Selecione o modelo");
                builder.setItems(items, (dialog, item) -> {
                    startActivity(new Intent(this, ReadSdDataActivity.class).putExtra("modelo", items[item]));
                });
                AlertDialog alert = builder.create();
                alert.show();
                break;
            default:
                Toast.makeText(this, "erro", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void openSavedFiles() {

    }

    public boolean saveImageOnExternalData(String filePath, byte[] fileData) {

        boolean isFileSaved = false;
        try {
            File f = new File(filePath);
            if (f.exists())
                f.delete();
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(fileData);
            fos.flush();
            fos.close();
            isFileSaved = true;
            // File Saved
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOException");
            e.printStackTrace();
        }
        return isFileSaved;
        // File Not Saved
    }

    @Override
    protected void onStart() {
        super.onStart();
        myBluetoothController.setActivity(this);
        if (service != null) {
            service.attach(this);
        } else {
            bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
        }

        instaceItensView();
        setButtonsEnabledDisabled();
    }

    private void instaceItensView() {
        ((TextView) findViewById(R.id.titleBar)).setText(R.string.chronlog);
        syncButton = findViewById(R.id.iconBar);
        syncButton.setImageDrawable(this.getDrawable(R.drawable.baseline_bluetooth_searching_white_18dp));

        progressBar = findViewById(R.id.progressBarAppBar);
        statusView = findViewById(R.id.status);

        configuraDeviceBtn = findViewById(R.id.configDeviceBtn);
        gerenciarDadosBtn = findViewById(R.id.gerenciarDadosBtn);
        analisaDadosBtn = findViewById(R.id.analiseDeDadosBtn);

        syncButton.setOnClickListener(this);
        configuraDeviceBtn.setOnClickListener(this);
        gerenciarDadosBtn.setOnClickListener(this);
        analisaDadosBtn.setOnClickListener(this);
    }

    @Override
    public void onStop() {
        try {
            this.unbindService(this);
        } catch (Exception ignored) {
        }
//        if (service != null && !this.isChangingConfigurations())
//            service.detach();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
            finishInstances();
        } else {
            Toast.makeText(this, "Toque mais uma vez para sair", Toast.LENGTH_SHORT).show();
        }
        mLastClickTime = SystemClock.elapsedRealtime();
    }

    private void finishInstances() {
        if (isDeviceConnected != Connected.False)
            if (serialSocket != null) {
                serialSocket.disconnect();
            }
        this.stopService(new Intent(this, SerialService.class));
        finishAffinity();
        System.exit(0);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        isDeviceConnected = Connected.True;
        runOnUiThread(() -> Toast.makeText(this, "Conectado com sucesso!", Toast.LENGTH_SHORT).show());
        if (deviceName != null) {
            runOnUiThread(() -> setStatus(deviceName, this));
        } else {
            runOnUiThread(() -> setStatus("conectado", this));
        }
        runOnUiThread(this::setButtonsEnabledDisabled);
    }

    @Override
    public void onSerialConnectError(Exception e) {
        if (Objects.requireNonNull(e.getMessage()).contains(JA_CONECTADO)) {
            runOnUiThread(() -> Toast.makeText(this, "Já conectado!", Toast.LENGTH_SHORT).show());
        } else {
            isDeviceConnected = Connected.False;
            runOnUiThread(() -> setStatus(CONEXAO_FALHOU, this));
        }
        runOnUiThread(this::setButtonsEnabledDisabled);
    }

    @Override
    public void onSerialRead(byte[] data) {
//        runOnUiThread(() -> Toast.makeText(this, "estou aqui", Toast.LENGTH_SHORT).show());
        //receive();
    }

    @Override
    public void onSerialIoError(Exception e) {
        runOnUiThread(() -> {
            isDeviceConnected = Connected.False;
            Toast.makeText(this, "A conexão foi perdida!", Toast.LENGTH_SHORT).show();
            setStatus(CONEXAO_PERDIDA, this);
            setButtonsEnabledDisabled();
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }
}
