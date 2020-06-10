package app.br.chronlog.activitys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import app.br.chronlog.R;
import app.br.chronlog.activitys.models.CTL0104A.CTL0104A_TermoparLog;
import app.br.chronlog.activitys.models.CTL0104A.CTL0104A_TermoparLogEntry;
import app.br.chronlog.activitys.models.CTL0104B.CTL0104B_TermoparLog;
import app.br.chronlog.activitys.models.CTL0104B.CTL0104B_TermoparLogEntry;
import app.br.chronlog.activitys.models.CVL0101A.CVL0101A_TermoparLog;
import app.br.chronlog.activitys.models.CVL0101A.CVL0101A_TermoparLogEntry;
import app.br.chronlog.utils.ItemClickSupport;
import app.br.chronlog.utils.RecyclerAdapter;
import app.br.chronlog.utils.RecyclerItemTouchHelper;
import app.br.chronlog.utils.Utils;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static app.br.chronlog.activitys.DevicesActivity.modelo;
import static app.br.chronlog.utils.Utils.TAG_LOG;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.myBluetoothController;
import static app.br.chronlog.utils.Utils.send;
import static java.lang.Thread.sleep;

public class ReadTermoparDataActivity extends AppCompatActivity implements ServiceConnection, SerialListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {
    private SerialService service;
    public RecyclerView logsRecyclerView;
    public static ArrayList<Object> logsList = new ArrayList<>();
    final String protocolReadSdData = "@04000000000000";
    private static String receivedData = "";
    private Object selectedLog;
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
    private Button buttonSave, buttonShare;
    private File fileInCache;

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
            builder.setView(R.layout.dialog_open_log);

            readFile(selectedLog, false);
        });
        ItemClickSupport.addTo(logsRecyclerView).setOnItemLongClickListener((RecyclerView recyclerView, int position, View v) -> {
            builder = new AlertDialog.Builder(ReadTermoparDataActivity.this, R.style.DialogOpenLogStyle);
            builder.setView(R.layout.dialog_share_save_log);

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
            switch (modelo) {
                case "CVL0101A":
                    ((TextView) Objects.requireNonNull(alertDialog[0].findViewById(R.id.logId))).setText(((CVL0101A_TermoparLog) selectedLog).getName() + " (" + ((CVL0101A_TermoparLog) selectedLog).getPeso().trim() + " bytes)");
                    break;
                case "CTL0104B":
                    ((TextView) Objects.requireNonNull(alertDialog[0].findViewById(R.id.logId))).setText(((CTL0104B_TermoparLog) selectedLog).getName() + " (" + ((CTL0104B_TermoparLog) selectedLog).getPeso().trim() + " bytes)");
                    break;
                case "CTL0104A":
                default:
                    ((TextView) Objects.requireNonNull(alertDialog[0].findViewById(R.id.logId))).setText(((CTL0104A_TermoparLog) selectedLog).getName() + " (" + ((CTL0104A_TermoparLog) selectedLog).getPeso().trim() + " bytes)");
                    break;
            }

            buttonShare = alertDialog[0].findViewById(R.id.btnShareLog);
            if (buttonShare != null) {
                buttonShare.setOnClickListener(v1 -> {
                    //antes de compartilhar limpar o cache
                    deleteCacheFiles();
                    String filename;
                    switch (modelo) {
                        case "CVL0101A":
                            filename = ((CVL0101A_TermoparLog) logsList.get(position)).getName();
                            break;
                        case "CTL0104B":
                            filename = ((CTL0104B_TermoparLog) logsList.get(position)).getName();
                            break;
                        case "CTL0104A":
                        default:
                            filename = ((CTL0104A_TermoparLog) logsList.get(position)).getName();
                            break;
                    }

                    try {
                        saveCacheFile(filename, retiraCabecalho());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");

                    switch (modelo) {
                        case "CVL0101A":
                            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ((CVL0101A_TermoparLog) logsList.get(position)).getName());
                            break;
                        case "CTL0104B":
                            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ((CTL0104B_TermoparLog) logsList.get(position)).getName());
                            break;
                        case "CTL0104A":
                        default:
                            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ((CTL0104A_TermoparLog) logsList.get(position)).getName());
                            break;
                    }

                    fileInCache = readCacheFile(filename);
                    if (fileInCache != null) {
                        Uri logFileURI = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", fileInCache);
                        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        sharingIntent.putExtra(Intent.EXTRA_STREAM, logFileURI);
                        startActivity(Intent.createChooser(sharingIntent, "Compartilhar Via"));
                    }
                });
            }
            buttonSave = alertDialog[0].findViewById(R.id.btnSaveLog);
            if (buttonSave != null) {
                buttonSave.setOnClickListener(v1 -> {
                    try {
                        String name;
                        switch (modelo) {
                            case "CVL0101A":
                                name = ((CVL0101A_TermoparLog) logsList.get(position)).getName();
                                break;
                            case "CTL0104B":
                                name = ((CTL0104B_TermoparLog) logsList.get(position)).getName();
                                break;
                            case "CTL0104A":
                            default:
                                name = ((CTL0104A_TermoparLog) logsList.get(position)).getName();
                                break;
                        }
//                        String peso = logsList.get(position).getPeso();
                        if (isModelFolderPresent()) {
                            if (isFilePresent(name)) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ReadTermoparDataActivity.this, R.style.DialogOpenLogStyle);
                                builder.setTitle("Arquivo já existe!");
                                builder.setMessage("Arquivo já existe no aparelho, deseja sobreescrever?");
                                builder.setPositiveButton("Sobreescrever", (dialog, which) -> {
                                    try {
                                        saveFileToLocalSD(name);
                                        adapter.notifyItemChanged(position);
                                        Toast.makeText(ReadTermoparDataActivity.this, "Arquivo sobreescrito com sucesso!", Toast.LENGTH_SHORT).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(ReadTermoparDataActivity.this, "Falhou ao sobreescrever o arquivo!", Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(position);
                                    }
                                });
                                builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(position));
                                builder.setCancelable(false);
                                builder.create().show();
                            } else {
                                saveFileToLocalSD(name);
                                Toast.makeText(ReadTermoparDataActivity.this, "Arquivo salvo com sucesso!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            boolean success;
                            String pathFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + modelo + File.separator;
                            File folder = new File(pathFolder);
                            success = folder.mkdirs();
                            if (!success) {
                                Toast.makeText(this, "Falhou ao criar pasta do modelo!", Toast.LENGTH_SHORT).show();
                            } else {
                                saveFileToLocalSD(name);
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

    private boolean isModelFolderPresent() {
        String pathFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + modelo + "/";
        File folder = new File(pathFolder);
        return folder.exists();
    }

    public boolean isFilePresent(String fileName) {
        String pathFile = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + modelo + "/" + fileName; // + peso
        File file = new File(pathFile);
        return file.exists();

    }

    private void saveFileToLocalSD(String fileName) throws IOException {
        if (receivedData != null) {
            String logRecebido = retiraCabecalho();
            saveFile(fileName, logRecebido);
        }
    }

    private String retiraCabecalho() {
        if (receivedData.startsWith("@05")) {
            return receivedData.substring(3);
        } else {
            return receivedData;
        }
    }

    private void saveFile(String fileName, String value) throws IOException {
        BufferedWriter file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + modelo + "/" + fileName), StandardCharsets.UTF_8)); //ja salva com ".LOG" (sem o peso)
        String[] lines = value.split("\\r?\\n|\\r");
        for (String line : lines) {
            if (!line.equals("")) {
                Log.d(TAG_LOG, "VALOR DA LINHA: " + line);
                file.append(line).append("\r\n");
            }
            //file.newLine(); nao funciona no windows
        }
        file.flush();
        file.close();
    }

    private void saveCacheFile(String fileName, String content) throws IOException {
        BufferedWriter file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/cache/" + fileName), StandardCharsets.UTF_8)); //ja salva com ".LOG" (sem o peso)
        String[] lines = content.split("\\r?\\n|\\r");
        for (String line : lines) {
            if (!line.equals("")) {
                Log.d(TAG_LOG, "VALOR DA LINHA: " + line);
                file.append(line).append("\r\n");
            }
            //file.newLine(); nao funciona no windows
        }
        file.flush();
        file.close();
    }

    private void deleteCacheFiles() {
        File folder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS + "/cache/");
        File[] cacheFiles = folder != null ? folder.listFiles() : new File[0];
        if (cacheFiles != null) {
            for (File file : cacheFiles) {
                if (file.delete()) {
                    Log.i(TAG_LOG, "Cache Deletado!");
                } else {
                    Toast.makeText(this, "Cache não deletado!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private File readCacheFile(String fileName) {
        File folder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS + "/cache/");
        if (folder != null) {
            //Get the logFiles
            File[] filesInFolder = folder.listFiles();
            if (filesInFolder != null) {
                for (File file : filesInFolder) {
                    if (file.getName().equals(fileName)) {
                        return file;
                    }
                }
                Toast.makeText(this, "Não encontrou Arquivo em Cache!", Toast.LENGTH_SHORT).show();
            }
        }
        Toast.makeText(this, "Cache Nulo!", Toast.LENGTH_SHORT).show();
        return null;
    }

    //Resgata 01 Arquivo
    //@param saveToSd = boolean para definir se é apenas para ler (salvar/compartilhar) ou seguir para tela do chart
    private void readFile(Object selectedLog, boolean saveToSD) {
        receivedData = "";
        receivedSize = 0;

        //interface no item para informar que estamos tentando ler o arquivo
        if (!saveToSD) {
            progressBarItem.setVisibility(View.VISIBLE);
        }
        String protocolReadFileData;
        int max;
        switch (modelo) {
            case "CVL0101A":
                protocolReadFileData = "@05" + ((CVL0101A_TermoparLog) selectedLog).getName() + "0000";
                max = Integer.parseInt(((CVL0101A_TermoparLog) selectedLog).getPeso().trim()) * 10;
                break;
            case "CTL0104B":
                protocolReadFileData = "@05" + ((CTL0104B_TermoparLog) selectedLog).getName() + "0000";
                max = Integer.parseInt(((CTL0104B_TermoparLog) selectedLog).getPeso().trim()) * 10;
                break;
            case "CTL0104A":
            default:
                protocolReadFileData = "@05" + ((CTL0104A_TermoparLog) selectedLog).getName() + "0000";
                max = Integer.parseInt(((CTL0104A_TermoparLog) selectedLog).getPeso().trim()) * 10;
                break;
        }

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

                    progressBarOpenLogDialog.setMax(max);
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
                    progressBarSaveShareLog.setMax(max);
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
                        //terminou de receber e não foi cancelado
                        if (!saveToSD) {
                            if (!aberturaCancelada) {
                                configFileReceived();
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
    private void configFileReceived() {
        receivedStrArray = receivedData.split("(\\r?\\n|\\r)");
        String lineValue;
        ArrayList<CTL0104A_TermoparLogEntry> ctl0104a_entries = new ArrayList<>();
        ArrayList<CTL0104B_TermoparLogEntry> ctl0104b_entries = new ArrayList<>();
        ArrayList<CVL0101A_TermoparLogEntry> cvl0101a_entries = new ArrayList<>();
        for (int i = 0; i < receivedStrArray.length; i++) {
            if (i >= 2) { // 2 = 3ª linha [valores a partir da 2 linha[0 - @ 05, 1 - Data / Hora /...]
                lineValue = receivedStrArray[i];
                String[] colunas = lineValue.split(" ");
                switch (modelo) {
                    case "CVL0101A":
                        colunas = Arrays.copyOf(colunas, 6);
                        for (int j = 0; j < colunas.length; j++) {
                            if (colunas[j] == null || colunas[j].contains("�") || colunas[j].contains("OVUV")) {
                                colunas[j] = "OPEN";
                            }
                        }
                        cvl0101a_entries.add(new CVL0101A_TermoparLogEntry(colunas[0], colunas[1], colunas[2], colunas[3], colunas[4], colunas[5]));
                        break;
                    case "CTL0104B":
                        colunas = Arrays.copyOf(colunas, 9);
                        for (int j = 0; j < colunas.length; j++) {
                            if (colunas[j] == null || colunas[j].contains("�") || colunas[j].contains("OVUV")) {
                                colunas[j] = "OPEN";
                            }
                        }
                        ctl0104b_entries.add(new CTL0104B_TermoparLogEntry(colunas[0], colunas[1], colunas[2], colunas[3], colunas[4], colunas[5], colunas[6], colunas[7], colunas[8]));
                        break;
                    case "CTL0104A":
                    default:
                        colunas = Arrays.copyOf(colunas, 6);
                        for (int j = 0; j < colunas.length; j++) {
                            if (colunas[j] == null || colunas[j].contains("�") || colunas[j].contains("OVUV")) {
                                colunas[j] = "OPEN";
                            }
                        }
                        ctl0104a_entries.add(new CTL0104A_TermoparLogEntry(colunas[0], colunas[1], colunas[2], colunas[3], colunas[4], colunas[5]));
                        break;
                }
            }
        }
        switch (modelo) {
            case "CVL0101A":
                if (cvl0101a_entries.size() > 1) {
                    runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

                    ((CVL0101A_TermoparLog) this.selectedLog).setEntries(cvl0101a_entries);

                    ArrayList<CVL0101A_TermoparLog> CVL0101A_TermoparLog = new ArrayList<>();
                    CVL0101A_TermoparLog.add(((CVL0101A_TermoparLog) this.selectedLog));

                    Intent intent = new Intent(this, ChartViewActivity.class);
                    intent.putExtra("modelo", modelo);
                    intent.putParcelableArrayListExtra("selectedLog", CVL0101A_TermoparLog);
                    intent.putExtra("logName", ((CVL0101A_TermoparLog) this.selectedLog).getName());
                    startActivity(intent);
                } else if (cvl0101a_entries.size() == 1) {
                    createCVL0101ADialog();
                } else {
                    semRegistrosDialog();
                }
                break;
            case "CTL0104B":
                if (ctl0104b_entries.size() > 1) {
                    runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

                    ((CTL0104B_TermoparLog) this.selectedLog).setEntries(ctl0104b_entries);

                    ArrayList<CTL0104B_TermoparLog> CTL0104BTermoparLog = new ArrayList<>();
                    CTL0104BTermoparLog.add(((CTL0104B_TermoparLog) this.selectedLog));

                    Intent intent = new Intent(this, ChartViewActivity.class);
                    intent.putExtra("modelo", modelo);
                    intent.putParcelableArrayListExtra("selectedLog", CTL0104BTermoparLog);
                    intent.putExtra("logName", ((CTL0104B_TermoparLog) this.selectedLog).getName());
                    startActivity(intent);

                } else if (ctl0104b_entries.size() == 1) {
                    createCTL0104BDialog();
                } else {
                    semRegistrosDialog();
                }
                break;
            case "CTL0104A":
            default:
                if (ctl0104a_entries.size() > 1) {
                    runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

                    ((CTL0104A_TermoparLog) this.selectedLog).setEntries(ctl0104a_entries);

                    ArrayList<CTL0104A_TermoparLog> CTL0104ATermoparLog = new ArrayList<>();
                    CTL0104ATermoparLog.add(((CTL0104A_TermoparLog) this.selectedLog));

                    Intent intent = new Intent(this, ChartViewActivity.class);
                    intent.putExtra("modelo", modelo);
                    intent.putParcelableArrayListExtra("selectedLog", CTL0104ATermoparLog);
                    intent.putExtra("logName", ((CTL0104A_TermoparLog) this.selectedLog).getName());
                    startActivity(intent);

                } else if (ctl0104a_entries.size() == 1) {
                    createCTL0104ADialog();
                } else {
                    semRegistrosDialog();
                }
                break;
        }


    }

    private void createCVL0101ADialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
        builder.setMessage("Só existe apenas um registro no Log: "
                + "\n" + "\n" +
                "Data: " + ((CVL0101A_TermoparLog) selectedLog).getEntries().get(0).getData() + "\n" +
                "Horário: " + ((CVL0101A_TermoparLog) selectedLog).getEntries().get(0).getHora() + "\n" +
                "vMax: " + ((CVL0101A_TermoparLog) selectedLog).getEntries().get(0).getvMax() + "\n" +
                "vMed: " + ((CVL0101A_TermoparLog) selectedLog).getEntries().get(0).getvMed() + "\n" +
                "vMin: " + ((CVL0101A_TermoparLog) selectedLog).getEntries().get(0).getvMin() + "\n" +
                "THD: " + ((CVL0101A_TermoparLog) selectedLog).getEntries().get(0).getTHD() + "\n"
        );
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        builder.setTitle("Log Único!");
        runOnUiThread(() -> builder.create().show());
    }

    private void semRegistrosDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
        builder.setMessage("Não existem registros no Log!");
        builder.setTitle("Log Inválido!");
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        runOnUiThread(() -> builder.create().show());
    }

    private void createCTL0104ADialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
        builder.setMessage("Só existe apenas um registro no Log: "
                + "\n" + "\n" +
                "Data: " + ((CTL0104A_TermoparLog) selectedLog).getEntries().get(0).getData() + "\n" +
                "Horário: " + ((CTL0104A_TermoparLog) selectedLog).getEntries().get(0).getHora() + "\n" +
                "T1: " + ((CTL0104A_TermoparLog) selectedLog).getEntries().get(0).getT1() + "\n" +
                "T2: " + ((CTL0104A_TermoparLog) selectedLog).getEntries().get(0).getT2() + "\n" +
                "T3: " + ((CTL0104A_TermoparLog) selectedLog).getEntries().get(0).getT3() + "\n" +
                "T4: " + ((CTL0104A_TermoparLog) selectedLog).getEntries().get(0).getT4()
        );
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        builder.setTitle("Log Único!");
        runOnUiThread(() -> builder.create().show());
    }

    private void createCTL0104BDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
        builder.setMessage("Só existe apenas um registro no Log: "
                + "\n" + "\n" +
                "Data: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getData() + "\n" +
                "Horário: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getHora() + "\n" +
                "T1: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getT1() + "\n" +
                "T2: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getT2() + "\n" +
                "T3: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getT3() + "\n" +
                "T4: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getT4() + "\n" +
                "M5: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getM5() + "\n" +
                "M6: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getM6() + "\n" +
                "M7: " + ((CTL0104B_TermoparLog) selectedLog).getEntries().get(0).getM7()
        );
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        builder.setTitle("Log Único!");
        runOnUiThread(() -> builder.create().show());
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
                        switch (modelo) {
                            case "CVL0101A":
                                logsList.add(new CVL0101A_TermoparLog(finalNome, finalPeso, null)); //recebeu um log
                                break;
                            case "CTL0104B":
                                logsList.add(new CTL0104B_TermoparLog(finalNome, finalPeso, null)); //recebeu um log
                                break;
                            case "CTL0104A":
                            default:
                                logsList.add(new CTL0104A_TermoparLog(finalNome, finalPeso, null)); //recebeu um log
                                break;
                        }
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

    public void swapItems(ArrayList<Object> items) {
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
                int pesoTodosAsEntradas;
                switch (modelo) {
                    case "CVL0101A":
                        pesoTodosAsEntradas = Integer.parseInt(((CVL0101A_TermoparLog) selectedLog).getPeso().trim());
                        break;
                    case "CTL0104B":
                        pesoTodosAsEntradas = Integer.parseInt(((CTL0104B_TermoparLog) selectedLog).getPeso().trim());
                        break;
                    case "CTL0104A":
                    default:
                        pesoTodosAsEntradas = Integer.parseInt(((CTL0104A_TermoparLog) selectedLog).getPeso().trim());
                        break;
                }
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
                String name;
                switch (modelo) {
                    case "CVL0101A":
                        name = ((CVL0101A_TermoparLog) logsList.get(viewHolder.getAdapterPosition())).getName();
                        break;
                    case "CTL0104B":
                        name = ((CTL0104B_TermoparLog) logsList.get(viewHolder.getAdapterPosition())).getName();
                        break;
                    case "CTL0104A":
                    default:
                        name = ((CTL0104A_TermoparLog) logsList.get(viewHolder.getAdapterPosition())).getName();
                        break;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(ReadTermoparDataActivity.this, R.style.DialogOpenLogStyle);
                builder.setTitle(getResources().getString(R.string.atencao_));
                builder.setMessage("Deseja realmente excluir o log: " + "\n" + name + "?");
                builder.setPositiveButton("Excluir", (dialog, which) -> {
                    //deletar arquivo termopar
                    deleteLogFile(name);
                    logsList.remove(viewHolder.getAdapterPosition());
                    adapter.notifyDataSetChanged();
                });
                builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(viewHolder.getAdapterPosition()));
                builder.setCancelable(false);
                builder.create().show();
            }
        }
    }
}
