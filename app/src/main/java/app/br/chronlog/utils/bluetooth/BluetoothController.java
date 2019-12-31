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
import android.widget.ImageButton;
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
                                isDeviceConnected = Utils.Connected.False;
                                if (activity instanceof DevicesActivity) {
                                    ((DevicesActivity) activity).setEmptyText("");
                                    devicesList.clear();
                                    ((DevicesActivity) activity).setEmptyText("Habilite o Bluetooth, Por Favor.");
                                    listAdapter.notifyDataSetChanged();
                                    ((ImageButton) activity.findViewById(R.id.iconBar)).setImageDrawable(activity.getDrawable(R.drawable.baseline_bluetooth_disabled_black_18dp));

                                } else if (activity instanceof MainActivity) {
                                    ((MainActivity) activity).setButtonsEnabledDisabled();
                                } else {
                                    activity.startActivity(new Intent(activity, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }
                                break;
                            }
                        case BluetoothAdapter.STATE_ON:
                            Toast.makeText(context, "Bluetooth Ativado!", Toast.LENGTH_SHORT).show();
                            if (activity != null) {
                                if (activity instanceof DevicesActivity) {
                                    ((DevicesActivity) activity).setEmptyText("");
                                    ((ImageButton) activity.findViewById(R.id.iconBar)).setImageDrawable(activity.getDrawable(R.drawable.baseline_bluetooth_searching_white_18dp));
                                    devicesList.clear();
                                    ((DevicesActivity) activity).setEmptyText("Nenhum dispositivo encontrado. :(" + "\n" + " Tente novamente");
                                    listAdapter.notifyDataSetChanged();
                                } else {
                                    activity.startActivity(new Intent(activity, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                }
                            }
                            break;
                        case BluetoothAdapter.STATE_CONNECTED:
                            Toast.makeText(context, "Bluetooth Conectado!", Toast.LENGTH_SHORT).show();
                            hideProgressBar(activity);
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTED:
                            Toast.makeText(context, "Bluetooth Desconectado!", Toast.LENGTH_LONG).show();
                            if (activity != null) {
                                if (activity instanceof DevicesActivity) {
                                    devicesList.clear();
                                    ((DevicesActivity) activity).setEmptyText("Habilite o Bluetooth, Por Favor.");
                                    ((ImageButton) activity.findViewById(R.id.iconBar)).setImageDrawable(activity.getDrawable(R.drawable.baseline_bluetooth_disabled_black_18dp));
                                    listAdapter.notifyDataSetChanged();
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
                        if (isDeviceConnected == Utils.Connected.True) {
                            if (deviceName != null)
                                setStatus(deviceName, activity);
                        }
                    } else if (deviceName != null) {
                        if (isDeviceConnected == Utils.Connected.True) {
                            setStatus(deviceName, activity);
                        }
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
