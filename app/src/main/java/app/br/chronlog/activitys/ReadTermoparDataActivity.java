package app.br.chronlog.activitys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import app.br.chronlog.R;
import app.br.chronlog.utils.ItemClickSupport;
import app.br.chronlog.utils.RecyclerAdapter;
import app.br.chronlog.utils.RecyclerItemTouchHelper;
import app.br.chronlog.utils.TermoparLog;
import app.br.chronlog.utils.TermoparLogEntry;
import app.br.chronlog.utils.Utils;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static app.br.chronlog.utils.Utils.TAG_LOG;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.myBluetoothController;
import static app.br.chronlog.utils.Utils.send;
import static java.lang.Thread.sleep;

public class ReadTermoparDataActivity extends AppCompatActivity implements ServiceConnection, SerialListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {
    private SerialService service;
    public RecyclerView logsRecyclerView;
    public static ArrayList<TermoparLog> logsList = new ArrayList<>();
    final String protocolReadSdData = "@04000000000000";
    private static String receivedData = "";
    private TermoparLog selectedLog;
    private String[] receivedStrArray;
    private ProgressBar progressBarContainerView, progressBarItem, progressBarOpenLogDialog;
    private int receivedSize = 0;
    private final Object lock = new Object();
    private Thread myCommandThread;
    private int stopReceiveFlag = 0;
    private boolean aberturaCancelada;
    private RecyclerAdapter adapter;
    AlertDialog[] alertDialog = new AlertDialog[2];
    private AlertDialog.Builder builder;
    private ProgressBar progressBarSaveShareLog;
    private MaterialButton buttonSave, buttonShare;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_termopar_data);

        if (isDeviceConnected != Utils.Connected.True) {
            Toast.makeText(this, R.string.nao_conectado, Toast.LENGTH_SHORT).show();
            finish();
        }

        progressBarContainerView = findViewById(R.id.progressBar);

        logsRecyclerView = findViewById(R.id.logsListView);
        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        logsRecyclerView.addItemDecoration(decoration);
        adapter = new RecyclerAdapter(logsList, null, progressBarContainerView);
        adapter.setHasStableIds(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        logsRecyclerView.setLayoutManager(linearLayoutManager);

        logsRecyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(logsRecyclerView);
        ItemClickSupport.addTo(logsRecyclerView).setOnItemClickListener((recyclerView, position, v) -> {
            selectedLog = logsList.get(position);
            progressBarItem = v.findViewById(R.id.progressBarItem);

            builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
            builder.setView(R.layout.open_log_dialog);

            readFile(selectedLog, false);
        });
        ItemClickSupport.addTo(logsRecyclerView).setOnItemLongClickListener((RecyclerView recyclerView, int position, View v) -> {
            builder = new AlertDialog.Builder(ReadTermoparDataActivity.this, R.style.DialogOpenLogStyle);
            builder.setView(R.layout.share_save_item_dialog);

            selectedLog = logsList.get(position);

            if (alertDialog[0] != null) {
                if (alertDialog[0].isShowing()) {
                    alertDialog[0].dismiss();
                }
                alertDialog[0] = null;
            }

            alertDialog[0] = builder.create();
            alertDialog[0].show();

            progressBarSaveShareLog = alertDialog[0].findViewById(R.id.progressBarSaveShareLog);
            ((TextView) Objects.requireNonNull(alertDialog[0].findViewById(R.id.logId))).setText(selectedLog.getName() + " (" + selectedLog.getPeso().trim() + " bytes)");

            buttonShare = alertDialog[0].findViewById(R.id.btnShareLog);
            if (buttonShare != null) {
                buttonShare.setOnClickListener(v1 -> {
                    Intent sharingIntent = new Intent(
                            android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBody = receivedData.replace("@05", "");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                            "Log: " + logsList.get(position).getName());
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, "Compartilhar Via"));
                });
            }
            buttonSave = alertDialog[0].findViewById(R.id.btnSaveLog);
            if (buttonSave != null) {
                buttonSave.setOnClickListener(v1 -> {
                    try {
                        String name = logsList.get(position).getName();
                        String peso = logsList.get(position).getPeso();

                        if (peso == null) {
                            if (isFilePresent(name)) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ReadTermoparDataActivity.this);
                                builder.setTitle("Arquivo já existe!");
                                builder.setMessage("Arquivo já existe no aparelho, deseja sobreescrever?");
                                builder.setPositiveButton("Salvar", (dialog, which) -> {
                                    try {
                                        saveFileToLocalSD(logsList.get(position).getName(), null);
                                        adapter.notifyItemChanged(position);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(ReadTermoparDataActivity.this, "Falhou ao salvar o arquivo!", Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(position);
                                    }
                                });
                                builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position));
                                builder.setCancelable(false);
                                builder.create().show();
                            } else {
                                saveFileToLocalSD(logsList.get(position).getName(), logsList.get(position).getPeso());
                                Toast.makeText(ReadTermoparDataActivity.this, "Arquivo salvo com sucesso!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            if (isFilePresent(name + peso)) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ReadTermoparDataActivity.this);
                                builder.setTitle("Arquivo já existe!");
                                builder.setMessage("Arquivo já existe no aparelho, deseja sobreescrever?");
                                builder.setPositiveButton("Salvar", (dialog, which) -> {
                                    try {
                                        saveFileToLocalSD(logsList.get(position).getName(), logsList.get(position).getPeso());
                                        adapter.notifyItemChanged(position);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(ReadTermoparDataActivity.this, "Falhou ao salvar o arquivo!", Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(position);
                                    }
                                });
                                builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position));
                                builder.setCancelable(false);
                                builder.create().show();
                            } else {
                                saveFileToLocalSD(logsList.get(position).getName(), logsList.get(position).getPeso());
                                Toast.makeText(ReadTermoparDataActivity.this, "Arquivo salvo com sucesso!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(ReadTermoparDataActivity.this, "Falhou ao salvar o arquivo!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            //inicia processo de resgate do log
            readFile(logsList.get(position), true);
            return true;
        });

        setFinishOnTouchOutside(true);
    }

    public boolean isFilePresent(String fileName) {
        String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + fileName;
        File file = new File(path);
        return file.exists();
    }

    private void saveFileToLocalSD(String fileName, String peso) throws IOException {
        if (receivedData != null) {
            String logRecebido;
            if (receivedData.startsWith("@05")) {
                logRecebido = receivedData.substring(3);
            } else {
                logRecebido = receivedData;
            }
            saveFile(fileName, peso, logRecebido);
        }
    }

    private void saveFile(String fileName, String peso, String value) throws IOException {
        BufferedWriter file;
        if (peso != null) {
            file = new BufferedWriter(new FileWriter(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + fileName + " " + peso)); //ja salva com ".LOG"
        } else {
            file = new BufferedWriter(new FileWriter(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + fileName + "_")); //ja salva com ".LOG"
        }
        file.write(value);
        file.flush();
        file.close();
    }

    //Resgata 01 Arquivo
    //@param saveToSd = boolean para definir se é apenas para ler (salvar/compartilhar) ou seguir para tela do chart
    private void readFile(TermoparLog selectedLog, boolean saveToSD) {
        receivedData = "";
        receivedSize = 0;

        //interface no item para informar que estamos tentando ler o arquivo
        if (!saveToSD) {
            progressBarItem.setVisibility(View.VISIBLE);
        }

        String protocolReadFileData = "@05" + selectedLog.getName() + "0000";
        if (myCommandThread != null) {
            if (myCommandThread.isAlive()) {
                myCommandThread.interrupt();
            }
        }

        if (!saveToSD) {
            if (alertDialog[0] == null) {
                alertDialog[0] = builder.create();
                alertDialog[0].show();

                progressBarOpenLogDialog = alertDialog[0].findViewById(R.id.progressBarOpenLog);
                if (progressBarOpenLogDialog != null) {
                    progressBarOpenLogDialog.setMax(Integer.parseInt(selectedLog.getPeso().trim()) * 10);
                }
                Button btnCancel = alertDialog[0].findViewById(R.id.btnCancelOpenLog);
                if (btnCancel != null) {
                    btnCancel.setOnClickListener(v -> {
                        progressBarOpenLogDialog.setProgress(0);
                        aberturaCancelada = true;
                    });
                }
            }
        } else {
            if (alertDialog[0] != null) {
                if (progressBarSaveShareLog != null) {
                    progressBarSaveShareLog.setMax(Integer.parseInt(selectedLog.getPeso().trim()) * 10);
                }
            }
        }
        new Thread(() -> {
            synchronized (lock) {
                try {
                    myCommandThread = new Thread(() -> send(protocolReadFileData, this, this));
                    myCommandThread.start();
                    lock.wait(300);
                    if (receivedData.equals("")) {
                        readFile(selectedLog, saveToSD);
                    } else {
                        if (!saveToSD) {
                            runOnUiThread(() -> progressBarItem.setVisibility(View.INVISIBLE));
                        }

                        lock.wait();

                        //resgatou os dados
                        runOnUiThread(() -> {
                            if (alertDialog[0] != null) {
                                if (!saveToSD) {
                                    if (alertDialog[0].isShowing()) {
                                        alertDialog[0].dismiss();
                                    }
                                    alertDialog[0] = null;
                                }
                            }
                        });
                        //terminou de receber ou foi cancelado
                        if (!saveToSD) {
                            if (!aberturaCancelada) {
                                configFileReceived(selectedLog);
//                            } else {
//                                runOnUiThread(() -> Toast.makeText(this, "Cancelado!", Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            runOnUiThread(() -> {
                                if (buttonSave != null)
                                    buttonSave.setEnabled(true);
                                if (buttonShare != null)
                                    buttonShare.setEnabled(true);
                            });
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //Configura o arquivo PARCELABLE para enviar ao chart
    private void configFileReceived(TermoparLog selectedLog) {
        receivedStrArray = receivedData.split("(\\r?\\n|\\r)");
        String lineValue;
        ArrayList<TermoparLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < receivedStrArray.length; i++) {
            if (i >= 2) { // 2 = 3ª linha [valores a partir da 2 linha[0 - @ 05, 1 - Data / Hora /...]
                lineValue = receivedStrArray[i];

                String[] colunas = lineValue.split(" ");
                colunas = Arrays.copyOf(colunas, 6);

                for (int j = 0; j < colunas.length; j++) {
                    if (colunas[j] == null || colunas[j].contains("�") || colunas[j].contains("OVUV")) {
                        colunas[j] = "OPEN";
                    }
                }
                entries.add(new TermoparLogEntry(colunas[0], colunas[1], colunas[2], colunas[3], colunas[4], colunas[5]));
            }
        }

        selectedLog.setEntries(entries);
        if (entries.size() > 1) {
            runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

            ArrayList<TermoparLog> termoparLog = new ArrayList<>();
            termoparLog.add(selectedLog);

            Intent intent = new Intent(this, ChartViewActivity.class);
            intent.putParcelableArrayListExtra("selectedLog", termoparLog);
            startActivity(intent);

        } else if (entries.size() == 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
            builder.setMessage("Não existem registros no Log!");
            builder.setTitle("Log Inválido!");
            builder.setPositiveButton("OK", (dialog, which) -> {
            });
            runOnUiThread(() -> builder.create().show());
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
                logsRecyclerView.setAdapter(null);
                logsRecyclerView.setAdapter(adapter);

                //simula carregamento
                progressBarContainerView.setVisibility(View.VISIBLE);
            });
            configLogsReceived();
        }).start();
    }

    //Configura arquivos resgatados
    private void configLogsReceived() {
        try {
            sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (receivedData.equals("")) {
            getFilesNameByProtocol();
//            runOnUiThread(() -> {
//                Toast.makeText(this, "Tente Novamente!", Toast.LENGTH_SHORT).show();
//                progressBarContainerView.setVisibility(View.GONE);
//                iconRefresh.setVisibility(View.VISIBLE);
//            });
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "Logs Resgatados com Sucesso!", Toast.LENGTH_SHORT).show();
                progressBarContainerView.setVisibility(View.GONE);
            });
            receivedStrArray = receivedData.split("(\\r?\\n|\\r)");
            String nome, peso;
            for (String tmp : receivedStrArray) {
                if (tmp.equals("@")) {
                    tmp = tmp.replace("@04", "");
                }
                if (tmp.contains(".LOG")) {

                    int index = tmp.indexOf(" ");

                    nome = tmp.substring(0, index);
                    peso = tmp.substring(index);

                    Log.d(TAG_LOG, "TermoparLog Nome: " + nome);
                    Log.d(TAG_LOG, "TermoparLog Peso: " + peso);

                    String finalNome = nome;
                    String finalPeso = peso;
                    runOnUiThread(() -> {
                        logsList.add(new TermoparLog(finalNome, finalPeso, null)); //recebeu um log
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
                logsRecyclerView.setAdapter(null);
                logsRecyclerView.setAdapter(adapter);
            });
            if (!receivedData.equals("")) {
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Log excluído com sucesso!", Toast.LENGTH_SHORT).show();
                });
            } else {
                deleteLogFile(fileName);
//                runOnUiThread(() -> Toast.makeText(this, "Falhou ao excluír log, tente novamente!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public void swapItems(ArrayList<TermoparLog> items) {
        logsList = items;
        adapter.notifyDataSetChanged();
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
        logsRecyclerView.setAdapter(null);
        logsRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (service != null) {
            service.attach(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (myCommandThread != null) {
            if (myCommandThread.isAlive()) {
                myCommandThread.interrupt();
            }
            myCommandThread = null;
        }
        super.onDestroy();
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
                Log.d(TAG_LOG, "\nPESO JA RECEBIDO: " + receivedSize);
                Log.d(TAG_LOG, "\nPESO JA RECEBIDO + PESO DO DADO RECEBIDO - > " + (receivedSize + data.length));

                String same = receivedData.concat(valorRecebido);

                Log.d(TAG_LOG, "SAME :" + same);

                if (receivedData.contains("error")) {
                    stopReceiveFlag = 0;

                    if (alertDialog[0] != null) {
                        if (alertDialog[0].isShowing()) {
                            alertDialog[0].dismiss();
                            runOnUiThread(() -> Toast.makeText(this, "Arquivo Corrompido!", Toast.LENGTH_SHORT).show());
                        }
                    }
                    lock.notify();
                } else {
                    if (!receivedData.equals(same)) {
                        Log.d(TAG_LOG, "RECEIVED DATA != SAME (RECEIVED)" + receivedData);
                        stopReceiveFlag = 0;

                        receivedData = same;
                        receivedSize = receivedSize + data.length;


                        //atualiza progress bar do dialogo com peso recebido;
                        if (progressBarOpenLogDialog != null) {
                            //*10 para ficar melhor a progressão do dialogo
                            runOnUiThread(() -> progressBarOpenLogDialog.setProgress(receivedSize * 10));
                        }
                        if (progressBarSaveShareLog != null) {
                            //*10 para ficar melhor a progressão do dialogo
                            runOnUiThread(() -> progressBarSaveShareLog.setProgress(receivedSize * 10));
                        }

                        if (receivedSize >= pesoTodosAsEntradas) {
                            Log.d(TAG_LOG, "MAIOR OU IGUAL RECEIVEDZISE" + receivedSize);
                            Log.d(TAG_LOG, "MAIOR OU IGUAL PESO TODAS AS ENTRADAS" + receivedData);
                            Log.d(TAG_LOG, "NOTIFICADO" + receivedData);
                            lock.notify();
                        } else {
                            Log.d(TAG_LOG, "ELSE RECEIVED SIZE" + receivedSize);
                            Log.d(TAG_LOG, "ELSE RECEIVED DATA" + receivedData);
                        }

                    } else {
                        stopReceiveFlag++;
                        Log.d(TAG_LOG, "RECEIVED DATA == SAME (RECEIVED)" + receivedData);
                        Log.d(TAG_LOG, "STOP RECEIVE FLAG" + stopReceiveFlag);
                        if (stopReceiveFlag == 50) {
                            stopReceiveFlag = 0;
                            Log.d(TAG_LOG, "\nErro no Arquivo!");
                            lock.notify();
                        }
                    }
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


    //SWIPE


    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof RecyclerAdapter.MyViewHolder) {
            if (direction == ItemTouchHelper.LEFT) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ReadTermoparDataActivity.this, R.style.DialogOpenLogStyle);
                builder.setTitle(getResources().getString(R.string.atencao_));
                builder.setMessage("Deseja realmente excluir o log: " + "\n" + logsList.get(viewHolder.getAdapterPosition()).getName() + "?");
                builder.setPositiveButton("Excluir", (dialog, which) -> {
                    deleteLogFile(logsList.get(viewHolder.getAdapterPosition()).getName());
                    logsList.remove(viewHolder.getAdapterPosition());
                    adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                });
                builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(viewHolder.getAdapterPosition()));
                builder.setCancelable(false);
                builder.create().show();

            }
        }
    }
}
