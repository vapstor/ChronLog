package app.br.chronlog.utils.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import app.br.chronlog.R;

import static android.view.View.GONE;
import static app.br.chronlog.activitys.ConfigDeviceFragment.mySendThread;
import static app.br.chronlog.activitys.DevicesFragment.devicesList;
import static app.br.chronlog.activitys.DevicesFragment.listAdapter;
import static com.github.mikephil.charting.charts.Chart.LOG_TAG;

public class BluetoothController implements ServiceConnection, SerialListener {
    private final BluetoothAdapter bluetoothAdapter;
    private final View appBarView;
    private final Activity activity;
    private final ProgressBar progressBar;
    public SerialSocket socket;
    public SerialService service;

    private static final int REQUEST_USE_BLUETOOTH = 1;
    private static final int REQUEST_USE_COARS_LOCATION = 2;

    private IntentFilter filter;
    private CountDownTimer countDownTimer;
    private Thread searchDevices;
    private TextView statusView;
    private String deviceName, deviceAddress;
    public static int mySendFlag = 0;

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public void searchDevices() {
        if (bluetoothAdapter != null) {
            try {
                if (searchDevices != null) {
                    if (searchDevices.isAlive()) {
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        if (bluetoothAdapter.isDiscovering()) {
                            bluetoothAdapter.cancelDiscovery();
                        }
                        searchDevices.interrupt();
                    }
                }
                searchDevices = new Thread(() -> {
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    bluetoothAdapter.startDiscovery();
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    countDownTimer = new CountDownTimer(5000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            bluetoothAdapter.cancelDiscovery();
                        }
                    };
                    countDownTimer.start();
//            } else{
//                getActivity().runOnUiThread(() -> ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_USE_BLUETOOTH));
//            }
                });
                searchDevices.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //    private boolean initialStart = true;
    public enum Connected {False, Pending, True}

    public Connected connected = Connected.False;


    // Create a BroadcastReceiver for BLUETOOTH_CHANGES.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device;
            assert action != null;
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //String deviceName = device.getName();
                    //String deviceHardwareAddress = device.getAddress(); // MAC address
                    if (devicesList != null) {
                        if (devicesList.size() == 0) {
                            listAdapter.add(device);
                            listAdapter.notifyDataSetChanged();
                        } else {
                            for (int i = 0; i < devicesList.size(); i++) {
                                if (!devicesList.get(i).getAddress().equals(device.getAddress())) {
                                    listAdapter.add(device);
                                    listAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                    break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    try {
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234);
                        //the pin in case you need to accept for an specific pin
                        Log.d(LOG_TAG, "Start Auto Pairing. PIN = " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234));
                        byte[] pinBytes;
                        pinBytes = ("" + pin).getBytes(StandardCharsets.UTF_8);
                        assert device != null;
                        device.setPin(pinBytes);
                        //setPairing confirmation if neeeded
                        device.setPairingConfirmation(true);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error occurs when trying to auto pair");
                        e.printStackTrace();
                    }
                    break;
                //                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
//                    //checa a cada 5s se ainda esta ficando distante
//                    if (SystemClock.elapsedRealtime() - passedTime < 5000) {
//                        Toast.makeText(context, "Sinal de Bluetooth Fraco...", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                    passedTime = SystemClock.elapsedRealtime();
//                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            Toast.makeText(context, "Bluetooth Desativado!", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Toast.makeText(context, "Bluetooth Ativado!", Toast.LENGTH_SHORT).show();
                            break;
                        //                        case BluetoothAdapter.STATE_CONNECTED:
//                            Toast.makeText(context, "Dispositivo Conectado!", Toast.LENGTH_SHORT).show();
//                            setStatus("conectado");
//                            hideProgressBar();
//                            break;
//                        case BluetoothAdapter.STATE_DISCONNECTED:
//                            Toast.makeText(context, "Dispositivo Desconectado!", Toast.LENGTH_LONG).show();
//                            setStatus("");
//                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    actionDiscoverStarted();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if (statusView.getText().toString().equals("conectando...")) {

                    } else {
                        setStatus("");
                        hideProgressBar();
                    }
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    setStatus("");
                    break;
            }
        }
    };

    private void actionDiscoverStarted() {
        setStatus("procurando...");
        showProgressBar();
    }

    public Connected getConnected() {
        return connected;
    }

    private void setStatus(String status) {
        statusView.setText(status);
    }

    private void showProgressBar() {
        if (progressBar.getVisibility() == GONE) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        if (progressBar.getVisibility() != GONE) {
            progressBar.setVisibility(GONE);
        }
    }

    public BluetoothController(Activity activity) {
        this.activity = activity;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            //            //busca dispositivos de primeira apenas na criação do fragmento
//            if (android.os.Build.VERSION.SDK_INT >= 23) {
//                // ANDROID 6.0 AND UP!
//                boolean accessCoarseLocationAllowed = false;
//                try {
//                    // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
//                    java.lang.reflect.Method methodCheckPermission = Activity.class.getMethod("checkSelfPermission", java.lang.String.class);
//                    Object resultObj = methodCheckPermission.invoke(this, Manifest.permission.ACCESS_COARSE_LOCATION);
//                    int result = Integer.parseInt(resultObj.toString());
//                    if (result == PackageManager.PERMISSION_GRANTED) {
//                        accessCoarseLocationAllowed = true;
//                    }
//                } catch (Exception ignored) {
//                }
//                if (accessCoarseLocationAllowed) {
//                    refresh();
//                }
//                try {
//                    // We have to invoke the method "void requestPermissions (Activity activity, String[] permissions, int requestCode) "
//                    // from android 6
//                    java.lang.reflect.Method methodRequestPermission = Activity.class.getMethod("requestPermissions", java.lang.String[].class, int.class);
//                    methodRequestPermission.invoke(this, new String[]
//                            {
//                                    Manifest.permission.ACCESS_COARSE_LOCATION
//                            }, 0x12345);
//                } catch (Exception ignored) {
//                }
//            }
            if (service != null)
                service.attach(this);
            else
                Objects.requireNonNull(activity).startService(new Intent(activity, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
            activity.bindService(new Intent(activity, SerialService.class), this, Context.BIND_AUTO_CREATE);
        }

        appBarView = activity.findViewById(R.id.appBar);
        statusView = appBarView.findViewById(R.id.status);
        progressBar = appBarView.findViewById(R.id.progressBarAppBar);
        createFilters();
    }


    public void pairDevice(BluetoothDevice deviceSelected) {
        try {
            deviceAddress = deviceSelected.getAddress();
            deviceName = deviceSelected.getName();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            deviceName = device.getName();
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Connected to " + deviceName);
            socket.connect(activity, service, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }


    private void showExplanation(String title, String message, final String permission, final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> requestPermission(permission, permissionRequestCode));
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{permissionName}, permissionRequestCode);
    }


    public void send(String str) {
        Log.d(LOG_TAG, "String a enviar para termopar: " + str);
        if (connected != Connected.True) {
            Toast.makeText(activity, "Não Conectado", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        synchronized (mySendThread) {
            if(mySendFlag == 1) { //o aparelho retorna 2 vezes 1: @ 2: 01 [0-1]
                mySendFlag = 0;
                mySendThread.notify(); //notifica para outro envio após os dois recebimentos
            }
            mySendFlag = 1;
            String messageReceived = new String(data);
            Log.d(LOG_TAG, "receive: "+messageReceived);
        }
        Toast.makeText(activity, "Ajustes no termopar efetuados com sucesso!", Toast.LENGTH_SHORT).show();
    }


    //    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        if (requestCode == REQUEST_USE_BLUETOOTH || requestCode == REQUEST_USE_COARS_LOCATION) {
//            // If request is cancelled, the result arrays are empty.
//            if (grantResults.length > 0
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Register for broadcasts when a device is discovered.
//                createFilters();
//            } else {
//                Toast.makeText(activity, "Permissão Negada! :(", Toast.LENGTH_SHORT).show();
//                // permission denied, boo! Disable the
//                // functionality that depends on this permission.
//            }
//        }
//    }

    private void createFilters() {
//        if (filter == null) {
        filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        Objects.requireNonNull(activity).registerReceiver(receiver, filter);
//        }
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        setStatus("conectado");
        hideProgressBar();
        connected = Connected.True;
        if (deviceName.length() > 10) {
            setStatus(deviceName.substring(0, 7) + "...");
        } else {
            setStatus(deviceName);
        }
        Toast.makeText(activity, "Aparelho conectado com sucesso!", Toast.LENGTH_SHORT).show();
//        String[] params = new String[]{deviceName};
//        gotToHome(params);
    }

    @Override
    public void onSerialConnectError(Exception e) {
        disconnect();
        setStatus("conexão falhou");
        hideProgressBar();
        e.printStackTrace();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        disconnect();
        setStatus("conexão perdida");
        hideProgressBar();
        Toast.makeText(activity, "O Aparelho Foi Desconectado!", Toast.LENGTH_LONG).show();
        e.getMessage();
    }

    public void disconnect() {
        setStatus("");
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

//    @Override
//    public void onDestroy() {
//        if (connected != Connected.False)
//            disconnect();
//        getActivity().stopService(new Intent(getActivity(), SerialService.class));
//        super.onDestroy();
//    }

}
