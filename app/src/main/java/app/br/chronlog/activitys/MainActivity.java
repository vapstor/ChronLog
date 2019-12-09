package app.br.chronlog.activitys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import app.br.chronlog.R;
import app.br.chronlog.utils.Utils.Connected;
import app.br.chronlog.utils.bluetooth.BluetoothController;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;

import static app.br.chronlog.activitys.DevicesActivity.deviceName;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.myBluetoothController;
import static app.br.chronlog.utils.Utils.setStatus;
import static app.br.chronlog.utils.Utils.startActivityWithExplosion;
import static app.br.chronlog.utils.bluetooth.Constants.CONECTANDO_;
import static app.br.chronlog.utils.bluetooth.Constants.CONEXAO_FALHOU;
import static app.br.chronlog.utils.bluetooth.Constants.CONEXAO_PERDIDA;
import static app.br.chronlog.utils.bluetooth.Constants.JA_CONECTADO;


public class MainActivity extends AppCompatActivity implements ServiceConnection, SerialListener, View.OnClickListener {
    private ProgressBar progressBar;
    private ImageButton syncButton;
    private Button configuraDeviceBtn, analisaDadosBtn, eepromBtn, gerenciarDadosBtn;
    private TextView statusView;

    private SerialService service;

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

    }

    public void setButtonsEnabledDisabled() {
        getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
        syncButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        if (isDeviceConnected == null) isDeviceConnected = Connected.False;

        if (isDeviceConnected == Connected.False) {
            if (!statusView.getText().equals(CONEXAO_FALHOU) && !statusView.getText().equals(CONEXAO_PERDIDA))
                statusView.setText("");
            progressBar.setVisibility(View.GONE);
            configuraDeviceBtn.setEnabled(false);
            gerenciarDadosBtn.setEnabled(false);
            eepromBtn.setEnabled(false);
            configuraDeviceBtn.setText(R.string.configurar);
            gerenciarDadosBtn.setText(R.string.gerenciar_dados);
            eepromBtn.setText(R.string.eeprom);
        } else if (isDeviceConnected == Connected.Pending) {
            statusView.setText(CONECTANDO_);
            progressBar.setVisibility(View.VISIBLE);

            configuraDeviceBtn.setEnabled(false);
            gerenciarDadosBtn.setEnabled(false);
            eepromBtn.setEnabled(false);
            configuraDeviceBtn.setText(R.string.aguarde);
            gerenciarDadosBtn.setText(R.string.aguarde);
            eepromBtn.setText(R.string.aguarde);
        } else {
            statusView.setText(deviceName);
            progressBar.setVisibility(View.GONE);
            configuraDeviceBtn.setText(R.string.configurar);
            gerenciarDadosBtn.setText(R.string.gerenciar_dados);
            eepromBtn.setText(R.string.eeprom);
            configuraDeviceBtn.setEnabled(true);
            gerenciarDadosBtn.setEnabled(true);
            eepromBtn.setEnabled(true);
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
                startActivity(new Intent(this, ReadDataActivity.class));
                break;
            case R.id.analiseDeDadosBtn:
                startActivityWithExplosion(this, new Intent(this, ChartViewActivity.class));
                break;
            case R.id.eeppromBtn:
                Toast.makeText(this, "EEPROM [?]", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "erro", Toast.LENGTH_SHORT).show();
                break;
        }
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
        eepromBtn = findViewById(R.id.eeppromBtn);

        syncButton.setOnClickListener(this);
        configuraDeviceBtn.setOnClickListener(this);
        gerenciarDadosBtn.setOnClickListener(this);
        analisaDadosBtn.setOnClickListener(this);
        eepromBtn.setOnClickListener(this);
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
    protected void onDestroy() {
        super.onDestroy();
//        if (deviceIsConnected != Connected.False)
//            disconnect();
//        this.stopService(new Intent(this, SerialService.class));
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
        isDeviceConnected = Connected.False;
        if (Objects.requireNonNull(e.getMessage()).contains(JA_CONECTADO)) {
            runOnUiThread(() -> Toast.makeText(this, "Já conectado!", Toast.LENGTH_SHORT).show());
        } else {
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
