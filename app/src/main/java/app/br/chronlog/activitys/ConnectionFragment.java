//package app.br.chronlog.activitys;
//
//import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.text.Spannable;
//import android.text.SpannableStringBuilder;
//import android.text.style.ForegroundColorSpan;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//
//import app.br.chronlog.R;
//import app.br.chronlog.utils.bluetooth.SerialListener;
//import app.br.chronlog.utils.bluetooth.SerialService;
//import app.br.chronlog.utils.bluetooth.SerialSocket;
//
//
//public class ConnectionFragment extends Fragment implements ServiceConnection, SerialListener, View.OnClickListener {
//
//    private ProgressBar progressBar;
//    private TextView sendText;
//
//    private enum Connected { False, Pending, True }
//
//    private String deviceAddress, deviceName;
//    private String newline = "\r\n";
//
//    private TextView receiveText;
//
//    private SerialSocket socket;
//    private SerialService service;
//    private boolean initialStart = true;
//    private Connected connected = Connected.False;
//
//    /*
//     * Lifecycle
//     */
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setRetainInstance(true);
//        deviceAddress = getArguments().getString("device");
//        deviceName = getArguments().getString("deviceName");
//    }
//
//    @Override
//    public void onDestroy() {
//        if (connected != Connected.False)
//            disconnect();
//        getActivity().stopService(new Intent(getActivity(), SerialService.class));
//        super.onDestroy();
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        if(service != null)
//            service.attach(this);
//        else
//            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
//    }
//
//    @Override
//    public void onStop() {
//        if(service != null && !getActivity().isChangingConfigurations())
//            service.detach();
//        super.onStop();
//    }
//
//    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
//    }
//
//    @Override
//    public void onDetach() {
//        try { getActivity().unbindService(this); } catch(Exception ignored) {}
//        super.onDetach();
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if(initialStart && service !=null) {
//            initialStart = false;
//            getActivity().runOnUiThread(this::connect);
//        }
//    }
//
//    @Override
//    public void onServiceConnected(ComponentName name, IBinder binder) {
//        service = ((SerialService.SerialBinder) binder).getService();
//        if(initialStart && isResumed()) {
//            initialStart = false;
//            getActivity().runOnUiThread(this::connect);
//        }
//    }
//
//    @Override
//    public void onServiceDisconnected(ComponentName name) {
//        service = null;
//    }
//
//    /*
//     * UI
//     */
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_connection, container, false);
//
////        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
////        receiveText.setTextColor(getResources().getColor(R.color.colorPrimary)); // set as default color to reduce number of spans
////        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
////        sendText = view.findViewById(R.id.send_text);
//
//    }
//
//    /*
//     * Serial + UI
//     */
//    private void connect() {
//        try {
//            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
//            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
//            status("connecting...");
//            connected = Connected.Pending;
//            socket = new SerialSocket();
//            service.connect(this, "Connected to " + deviceName);
//            socket.connect(getContext(), service, device);
//        } catch (Exception e) {
//            onSerialConnectError(e);
//        }
//    }
//
//    private void disconnect() {
//        connected = Connected.False;
//        service.disconnect();
//        socket.disconnect();
//        socket = null;
//    }
//
//    private void send(String str) {
//        if(connected != Connected.True) {
//            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
//            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimaryDark)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
//            byte[] data = (str + newline).getBytes();
//            socket.write(data);
//        } catch (Exception e) {
//            onSerialIoError(e);
//        }
//    }
//
//    private void receive(byte[] data) {
//        receiveText.append(new String(data));
//    }
//
//    private void status(String str) {
//        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
//        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        receiveText.append(spn);
//    }
//
//    /*
//     * SerialListener
//     */
//    @Override
//    public void onSerialConnect() {
//        status("connected");
//        connected = Connected.True;
//    }
//
//    @Override
//    public void onSerialConnectError(Exception e) {
//        status("connection failed: " + e.getMessage());
//        disconnect();
//    }
//
//    @Override
//    public void onSerialRead(byte[] data) {
//        receive(data);
//    }
//
//    @Override
//    public void onSerialIoError(Exception e) {
//        status("connection lost: " + e.getMessage());
//        disconnect();
//    }
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.iconBar:
//                ((AppCompatActivity)getContext()).getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//                break;
//            case R.id.send_btn:
//                send(sendText.getText().toString());
//                break;
//        }
//    }
//}