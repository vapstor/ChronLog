package app.br.chronlog.activitys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
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

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;

import app.br.chronlog.R;
import app.br.chronlog.utils.MyLog;
import app.br.chronlog.utils.MyLogEntry;
import app.br.chronlog.utils.Utils;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;

import static app.br.chronlog.utils.Utils.TAG_LOG;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.myBluetoothController;
import static app.br.chronlog.utils.Utils.send;
import static java.lang.Thread.sleep;

public class ReadDataActivity extends AppCompatActivity implements ServiceConnection, SerialListener {
    private SerialService service;
    public ListView logsListView;
    public static ArrayAdapter<MyLog> logListAdapter;
    public static ArrayList<MyLog> logsList = new ArrayList<>();
    final String protocolReadSdData = "@04000000000000";
    private static String receivedData = "";
    private ImageButton iconRefresh;
    private ProgressBar progressBar;
    private MyLog selectedLog;
    private String[] receivedStrArray;
    private ProgressBar progressBarItem;
    private int receivedSize = 0;
    private final Object lock = new Object();
    private Thread myCommandThread;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_data);

        if (isDeviceConnected != Utils.Connected.True) {
            Toast.makeText(this, R.string.nao_conectado, Toast.LENGTH_SHORT).show();
            finish();
            //((TextView) findViewById(R.id.status)).setText(deviceName);
        }

        iconRefresh = findViewById(R.id.refreshListIcon);
        progressBar = findViewById(R.id.progressBar);

        logsListView = findViewById(R.id.logsListView);
        logsListView.setDivider(new ColorDrawable(0x752ab3cf));
        logsListView.setDividerHeight(1);
        logListAdapter = new ArrayAdapter<MyLog>(this, 0, logsList) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                MyLog log = logsList.get(position);
                if (view == null)
                    view = getLayoutInflater().inflate(R.layout.log_list_item, parent, false);
                TextView logTitle = view.findViewById(R.id.logTitle);
                TextView peso = view.findViewById(R.id.peso);
                logTitle.setText(log.getName());
                peso.setText("(" + log.getPeso().trim() + " kb)");
                return view;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public int getCount() {
                return super.getCount();
            }
        };

        logsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (myBluetoothController.getBluetoothAdapter().isDiscovering()) {
                myBluetoothController.getBluetoothAdapter().cancelDiscovery();
            }
            selectedLog = logsList.get(position);
            progressBarItem = view.findViewById(R.id.progressBarItem);
            readFile(selectedLog);
        });
        logsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.atencao_));
            builder.setMessage("Deseja realmente excluir o log: " + "\n" + logsList.get(position).getName() + "?");
            builder.setPositiveButton("Excluir", (dialog, which) -> deleteLogFile(logsList.get(position).getName()));
            builder.setNegativeButton("Cancelar", (dialog, which) -> {
            });
            builder.create().show();
            return true;
        });
        iconRefresh.setOnClickListener((v) -> {
            getFilesNameByProtocol();
        });

        setFinishOnTouchOutside(true);
    }

    //Resgata 01 Arquivo
    private void readFile(MyLog selectedLog) {
        receivedData = "";
        receivedSize = 0;
        progressBarItem.setVisibility(View.VISIBLE);

        //simula carregamento
        progressBarItem.setVisibility(View.VISIBLE);

        String protocolReadFileData = "@05" + selectedLog.getName() + "0000";
        if (myCommandThread != null) {
            if (myCommandThread.isAlive()) {
                myCommandThread.interrupt();
                //cancela carregamento
                progressBarItem.setVisibility(View.GONE);
            }
        }
        new Thread(() -> {
            synchronized (lock) {
                try {
                    myCommandThread = new Thread(() -> send(protocolReadFileData, this, this));
                    myCommandThread.start();
                    lock.wait(500);
                    if (!receivedData.equals("")) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                configFileReceived(selectedLog);
            }
        }).start();
    }

    //Configura o arquivo em objeto e transforma em String para enviar ao chart
    private void configFileReceived(MyLog selectedLog) {
        runOnUiThread(() -> progressBarItem.setVisibility(View.GONE));
        if (receivedData.equals("")) {
            runOnUiThread(() -> Toast.makeText(this, "Tente Novamente!", Toast.LENGTH_SHORT).show());
        } else {
            receivedStrArray = receivedData.split("(\\r?\\n|\\r)");
            String lineValue;
            ArrayList<MyLogEntry> entries = new ArrayList<>();

            for (int i = 0; i < receivedStrArray.length; i++) {
                if (i >= 2) { // 2 = 3ª linha
                    lineValue = receivedStrArray[i]; //valores a partir da 2 linha [0 - @05, 1 - Data/Hora/...
                    String[] lineValues = lineValue.split(" ");
                    lineValues = Arrays.copyOf(lineValues, 6);
                    if (lineValues[0] == null || lineValues[0].contains("�")) {
                        lineValues[0] = "OPEN";
                    }
                    if (lineValues[1] == null || lineValues[1].contains("�")) {
                        lineValues[1] = "OPEN";
                    }
                    if (lineValues[2] == null || lineValues[2].contains("�")) {
                        lineValues[2] = "OPEN";
                    }
                    if (lineValues[3] == null || lineValues[3].contains("�")) {
                        lineValues[3] = "OPEN";
                    }
                    if (lineValues[4] == null || lineValues[4].contains("�")) {
                        lineValues[4] = "OPEN";
                    }
                    if (lineValues[5] == null || lineValues[5].contains("�")) {
                        lineValues[5] = "OPEN";
                    }

                    entries.add(new MyLogEntry(lineValues[0], lineValues[1], lineValues[2], lineValues[3], lineValues[4], lineValues[5]));
                }
            }
            selectedLog.setEntries(entries);
            if (entries.size() > 1) {
                Gson gs = new Gson();
                String selectedLogAsString = gs.toJson(selectedLog);
                Intent intent = new Intent(this, ChartViewActivity.class);
                intent.putExtra("selectedLogAsString", selectedLogAsString);
                startActivity(intent);
            } else if (entries.size() == 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Só existe apenas um registro no Log: "
                        + "\n" + "\n" +
                        "Data: " + selectedLog.getEntries().get(0).getData() + "\n" +
                        "Horário: " + selectedLog.getEntries().get(0).getHora() + "\n" +
                        "T1: " + selectedLog.getEntries().get(0).getT1() + "\n" +
                        "T2: " + selectedLog.getEntries().get(0).getT2() + "\n" +
                        "T3: " + selectedLog.getEntries().get(0).getT3() + "\n" +
                        "T4: " + selectedLog.getEntries().get(0).getT4()
                );
                builder.setPositiveButton("OK", (dialog, which) -> {
                });
                builder.setTitle("Log Único!");
                runOnUiThread(() -> builder.create().show());
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Não existem registros no Log!");
                builder.setTitle("Log Inválido!");
                builder.setPositiveButton("OK", (dialog, which) -> {
                });
                runOnUiThread(() -> builder.create().show());
            }
        }
    }

    //Resgata Todos os Arquivos
    public void getFilesNameByProtocol() {
        //zera
        logsList.clear();
        receivedData = "";
        send(protocolReadSdData, this, this);
        new Thread(() -> {
            runOnUiThread(() -> {
                logsListView.setAdapter(null);
                logsListView.setAdapter(logListAdapter);

                //simula carregamento
                iconRefresh.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            });

            try {
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            configLogsReceived();
        }).start();
    }

    //Configura arquivos resgatados
    private void configLogsReceived() {
        if (receivedData.equals("")) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Tente Novamente!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                iconRefresh.setVisibility(View.VISIBLE);
            });
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "Logs Resgatados com Sucesso!", Toast.LENGTH_SHORT).show();
                iconRefresh.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            });
            receivedStrArray = receivedData.split("(\\r?\\n|\\r)");
            String nome, peso;
            for (String tmp : receivedStrArray) {
                if (tmp.equals("@")) {
                    tmp = tmp.replace("04", "");
                }
                if (tmp.contains(".LOG")) {

                    int index = tmp.indexOf(" ");

                    nome = tmp.substring(0, index);
                    peso = tmp.substring(index);

                    Log.d(TAG_LOG, "MyLog Nome: " + nome);
                    Log.d(TAG_LOG, "MyLog Peso: " + peso);

                    String finalNome = nome;
                    String finalPeso = peso;
                    runOnUiThread(() -> {
                        logsList.add(new MyLog(finalNome, finalPeso, null)); //recebeu um log
                        swapItems(logsList);
                    });
                }

            }
        }

    }

    private void deleteLogFile(String fileName) {
        /** 6. Delete a file
         * @06FILENAMECRLF FILENAME filename always 8bytes long
         * **/
        receivedData = "";
        String protocolDeleteData = "@06" + fileName + "0000";
        send(protocolDeleteData, this, this);
        new Thread(() -> {
            runOnUiThread(() -> {
                logsListView.setAdapter(null);
                logsListView.setAdapter(logListAdapter);
            });
            try {
                sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!receivedData.equals("")) {
                runOnUiThread(() -> {
                    logListAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Log excluído com sucesso!", Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Falhou ao excluír log, tente novamente!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public void swapItems(ArrayList<MyLog> items) {
        logsList = items;
        logsListView.invalidateViews();
        logListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        myBluetoothController.setActivity(this);

        if (service != null) {
            service.attach(this);
        } else {
            bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
        }
        logsListView.setAdapter(null);
        logsListView.setAdapter(logListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (service != null) {
            service.attach(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        getFilesNameByProtocol();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public void onSerialConnect() {

    }

    @Override
    public void onSerialConnectError(Exception e) {
        Log.d(TAG_LOG, "ERRO DE CONEXAO COM SERVICO [onSerialConnectError]" + e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void onSerialRead(byte[] data) {
        Log.d(TAG_LOG, "Dado Vindo Direto do SOCKET [2]: " + Arrays.toString(data));
        Log.d(TAG_LOG, "Dado Vindo Direto do SOCKET [2]: " + data.length);
        String valorRecebido = new String(data);
        Log.d(TAG_LOG, "VALOR RECEBIDO : " + valorRecebido);
        if (selectedLog != null) {
            synchronized (lock) {
                int pesoTodosAsEntradas = Integer.parseInt(selectedLog.getPeso().trim());

                /**
                 * 1:
                 * FFFD � REPLACEMENT CHARACTER
                 * • used to replace an incoming character whose
                 * value is unknown or unrepresentable
                 * 2:
                 * � REPLACEMENT CHARACTER used to replace an unknown, unrecognized or unrepresentable character
                 * */

                if (valorRecebido.contains("�")) {
                    receivedSize = pesoTodosAsEntradas;
                    Toast.makeText(this, "Valor � Recebido!", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG_LOG, "\nPESO DO ARQUIVO: " + pesoTodosAsEntradas);
                Log.d(TAG_LOG, "\nPESO JA RECEBIDO" + receivedSize);
                Log.d(TAG_LOG, "\nPESO JA RECEBIDO + PESO DO DADO RECEBIDO - > " + (receivedSize + data.length));

                receivedData = receivedData.concat(valorRecebido);
                receivedSize = receivedSize + data.length;

                if (receivedSize >= pesoTodosAsEntradas) {
                    Log.d(TAG_LOG, "\nALERTA: receivedSize >= pesoTodosAsEntradas -> receivedSize = " + receivedSize);
                    lock.notify();
                } else {
                    Log.d(TAG_LOG, "ELSE" + receivedSize);
                }
            }
        } else {
            receivedData = receivedData + valorRecebido;
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        runOnUiThread(() -> {
            isDeviceConnected = Utils.Connected.False;
            Toast.makeText(this, "A conexão foi perdida!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
