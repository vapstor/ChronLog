package app.br.chronlog.activitys;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Collections;

import app.br.chronlog.R;

import static android.view.View.INVISIBLE;

public class DevicesFragment extends ListFragment implements View.OnClickListener {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private ProgressBar progressBar;
    private BluetoothDevice deviceSelected;
    private IntentFilter filter;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                String deviceName = device.getName();
//                String deviceHardwareAddress = device.getAddress(); // MAC address
                listAdapter.add(device);
                listAdapter.notifyDataSetChanged();
            }
        }
    };
    private int PERMISSION_REQUEST_CODE = 10;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                BluetoothDevice device = listItems.get(position);
                if (view == null)
                    view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                text1.setText(device.getName());
                text2.setText(device.getAddress());
                return view;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        ConstraintLayout appView = getActivity().findViewById(R.id.appBar);

        ((MaterialTextView) appView.findViewById(R.id.titleBar)).setText(R.string.dispositivos);

        MaterialButton icon = appView.findViewById(R.id.iconBar);
        icon.setIcon(getActivity().getDrawable(R.drawable.baseline_sync_24));

        progressBar = appView.findViewById(R.id.progressBarAppBar);

        progressBar.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == INVISIBLE) {
                icon.setEnabled(true);
            } else {
                icon.setEnabled(false);
            }
        });
        icon.setOnClickListener(this);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);


        setListAdapter(listAdapter);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            askForPermission(Manifest.permission.BLUETOOTH_ADMIN, PERMISSION_REQUEST_CODE);
        } else if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            askForPermission(Manifest.permission.BLUETOOTH, PERMISSION_REQUEST_CODE);
        } else {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getActivity().registerReceiver(receiver, filter);
            bluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(getActivity(), permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                //Call
                default:
                    // Register for broadcasts when a device is discovered.
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    getActivity().registerReceiver(receiver, filter);
                    bluetoothAdapter.startDiscovery();
                    break;
            }
            Toast.makeText(getActivity(), "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //        new Thread(() -> {
//            try {
////                sleep(750);
////                getActivity().runOnUiThread(() -> progressBar.setVisibility(View.INVISIBLE));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
        if (bluetoothAdapter == null)
            setEmptyText("Bluetooth Não Suportado.");
        else if (!bluetoothAdapter.isEnabled())
            setEmptyText("Bluetooth Desabilitado.");
        else
            setEmptyText("Nenhum Aparelho Encontrado. :(");
        refresh();
    }

    void refresh() {
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
        TerminalFragment fragment = new TerminalFragment();
        Bundle extras = new Bundle();
        extras.putString("device", deviceSelected.getAddress());
        extras.putString("deviceName", deviceSelected.getName());
        fragment.setArguments(extras);


        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, "config").addToBackStack(null).commit();
        }
    }


    /**
     * sort by name, then address. sort named devices first
     */
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
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

    public void abreConfiguracoesCel(View view) {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        FragmentTransaction transaction;
        switch (v.getId()) {
            case R.id.iconBar:
                refresh();
//                // Replace whatever is in the fragment_container view with this fragment,
//                // and add the transaction to the back stack
//                transaction = getFragmentManager().beginTransaction();
//                transaction.replace(R.id.fragment_container, new ConfigDeviceFragment());
//                transaction.addToBackStack(null);
//                // Commit the transaction
//                transaction.commit();
                break;
            default:
                Toast.makeText(getContext(), "TOASTE", Toast.LENGTH_SHORT).show();
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

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);
            }
        } else {
            Toast.makeText(getContext(), "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDetach() {
        if (receiver == null) {
            getActivity().unregisterReceiver(receiver);
        }
        super.onDetach();
    }
}
