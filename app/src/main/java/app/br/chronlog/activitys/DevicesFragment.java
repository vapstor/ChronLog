package app.br.chronlog.activitys;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import app.br.chronlog.R;

import static android.view.View.GONE;
import static app.br.chronlog.activitys.MainActivity.universalBtController;

public class DevicesFragment extends ListFragment {
    public static ArrayAdapter<BluetoothDevice> listAdapter;
    public static ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private BluetoothDevice deviceSelected;

    private ProgressBar progressBar;
    private ImageButton refreshButton;

    private TextView statusView;
    private String deviceAddress, deviceName;

    private View appBarView;

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
        //        if (Objects.requireNonNull(getActivity()).getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
//            if ((ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)) {
//                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_USE_BLUETOOTH);
//            } else if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_USE_BLUETOOTH);
//            } else if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
//                showExplanation("Permission Needed", "Rationale", Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_USE_BLUETOOTH);
//            } else {
//                requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_USE_COARS_LOCATION);
//            }
        listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, devicesList) {

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                BluetoothDevice device = devicesList.get(position);
                if (view == null)
                    view = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                text1.setText(device.getName());
                text2.setText(device.getAddress());
                return view;
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // If you want to auto-input the pin#:
        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(requestCode)) {
            deviceSelected.setPin("1234".getBytes());
        }
        super.onActivityResult(requestCode, resultCode, data);
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

        progressBar.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == GONE) {
                refreshButton.setEnabled(true);
                refreshButton.setImageDrawable(getActivity().getDrawable(R.drawable.baseline_sync_white_18dp));
            } else {
                refreshButton.setEnabled(false);
                refreshButton.setImageDrawable(getActivity().getDrawable(R.drawable.baseline_sync_black_18));
            }
        });

        //começa a busca
        refresh();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        setStatus("");
        if (universalBtController.getBluetoothAdapter() == null) {
            setIconToHome(appBarView);
            setEmptyText("Bluetooh Não Suportado!");
        } else {
            setIconToRefreshBluetooth(appBarView);
            if (!universalBtController.getBluetoothAdapter().isEnabled()) {
                btOff();
            } else {
                btOn();
            }
        }
    }

    public void btOn() {
        //refreshButton.setImageDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.baseline_bluetooth_searching_white_18dp));
        refreshButton.setImageDrawable(getActivity().getDrawable(R.drawable.baseline_sync_white_18dp));
        getActivity().findViewById(R.id.iconBar).setEnabled(true);
        if (devicesList.size() < 1) {
            setEmptyText("Nenhum Dispositivo Encontrado. :( " + "\n" + "Tente Novamente.");
        }
    }

    public void btOff() {
        setEmptyText("Habilite o Bluetooth, Por Favor.");
        //refreshButton.setImageDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.baseline_bluetooth_disabled_black_18dp));
        refreshButton.setImageDrawable(getActivity().getDrawable(R.drawable.baseline_bluetooth_disabled_black_18dp));
        getActivity().findViewById(R.id.iconBar).setEnabled(false);
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

    private void setStatus(String status) {
        statusView.setText(status);
    }

    private void refresh() {
        checkDevicesBonded();
        universalBtController.searchDevices();
    }

    private void checkDevicesBonded() {
        devicesList.clear();
        for (BluetoothDevice device : universalBtController.getBluetoothAdapter().getBondedDevices())
            if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                devicesList.add(device);
        Collections.sort(devicesList, DevicesFragment::compareTo);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        synchronized (this) {
            if (universalBtController.getBluetoothAdapter().isDiscovering()) {
                universalBtController.getBluetoothAdapter().cancelDiscovery();
            }
            setStatus("conectando...");
            deviceSelected = devicesList.get(position);
            universalBtController.pairDevice(deviceSelected);
        }
    }


//    @Override
//    public void onDetach() {
//        try {
//            getActivity().unbindService(this);
//        } catch (Exception ignored) {
//        }
//        super.onDetach();
//    }

}
