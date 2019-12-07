package app.br.chronlog.utils.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;

import app.br.chronlog.R;
import app.br.chronlog.activitys.DevicesActivity;
import app.br.chronlog.activitys.MainActivity;
import app.br.chronlog.utils.Utils;

import static app.br.chronlog.activitys.DevicesActivity.deviceName;
import static app.br.chronlog.activitys.DevicesActivity.devicesList;
import static app.br.chronlog.activitys.DevicesActivity.listAdapter;
import static app.br.chronlog.utils.Utils.TAG_LOG;
import static app.br.chronlog.utils.Utils.hideProgressBar;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.setStatus;
import static app.br.chronlog.utils.Utils.showProgressBar;

public class BluetoothController {
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Activity activity;
    private ProgressBar progressBar;
    public SerialSocket socket;

    private static final int REQUEST_USE_BLUETOOTH = 1;
    private static final int REQUEST_USE_COARS_LOCATION = 2;

    private IntentFilter filter;
    private CountDownTimer countDownTimer;
    private Thread searchDevices;
    private TextView statusView;
    public static boolean recebido;

    public void setActivity(Activity activity) {
        this.activity = activity;
        refreshReferenceToAppBar();
    }

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
                            if (!devicesList.contains(device)) {
                                listAdapter.add(device);
                                listAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    try {
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234);
                        //the pin in case you need to accept for an specific pin
                        Log.d(TAG_LOG, "Start Auto Pairing. PIN = " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234));
                        byte[] pinBytes;
                        pinBytes = ("" + pin).getBytes(StandardCharsets.UTF_8);
                        assert device != null;
                        device.setPin(pinBytes);
                        //setPairing confirmation if neeeded
                        device.setPairingConfirmation(true);
                    } catch (Exception e) {
                        Log.e(TAG_LOG, "Error occurs when trying to auto pair");
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
                            setStatus("Não Conectado", activity);
                            if (activity != null) {
                                if (activity instanceof DevicesActivity) {
                                    ((DevicesActivity) activity).setEmptyText("");
                                    devicesList.clear();
                                    listAdapter.notifyDataSetChanged();
                                    ((DevicesActivity) activity).setEmptyText("Nenhum dispositivo encontrado. :(" + "\n" + " Tente novamente");

                                } else {
                                    activity.startActivity(new Intent(activity, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }

                                if (activity instanceof MainActivity) {
                                    isDeviceConnected = Utils.Connected.False;
                                    ((MainActivity) activity).setButtonsEnabledDisabled();
                                }
                                break;
                            }
                        case BluetoothAdapter.STATE_ON:
                            Toast.makeText(context, "Bluetooth Ativado!", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothAdapter.STATE_CONNECTED:
                            Toast.makeText(context, "Bluetooth Conectado!", Toast.LENGTH_SHORT).show();
                            hideProgressBar(activity);
                            if (activity != null) {
                                if (activity instanceof DevicesActivity) {
                                    ((DevicesActivity) activity).setEmptyText("");
                                    devicesList.clear();
                                    listAdapter.notifyDataSetChanged();
                                    ((DevicesActivity) activity).setEmptyText("Nenhum dispositivo encontrado. :(" + "\n" + " Tente novamente");
                                } else {
                                    activity.startActivity(new Intent(activity, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }
                            }
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTED:
                            Toast.makeText(context, "Bluetooth Desconectado!", Toast.LENGTH_LONG).show();
                            if (activity != null) {
                                if (activity instanceof DevicesActivity) {
                                    devicesList.clear();
                                    listAdapter.notifyDataSetChanged();
                                    ((DevicesActivity) activity).setEmptyText("Habilite o Bluetooth, Por Favor.");
                                } else {
                                    activity.startActivity(new Intent(activity, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }
                            }
                            setStatus("", activity);
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    if (activity != null && activity instanceof DevicesActivity) {
                        ((DevicesActivity) activity).setEmptyText("");
                    }
                    actionDiscoverStarted();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    hideProgressBar(activity);
                    if (activity != null && activity instanceof DevicesActivity) {
                        if (devicesList.size() == 0) {
                            ((DevicesActivity) activity).setEmptyText("Nenhum Dispositivo Encontrado. :( " + "\n" + "Tente Novamente.");
                        }
                    }
                    if (statusView.getText().toString().equals("conectando...")) {

                    } else if (deviceName != null) {
                        statusView.getText().toString().equals(deviceName);
                    } else {
                        setStatus("", activity);
                    }
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    setStatus("", activity);
                    break;

            }
        }
    };

    public BluetoothController(Activity activity) {
        if (bluetoothAdapter != null) {
            this.activity = activity;
            refreshReferenceToAppBar();
            createFilters();
//            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
//            ActivityCompat.requestPermissions(activity,
//                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
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

        }

    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    private void refreshReferenceToAppBar() {
        statusView = activity.findViewById(R.id.status);
        progressBar = activity.findViewById(R.id.progressBarAppBar);
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


    private void actionDiscoverStarted() {
        setStatus("procurando...", activity);
        showProgressBar(activity);
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

//
//    public void send(String str) {
//        Log.d(TAG_LOG, "String a enviar para termopar: " + str);
//        if (connected != Connected.True) {
//            activity.runOnUiThread(() -> Toast.makeText(activity, "Não Conectado", Toast.LENGTH_SHORT).show());
//            return;
//        }
//        try {
//            byte[] data = (str).getBytes();
//            socket.write(data);
//        } catch (Exception e) {
//            e.printStackTrace();
//            onSerialIoError(e);
//        }
//    }
//
//    public void receive(byte[] data) {
//        if (data != null) {
//            recebido = true;
//        }
//        String messageReceived = new String(data);
//        Log.d(TAG_LOG, "receive: " + messageReceived);
//    }

//        @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        if (requestCode == REQUEST_USE_BLUETOOTH || requestCode == REQUEST_USE_COARS_LOCATION) {
//            // If request is cancelled, the result arrays are empty.
//            if (grantResults.length > 0
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Register for broadcasts when a device is discovered.
//                createFilters();
//            } else {
//                Toast.makeText(this, "Permissão Negada! :(", Toast.LENGTH_SHORT).show();
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
        activity.registerReceiver(receiver, filter);
//        }
    }


    public Activity getRunningActivity() {
        return activity;
    }
}
