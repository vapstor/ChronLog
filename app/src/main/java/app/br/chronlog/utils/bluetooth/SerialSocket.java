package app.br.chronlog.utils.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;

import static app.br.chronlog.utils.Utils.TAG_LOG;

public class SerialSocket implements Runnable {

    private static final UUID BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BroadcastReceiver disconnectBroadcastReceiver;

    private Context context;
    private SerialListener listener;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private boolean connected;
    private int somatorioTotalLength;

    public SerialSocket() {
        disconnectBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (listener != null)
                    listener.onSerialIoError(new IOException("background disconnect"));
                disconnect(); // disconnect now, else would be queued until UI re-attached
            }
        };
    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    public void connect(Context context, SerialListener listener, BluetoothDevice device) throws IOException {
        if (connected || socket != null)
            throw new IOException("ja conectado");
        this.context = context;
        this.listener = listener;
        this.device = device;
        context.registerReceiver(disconnectBroadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_DISCONNECT));
        Executors.newFixedThreadPool(1).submit(this);
    }


    public void disconnect() {
        listener = null; // ignore remaining data and errors
        // deviceIsConnected = false; // run loop will reset deviceIsConnected
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        try {
            context.unregisterReceiver(disconnectBroadcastReceiver);
        } catch (Exception ignored) {
        }
    }

    public void write(byte[] data) throws IOException {
        somatorioTotalLength = 0;
        if (!connected)
            throw new IOException("nao conectado");
        socket.getOutputStream().write(data);
    }

    @Override
    public void run() { // connect & read
        try {
            socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_SPP);
            socket.connect();
            if (listener != null)
                listener.onSerialConnect();
        } catch (Exception e) {
            if (listener != null)
                listener.onSerialConnectError(e);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
            return;
        }
        connected = true;
        try {
            byte[] buffer = new byte[1024];
            int len;
            //noinspection InfiniteLoopStatement
            while (true) {
                len = socket.getInputStream().read(buffer);
                Log.d(TAG_LOG, "LEN - Dado Vindo Direto do SOCKET: " + len);
                byte[] data = Arrays.copyOf(buffer, len);
                somatorioTotalLength = somatorioTotalLength + data.length;
                Log.d(TAG_LOG, "Dado Vindo Direto do SOCKET: " + data.length);
                Log.d(TAG_LOG, "Dado Vindo Direto do SOCKET [SOMATORIO TOTAL]: " + somatorioTotalLength);
                if (listener != null)
                    listener.onSerialRead(data);
            }
        } catch (Exception e) {
            connected = false;
            if (listener != null)
                listener.onSerialIoError(e);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }

    public void closeOutPutStream() {
        try {
            if (socket != null) {
                socket.getOutputStream().close();
            }
        } catch (IOException e) {
            listener.onSerialIoError(e);
            e.printStackTrace();
        }
    }

}