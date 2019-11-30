package app.br.chronlog.activitys;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import app.br.chronlog.R;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;
import app.br.chronlog.utils.bluetooth.SerialSocket;

import static android.view.View.INVISIBLE;
import static app.br.chronlog.utils.Utils.passedTime;
import static com.github.mikephil.charting.charts.Chart.LOG_TAG;
import static java.lang.Thread.sleep;

public class DevicesFragment extends ListFragment implements ServiceConnection, SerialListener {

    private static final int REQUEST_USE_BLUETOOTH = 1;
    private static final int REQUEST_USE_COARS_LOCATION = 2;

    private ImageButton refreshButton;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private ProgressBar progressBar;
    private BluetoothDevice deviceSelected;
    private TextView statusView;
    private CountDownTimer countDownTimer;
    private View appBarView;
    private IntentFilter filter;

    private enum Connected {False, Pending, True}

    private String deviceAddress, deviceName;
    private String newline = "\r\n";

    private SerialSocket socket;
    private SerialService service;
    //    private boolean initialStart = true;
    private Connected connected = Connected.False;

    private Thread searchDevices;


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
                    if (listItems != null) {
                        if (listItems.size() == 0) {
                            listAdapter.add(device);
                        } else {
                            for (int i = 0; i < listItems.size(); i++) {
                                if (!listItems.get(i).getAddress().equals(device.getAddress())) {
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
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    //checa a cada 5s se ainda esta ficando distante
                    if (SystemClock.elapsedRealtime() - passedTime < 5000) {
                        Toast.makeText(context, "Sinal de Bluetooth Fraco...", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    passedTime = SystemClock.elapsedRealtime();
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            btOff();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            btOn();
                            break;
                        case BluetoothAdapter.STATE_CONNECTED:
                            Toast.makeText(context, "Dispositivo Conectado!", Toast.LENGTH_SHORT).show();
                            setStatus("Conectado!");
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTED:
                            Toast.makeText(context, "Dispositivo Desconectado!", Toast.LENGTH_LONG).show();
                            setStatus("");
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    setStatus("Procurando...");
                    toggleProgressBarVisibility();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    setStatus("");
                    toggleProgressBarVisibility();
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    setStatus("");
                    break;
            }
        }
    };

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Objects.requireNonNull(getActivity()).getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            if ((ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_USE_BLUETOOTH);
            } else if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_USE_BLUETOOTH);
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showExplanation("Permission Needed", "Rationale", Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_USE_BLUETOOTH);
            } else {
                requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_USE_COARS_LOCATION);
            }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems) {

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                BluetoothDevice device = listItems.get(position);
                if (view == null)
                    view = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                text1.setText(device.getName());
                text2.setText(device.getAddress());
                return view;
            }
        };

        if (bluetoothAdapter != null) {
            //busca dispositivos de primeira apenas na criação do fragmento
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                // ANDROID 6.0 AND UP!
                boolean accessCoarseLocationAllowed = false;
                try {
                    // Invoke checkSelfPermission method from Android 6 (API 23 and UP)
                    java.lang.reflect.Method methodCheckPermission = Activity.class.getMethod("checkSelfPermission", java.lang.String.class);
                    Object resultObj = methodCheckPermission.invoke(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    int result = Integer.parseInt(resultObj.toString());
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        accessCoarseLocationAllowed = true;
                    }
                } catch (Exception ignored) {
                }
                if (accessCoarseLocationAllowed) {
                    searchDevices();
                }
                try {
                    // We have to invoke the method "void requestPermissions (Activity activity, String[] permissions, int requestCode) "
                    // from android 6
                    java.lang.reflect.Method methodRequestPermission = Activity.class.getMethod("requestPermissions", java.lang.String[].class, int.class);
                    methodRequestPermission.invoke(this, new String[]
                            {
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            }, 0x12345);
                } catch (Exception ignored) {
                }
            }
            if (service != null)
                service.attach(this);
            else
                Objects.requireNonNull(getActivity()).startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
        }
    }

    private void toggleProgressBarVisibility() {
        if (progressBar.getVisibility() == INVISIBLE) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(INVISIBLE);
        }
    }

    private void showExplanation(String title, String message, final String permission, final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> requestPermission(permission, permissionRequestCode));
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(getActivity(), new String[]{permissionName}, permissionRequestCode);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        appBarView = Objects.requireNonNull(getActivity()).findViewById(R.id.appBar);

                //titulo
        ((TextView) appBarView.findViewById(R.id.titleBar)).setText(R.string.dispositivos);
        progressBar = appBarView.findViewById(R.id.progressBarAppBar);
        statusView = appBarView.findViewById(R.id.status);

        setStatus("");
        if (bluetoothAdapter == null) {
            setIconToHome(appBarView);
            setEmptyText("Bluetooh Não Suportado!");
        } else {
            setIconToRefreshBluetooth(appBarView);
            if (!bluetoothAdapter.isEnabled()) {
                btOff();
            } else {
                btOn();
            }
        }

        progressBar.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == INVISIBLE) {
                refreshButton.setEnabled(true);
            } else {
                refreshButton.setEnabled(false);
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }

    private void btOn() {
        refreshButton.setImageDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.baseline_bluetooth_searching_white_18dp));
        getActivity().findViewById(R.id.iconBar).setEnabled(true);
        if (connected != Connected.True) {
            setStatus("Bluetooth" + "\n" + "Habilitado");
        }
        if (listItems.size() < 1) {
            setEmptyText("Nenhum Dispositivo Encontrado. :( " + "\n" + "Tente Novamente.");
        }
    }

    private void btOff() {
        setStatus("Bluetooth" + "\n" + "Desabilitado");
        setEmptyText("Habilite o Bluetooth, Por Favor.");
        refreshButton.setImageDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.baseline_bluetooth_disabled_black_18dp));
        getActivity().findViewById(R.id.iconBar).setEnabled(false);
    }


    private void setStatus(String status) {
        statusView.setText(status);
    }

    private void setIconToRefreshBluetooth(View appView) {
        refreshButton = appView.findViewById(R.id.iconBar);
        refreshButton.setImageDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.baseline_sync_white_18dp));
        refreshButton.setOnClickListener(v -> refresh());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        setListAdapter(listAdapter);
    }

    @SuppressWarnings("deprecation")
// onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    private void setIconToHome(View appBar) {
        ImageButton homeButton = appBar.findViewById(R.id.iconBar);
        homeButton.setImageDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.baseline_home_24));
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        homeButton.setOnClickListener((v) -> gotToHome(null));
    }

    private void gotToHome(String[] params) {
        assert getFragmentManager() != null;
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
//        if(params == null) {

//        } else {
        Bundle args = new Bundle();
//            args.putString("device", params);
        transaction.replace(R.id.fragment_container, new MainFragment());
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
//        }


    }

    private void searchDevices() {
        try {
            createFilters();
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

    private void refresh() {
        checkDevicesBonded();
        searchDevices();
    }

    private void checkDevicesBonded() {
        listItems.clear();
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
            if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                listItems.add(device);
        Collections.sort(listItems, DevicesFragment::compareTo);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        bluetoothAdapter.cancelDiscovery();
        deviceSelected = listItems.get(position);
        deviceAddress = deviceSelected.getAddress();
        deviceName = deviceSelected.getName();
        pairDevice(deviceAddress);

    }


    private void pairDevice(String deviceAddress) {
        try {
            toggleProgressBarVisibility();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            setStatus("conectando...");
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Connected to " + deviceName);
            socket.connect(getContext(), service, device);
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

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_USE_BLUETOOTH || requestCode == REQUEST_USE_COARS_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Register for broadcasts when a device is discovered.
                createFilters();
            } else {
                Toast.makeText(getActivity(), "Permissão Negada! :(", Toast.LENGTH_SHORT).show();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }

    private void createFilters() {
        if (filter == null) {
            filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            Objects.requireNonNull(getActivity()).registerReceiver(receiver, filter);
        }
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        toggleProgressBarVisibility();
        setStatus("conectado.");
        connected = Connected.True;
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (deviceName.length() > 10) {
            setStatus(deviceName.substring(0, 7) + "...");
        } else {
            setStatus(deviceName);
        }
        Toast.makeText(getContext(), "Aparelho conectado com sucesso!", Toast.LENGTH_SHORT).show();
        String[] params = new String[]{deviceName};
        gotToHome(params);
    }

    @Override
    public void onSerialConnectError(Exception e) {
        setStatus("conexao falhou");
        toggleProgressBarVisibility();
        e.printStackTrace();
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
//        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        setStatus("conexão perdida");
        try {
            sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        if (progressBar.getVisibility() == View.VISIBLE) {
            toggleProgressBarVisibility();
        }
        Toast.makeText(getContext(), "O Aparelho Foi Desconectado!", Toast.LENGTH_LONG).show();
        e.getMessage();
        disconnect();
    }

    private void disconnect() {
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

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }
}
