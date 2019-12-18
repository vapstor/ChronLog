//package app.br.chronlog.utils.bluetooth;
//
//import android.Manifest;
//import android.app.Activity;
//import android.app.Application;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.CountDownTimer;
//import android.util.Log;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.core.app.ActivityCompat;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Objects;
//
//import app.br.chronlog.R;
//
//import static app.br.chronlog.activitys.DevicesActivity.devicesList;
//import static app.br.chronlog.activitys.DevicesActivity.listAdapter;
//import static app.br.chronlog.utils.Utils.TAG_LOG;
//
//
//public class MyBluetoothController extends Application {
//    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//    private Activity activity;
//    private ProgressBar progressBar;
//    public SerialSocket socket;
//
//    private static final int REQUEST_USE_BLUETOOTH = 1;
//    private static final int REQUEST_USE_COARS_LOCATION = 2;
//
//    private IntentFilter filter;
//    private CountDownTimer countDownTimer;
//    private Thread searchDevices;
//    private TextView statusView;
//    private String deviceName, deviceAddress;
//    public static boolean recebido;
//    private SerialService service;
//
//    //    private boolean initialStart = true;
//    public enum Connected {False, Pending, True}
//
//    public BluetoothController.Connected connected = BluetoothController.Connected.False;
//    // Create a BroadcastReceiver for BLUETOOTH_CHANGES.
//    private final BroadcastReceiver receiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            BluetoothDevice device;
//            assert action != null;
//            switch (action) {
//                case BluetoothDevice.ACTION_FOUND:
//                    // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
//                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    //String deviceName = device.getName();
//                    //String deviceHardwareAddress = device.getAddress(); // MAC address
//                    if (devicesList != null) {
//                        if (devicesList.size() == 0) {
//                            listAdapter.add(device);
//                            listAdapter.notifyDataSetChanged();
//                        } else {
//                            if (!devicesList.contains(device)) {
//                                listAdapter.add(device);
//                                listAdapter.notifyDataSetChanged();
//                            }
//                        }
//                    }
//                    break;
//                case BluetoothDevice.ACTION_PAIRING_REQUEST:
//                    try {
//                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                        int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234);
//                        //the pin in case you need to accept for an specific pin
//                        TermoparLog.d(TAG_LOG, "Start Auto Pairing. PIN = " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234));
//                        byte[] pinBytes;
//                        pinBytes = ("" + pin).getBytes(StandardCharsets.UTF_8);
//                        assert device != null;
//                        device.setPin(pinBytes);
//                        //setPairing confirmation if neeeded
//                        device.setPairingConfirmation(true);
//                    } catch (Exception e) {
//                        TermoparLog.e(TAG_LOG, "Error occurs when trying to auto pair");
//                        e.printStackTrace();
//                    }
//                    break;
//                //                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
////                    //checa a cada 5s se ainda esta ficando distante
////                    if (SystemClock.elapsedRealtime() - passedTime < 5000) {
////                        Toast.makeText(context, "Sinal de Bluetooth Fraco...", Toast.LENGTH_SHORT).show();
////                        return;
////                    }
////                    passedTime = SystemClock.elapsedRealtime();
////                    break;
//                case BluetoothAdapter.ACTION_STATE_CHANGED:
//                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//                    switch (state) {
//                        case BluetoothAdapter.STATE_OFF:
//                            Toast.makeText(context, "Bluetooth Desativado!", Toast.LENGTH_SHORT).show();
//                            break;
//                        case BluetoothAdapter.STATE_ON:
//                            Toast.makeText(context, "Bluetooth Ativado!", Toast.LENGTH_SHORT).show();
//                            break;
//                        case BluetoothAdapter.STATE_CONNECTED:
//                            Toast.makeText(context, "Bluetooth Conectado!", Toast.LENGTH_SHORT).show();
//                            hideProgressBar();
//                            break;
//                        case BluetoothAdapter.STATE_DISCONNECTED:
//                            Toast.makeText(context, "Bluetooth Desconectado!", Toast.LENGTH_LONG).show();
//                            setStatus("");
//                            break;
//                    }
//                    break;
//                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
//                    actionDiscoverStarted();
//                    break;
//                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
//                    hideProgressBar();
//                    if (statusView.getText().toString().equals("conectando...")) {
//
//                    } else if (statusView.getText().toString().equals(deviceName)) {
//                    } else {
//                        setStatus("");
//                    }
//                    break;
//                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
//                    setStatus("");
//                    break;
//            }
//        }
//    };
//
//
//    public void send(String str) {
//        TermoparLog.d(TAG_LOG, "String a enviar para termopar: " + str);
//        if (connected != BluetoothController.Connected.True) {
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
//        TermoparLog.d(TAG_LOG, "receive: " + messageReceived);
//    }
//
////        @Override
////    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
////        if (requestCode == REQUEST_USE_BLUETOOTH || requestCode == REQUEST_USE_COARS_LOCATION) {
////            // If request is cancelled, the result arrays are empty.
////            if (grantResults.length > 0
////                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
////                // Register for broadcasts when a device is discovered.
////                createFilters();
////            } else {
////                Toast.makeText(activity, "Permissão Negada! :(", Toast.LENGTH_SHORT).show();
////                // permission denied, boo! Disable the
////                // functionality that depends on this permission.
////            }
////        }
////    }
//
//    private void createFilters() {
//        if (filter == null) {
//            filter = new IntentFilter();
//            filter.addAction(BluetoothDevice.ACTION_FOUND);
//            filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
//            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
//            Objects.requireNonNull(activity).registerReceiver(receiver, filter);
//        }
//    }
//
//    private void actionDiscoverStarted() {
//        setStatus("procurando...");
//        showProgressBar();
//    }
//
//    public MyBluetoothController(Activity activity) {
//        this.activity = activity;
//        if (bluetoothAdapter != null) {
//            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
//            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
//            if (socket == null) {
//                socket = new SerialSocket();
//            }
//            //            //busca dispositivos de primeira apenas na criação do fragmento
////            if (android.os.Build.VERSION.SDK_INT >= 23) {
////                // ANDROID 6.0 AND UP!
////                boolean accessCoarseLocationAllowed = false;
////                try {
////                    // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
////                    java.lang.reflect.Method methodCheckPermission = Activity.class.getMethod("checkSelfPermission", java.lang.String.class);
////                    Object resultObj = methodCheckPermission.invoke(this, Manifest.permission.ACCESS_COARSE_LOCATION);
////                    int result = Integer.parseInt(resultObj.toString());
////                    if (result == PackageManager.PERMISSION_GRANTED) {
////                        accessCoarseLocationAllowed = true;
////                    }
////                } catch (Exception ignored) {
////                }
////                if (accessCoarseLocationAllowed) {
////                    refresh();
////                }
////                try {
////                    // We have to invoke the method "void requestPermissions (Activity activity, String[] permissions, int requestCode) "
////                    // from android 6
////                    java.lang.reflect.Method methodRequestPermission = Activity.class.getMethod("requestPermissions", java.lang.String[].class, int.class);
////                    methodRequestPermission.invoke(this, new String[]
////                            {
////                                    Manifest.permission.ACCESS_COARSE_LOCATION
////                            }, 0x12345);
////                } catch (Exception ignored) {
////                }
////            }
//            if (service != null)
//                service.attach(this);
//            else
//                Objects.requireNonNull(activity).startService(new Intent(activity, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
//            activity.bindService(new Intent(activity, SerialService.class), this, Context.BIND_AUTO_CREATE);
//        }
//        statusView = activity.findViewById(R.id.status);
//        progressBar = activity.findViewById(R.id.progressBarAppBar);
//        createFilters();
//    }
//}
