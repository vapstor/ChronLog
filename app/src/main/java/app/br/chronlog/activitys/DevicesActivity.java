package app.br.chronlog.activitys;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import app.br.chronlog.R;
import app.br.chronlog.utils.Utils.Connected;
import app.br.chronlog.utils.bluetooth.BluetoothController;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;
import app.br.chronlog.utils.bluetooth.SerialSocket;

import static app.br.chronlog.utils.Utils.TAG_LOG;
import static app.br.chronlog.utils.Utils.bluetoothDeviceSelected;
import static app.br.chronlog.utils.Utils.hideProgressBar;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.myBluetoothController;
import static app.br.chronlog.utils.Utils.send;
import static app.br.chronlog.utils.Utils.serialSocket;
import static app.br.chronlog.utils.Utils.setStatus;
import static app.br.chronlog.utils.Utils.showProgressBar;
import static app.br.chronlog.utils.bluetooth.Constants.CONECTANDO_;
import static app.br.chronlog.utils.bluetooth.Constants.CONEXAO_FALHOU;
import static com.github.mikephil.charting.charts.Chart.LOG_TAG;
import static java.lang.Thread.sleep;

public class DevicesActivity extends AppCompatActivity implements ServiceConnection, SerialListener {
    private ProgressBar progressBar;
    private ImageButton refreshButton;

    private TextView statusView;
    public static String deviceAddress, deviceName;

    private View appBarView;

    public static ArrayAdapter<BluetoothDevice> listAdapter;
    public static ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private BluetoothDevice deviceSelected;
    private ListView listDevicesView;
    private TextView listEmptyView;

    private SerialService service;
    private static String receivedData;
    private final Object lock = new Object();
    private Thread sendCommandThread;
    public static String modelo;
    private Thread executeCommandThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        if (myBluetoothController == null) {
            myBluetoothController = new BluetoothController(this);
        } else {
            myBluetoothController.setActivity(this);
        }

        if (isDeviceConnected == Connected.True) {
            ((TextView) findViewById(R.id.status)).setText(deviceName);
        }

        listDevicesView = findViewById(R.id.listDevicesView);
        listEmptyView = findViewById(R.id.listEmpty);

        listAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, devicesList) {

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                BluetoothDevice device = devicesList.get(position);
                if (view == null)
                    view = getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                text1.setText(device.getName());
                text2.setText(device.getAddress());
                return view;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };

        listDevicesView.setOnItemClickListener((parent, view, position, id) -> {
            if (myBluetoothController.getBluetoothAdapter().isDiscovering()) {
                myBluetoothController.getBluetoothAdapter().cancelDiscovery();
            }
            deviceSelected = devicesList.get(position);
            pairDevice(deviceSelected);
        });
    }

    private void refreshReferencesToAppBarView() {
        //titulo
        Activity activity = myBluetoothController.getRunningActivity();
        ((TextView) activity.findViewById(R.id.titleBar)).setText(R.string.dispositivos);
        progressBar = activity.findViewById(R.id.progressBarAppBar);
        statusView = activity.findViewById(R.id.status);
        refreshButton = activity.findViewById(R.id.iconBar);
        boolean visivel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            visivel = progressBar.isAnimating();
        } else {
            visivel = progressBar.getVisibility() == View.VISIBLE;
        }
        if (visivel) {
            refreshButton.setEnabled(true);
            refreshButton.setImageDrawable(this.getDrawable(R.drawable.baseline_sync_white_18dp));
        } else {
            refreshButton.setEnabled(false);
            refreshButton.setImageDrawable(this.getDrawable(R.drawable.baseline_sync_black_18));
        }
    }

    @Override
    protected void onStart() {
        if (myBluetoothController == null) {
            myBluetoothController = new BluetoothController(this);
        } else {
            myBluetoothController.setActivity(this);
        }
        refreshReferencesToAppBarView();
        if (service != null) {
            service.attach(this);
        } else {
            bindService(new Intent(this, SerialService.class), this, 0);
        }
        listDevicesView.setAdapter(null);
        listDevicesView.setAdapter(listAdapter);
        super.onStart();
    }

    private void setIconToRefreshBluetooth() {
        refreshButton = findViewById(R.id.iconBar);
        refreshButton.setEnabled(true);
        refreshButton.setImageDrawable(getDrawable(R.drawable.baseline_sync_white_18dp));
        refreshButton.setOnClickListener(v -> refresh());
    }

    private void refresh() {
        checkDevicesBonded();
        myBluetoothController.searchDevices();
    }

    private void checkDevicesBonded() {
        if (myBluetoothController.getBluetoothAdapter() != null) {
            devicesList.clear();
            for (BluetoothDevice device : myBluetoothController.getBluetoothAdapter().getBondedDevices())
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    devicesList.add(device);
            Collections.sort(devicesList, DevicesActivity::compareTo);
            listAdapter.notifyDataSetChanged();
        }
    }


    public void btOn() {
        //refreshButton.setImageDrawable(Objects.requireNonNull(this).getDrawable(R.drawable.baseline_bluetooth_searching_white_18dp));
        refreshButton.setImageDrawable(this.getDrawable(R.drawable.baseline_sync_white_18dp));
        this.findViewById(R.id.iconBar).setEnabled(true);
        try {
            sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (myBluetoothController != null && !myBluetoothController.getBluetoothAdapter().isDiscovering()) {
            if (devicesList.size() < 1) {
                setEmptyText("Nenhum Dispositivo Encontrado. :( " + "\n" + "Tente Novamente.");
            } else {
                setEmptyText("");
            }
        } else {
            setEmptyText("");
        }
    }

    public void setEmptyText(String empty) {
        listEmptyView.setText(empty);
    }

    public void btOff() {
        setEmptyText("Habilite o Bluetooth, Por Favor.");
        //refreshButton.setImageDrawable(Objects.requireNonNull(this).getDrawable(R.drawable.baseline_bluetooth_disabled_black_18dp));
        refreshButton.setImageDrawable(this.getDrawable(R.drawable.baseline_bluetooth_disabled_black_18dp));
        this.findViewById(R.id.iconBar).setEnabled(false);
    }

    private void setIconToHome() {
        ImageButton homeButton = findViewById(R.id.iconBar);
        homeButton.setEnabled(true);
        homeButton.setImageDrawable(Objects.requireNonNull(this).getDrawable(R.drawable.baseline_home_24));
        homeButton.setOnClickListener((v) -> startActivity(new Intent(this, MainActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (myBluetoothController.getBluetoothAdapter() == null) {
            setIconToHome();
            setEmptyText("Bluetooh Não Suportado!");
        } else {
            myBluetoothController.setActivity(this);
            refreshReferencesToAppBarView();
            setIconToRefreshBluetooth();
            if (!myBluetoothController.getBluetoothAdapter().isEnabled()) {
                btOff();
            } else {
                btOn();
                //começa a busca
                refresh();
            }
        }
    }

    private void showExplanation(Activity activity, String title, String message, final String permission, final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> requestPermission(activity, permission, permissionRequestCode));
        builder.create().show();
    }


    private void requestPermission(Activity activity, String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{permissionName}, permissionRequestCode);
    }

    public void pairDevice(BluetoothDevice deviceSelected) {
        try {
            if (myBluetoothController.getBluetoothAdapter() != null) {
                if (myBluetoothController.getBluetoothAdapter().isEnabled()) {
                    if (isDeviceConnected == Connected.Pending) {
                        Toast.makeText(this, "Aguarde...", Toast.LENGTH_SHORT).show();
                    } else {
                        deviceAddress = deviceSelected.getAddress();
                        deviceName = deviceSelected.getName();
                        bluetoothDeviceSelected = myBluetoothController.getBluetoothAdapter().getRemoteDevice(deviceAddress);
                        if (serialSocket == null) {
                            serialSocket = new SerialSocket();
                        }
                        if (isDeviceConnected == Connected.False) {
                            showProgressBar(this);
                            setStatus(CONECTANDO_, this);
                            isDeviceConnected = Connected.Pending;
                        }
                        service.connect(this, "Conectado a " + deviceName);
                        serialSocket.connect(this, service, bluetoothDeviceSelected);

                    }
                } else {
                    Toast.makeText(this, "Bluetooth Desabilitado!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // If you want to auto-input the pin#:
        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(requestCode)) {
            deviceSelected.setPin("1234".getBytes());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * sort by name, then address. sort named devices first
     */
    private static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        boolean aValid = a.getName() != null && !a.getName().isEmpty();
        boolean bValid = b.getName() != null && !b.getName().isEmpty();
        if (aValid && bValid) {
            int ret = a.getName().compareTo(b.getName());
            if (ret != 0) return ret;
            return a.getAddress().compareTo(b.getAddress());
        }
        if (aValid) return -1;
        if (bValid) return +1;
        return a.getAddress().compareTo(b.getAddress());
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
        if (myBluetoothController.getBluetoothAdapter() != null && myBluetoothController.getBluetoothAdapter().isDiscovering()) {
            myBluetoothController.getBluetoothAdapter().cancelDiscovery();
        }
//        if (isDeviceConnected != Connected.False)
//            disconnect();
//        this.stopService(new Intent(this, SerialService.class));
    }

    private void disconnect() {
        isDeviceConnected = Connected.False;
        service.disconnect();
        serialSocket.disconnect();
        serialSocket = null;
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        isDeviceConnected = Connected.True;
        runOnUiThread(() -> {
            setStatus(deviceName, this);
            Log.d(LOG_TAG, "Conectado com sucesso!");
        });
        readDeviceModel();
    }

    @Override
    public void onSerialConnectError(Exception e) {
        hideProgressBar(this);
        if (!Objects.requireNonNull(e.getMessage()).contains("ja conectado")) {
            setStatus(CONEXAO_FALHOU, this);
            Toast.makeText(this, "Tente Novamente!", Toast.LENGTH_SHORT).show();
            disconnect();
        } else {
            if (deviceName != null) {
                setStatus(deviceName, this);
            }
            Toast.makeText(this, "Já Conectado!", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onSerialRead(byte[] data) {
        synchronized (lock) {
            String receveidStr = new String(data);
            Log.d(TAG_LOG, "recebeu: " + receveidStr);
            receivedData = receivedData.concat(receveidStr);
            if (receivedData.contains("@0D")) {
                Log.d(TAG_LOG, "VALOR RECEBIDO : " + receveidStr);
                Log.d(TAG_LOG, "RECEIVED DATA : " + receivedData);
                //returns model like: "@0DEEEEEEEE"
                modelo = receivedData.replace("@0D", "").replace("\r\n", "").trim();
                lock.notify();
            }
        }
    }

    private void readDeviceModel() {
        receivedData = "";

        String protocolReadDeviceModel = "@0DRRRRRRRRCRLF";
        if (sendCommandThread != null) {
            if (sendCommandThread.isAlive()) {
                sendCommandThread.interrupt();
            }
            sendCommandThread = null;
        }


        if (executeCommandThread != null) {
            if (executeCommandThread.isAlive()) {
                executeCommandThread.interrupt();
            }
            executeCommandThread = null;
        }


        sendCommandThread = new Thread(() -> send(protocolReadDeviceModel, this, this));
        sendCommandThread.start();

        executeCommandThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(500);
                    if (receivedData.equals("")) {
                        readDeviceModel();
                    } else {
                        runOnUiThread(() -> {
                            hideProgressBar(this);
                            Toast.makeText(getApplicationContext(), "Conectado com sucesso! (" + modelo + ")", Toast.LENGTH_LONG).show();
                            super.onBackPressed();
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        executeCommandThread.start();
    }

    @Override
    public void onSerialIoError(Exception e) {
        hideProgressBar(this);
        setStatus("Conexão Perdida", this);
        disconnect();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }


    @Override
    public void onBackPressed() {
        if (isDeviceConnected == Connected.Pending) {
            Toast.makeText(this, "Aguarde a conexão...", Toast.LENGTH_SHORT).show();
        } else if (modelo == null || modelo.equals("")) {
            Toast.makeText(this, "Aguarde a configuração do modelo...", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}
