package app.br.chronlog.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static app.br.chronlog.activitys.DevicesActivity.modelo;
import static app.br.chronlog.utils.Utils.getFileContents;

public class ReadSdDataActivity extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private CVL0101A_TermoparLog cvl0101a_log;
    private CTL0104A_TermoparLog ctl0104a_log;
    private CTL0104B_TermoparLog ctl0104b_log;
    private RecyclerView logsRecyclerView;
    private RecyclerAdapter adapter;
    private final int REQUISICAO_ACESSO_EXTERNO = 0;
    private ArrayList<String[]> logFilesList;
    private ProgressBar progressBarContainer;
    private String mModelo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_sd_data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUISICAO_ACESSO_EXTERNO);
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mModelo = extras.getString("modelo");
        } else {
            Toast.makeText(this, "Ocorreu um erro ao recuperar o modelo!", Toast.LENGTH_SHORT).show();
            finish();
        }

        progressBarContainer = findViewById(R.id.progressBar);

        logsRecyclerView = findViewById(R.id.logsListView);
        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        logsRecyclerView.addItemDecoration(decoration);

        logFilesList = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        logsRecyclerView.setLayoutManager(linearLayoutManager);

        adapter = new RecyclerAdapter(null, logFilesList, progressBarContainer);
        adapter.setHasStableIds(true);

        logsRecyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(logsRecyclerView);

        ItemClickSupport.addTo(logsRecyclerView).setOnItemClickListener((recyclerView, position, v) -> {
            if (isFilePresent(logFilesList.get(position)[0])) { // + logFilesList.get(position)[1]
                File file = read(logFilesList.get(position)[0]); // + logFilesList.get(position)[1]
                switch (modelo) {
                    case "CVL0101A":
                        cvl0101a_log = (CVL0101A_TermoparLog) configFile(file, logFilesList.get(position)[0], logFilesList.get(position)[1]);
                        if (cvl0101a_log != null) {
                            if (cvl0101a_log.getEntries().size() > 1) {
                                runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

                                ArrayList<CVL0101A_TermoparLog> CVL0101ATermoparLog = new ArrayList<>();
                                CVL0101ATermoparLog.add(cvl0101a_log);

                                Intent intent = new Intent(this, ChartViewActivity.class);
                                intent.putExtra("modelo", mModelo);
                                intent.putParcelableArrayListExtra("selectedLog", CVL0101ATermoparLog);
                                intent.putExtra("logName", cvl0101a_log.getName());
                                startActivity(intent);

                            } else if (cvl0101a_log.getEntries().size() == 1) {
                                createCVL0101ADialog();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
                                builder.setMessage("Não existem registros no Log!");
                                builder.setTitle("Log Inválido!");
                                builder.setPositiveButton("OK", (dialog, which) -> {
                                });
                                runOnUiThread(() -> builder.create().show());
                            }
                        } else {
                            Toast.makeText(this, "Falhou ao recuperar informações do arquivo!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "CTL0104B":
                        ctl0104b_log = (CTL0104B_TermoparLog) configFile(file, logFilesList.get(position)[0], logFilesList.get(position)[1]);
                        if (ctl0104b_log != null) {
                            if (ctl0104b_log.getEntries().size() > 1) {
                                runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

                                ArrayList<CTL0104B_TermoparLog> CTL0104BTermoparLog = new ArrayList<>();
                                CTL0104BTermoparLog.add(ctl0104b_log);

                                Intent intent = new Intent(this, ChartViewActivity.class);
                                intent.putExtra("modelo", mModelo);
                                intent.putParcelableArrayListExtra("selectedLog", CTL0104BTermoparLog);
                                intent.putExtra("logName", ctl0104b_log.getName());
                                startActivity(intent);
                            } else if (ctl0104b_log.getEntries().size() == 1) {
                                createCTL0104BDialog();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
                                builder.setMessage("Não existem registros no Log!");
                                builder.setTitle("Log Inválido!");
                                builder.setPositiveButton("OK", (dialog, which) -> {
                                });
                                runOnUiThread(() -> builder.create().show());
                            }
                        } else {
                            Toast.makeText(this, "Falhou ao recuperar informações do arquivo!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                    case "CTL0104A":
                        ctl0104a_log = (CTL0104A_TermoparLog) configFile(file, logFilesList.get(position)[0], logFilesList.get(position)[1]);
                        if (ctl0104a_log != null) {
                            if (ctl0104a_log.getEntries().size() > 1) {
                                runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

                                ArrayList<CTL0104A_TermoparLog> CTL0104ATermoparLog = new ArrayList<>();
                                CTL0104ATermoparLog.add(ctl0104a_log);

                                Intent intent = new Intent(this, ChartViewActivity.class);
                                intent.putExtra("modelo", mModelo);
                                intent.putParcelableArrayListExtra("selectedLog", CTL0104ATermoparLog);
                                intent.putExtra("logName", ctl0104a_log.getName());
                                startActivity(intent);

                            } else if (ctl0104a_log.getEntries().size() == 1) {
                                createCTL0104ADialog();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
                                builder.setMessage("Não existem registros no Log!");
                                builder.setTitle("Log Inválido!");
                                builder.setPositiveButton("OK", (dialog, which) -> {
                                });
                                runOnUiThread(() -> builder.create().show());
                            }
                        } else {
                            Toast.makeText(this, "Falhou ao recuperar informações do arquivo!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }

        });
        ItemClickSupport.addTo(logsRecyclerView).setOnItemLongClickListener((RecyclerView recyclerView, int position, View v) -> {
            String fileName = logFilesList.get(position)[0];
            if (isFilePresent(fileName)) {
                String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + mModelo + "/" + fileName;
                File file = new File(path);

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, logFilesList.get(position)[0]);

                Uri logFileURI = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                sharingIntent.putExtra(Intent.EXTRA_STREAM, logFileURI);
                startActivity(Intent.createChooser(sharingIntent, "Compartilhar Via"));
            } else {
                Toast.makeText(this, "Arquivo não encontrado!", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        setFinishOnTouchOutside(true);
    }

    private void createCVL0101ADialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
        builder.setMessage("Só existe apenas um registro no Log: "
                + "\n" + "\n" +
                "Data: " + cvl0101a_log.getEntries().get(0).getData() + "\n" +
                "Horário: " + cvl0101a_log.getEntries().get(0).getHora() + "\n" +
                "vMax: " + cvl0101a_log.getEntries().get(0).getvMax() + "\n" +
                "vMed: " + cvl0101a_log.getEntries().get(0).getvMed() + "\n" +
                "vMin: " + cvl0101a_log.getEntries().get(0).getvMin() + "\n" +
                "THD: " + cvl0101a_log.getEntries().get(0).getTHD()
        );
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        builder.setTitle("Log Único!");
        runOnUiThread(() -> builder.create().show());
    }

    private void createCTL0104ADialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
        builder.setMessage("Só existe apenas um registro no Log: "
                + "\n" + "\n" +
                "Data: " + ctl0104a_log.getEntries().get(0).getData() + "\n" +
                "Horário: " + ctl0104a_log.getEntries().get(0).getHora() + "\n" +
                "T1: " + ctl0104a_log.getEntries().get(0).getT1() + "\n" +
                "T2: " + ctl0104a_log.getEntries().get(0).getT2() + "\n" +
                "T3: " + ctl0104a_log.getEntries().get(0).getT3() + "\n" +
                "T4: " + ctl0104a_log.getEntries().get(0).getT4()
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
                "Data: " + ctl0104b_log.getEntries().get(0).getData() + "\n" +
                "Horário: " + ctl0104b_log.getEntries().get(0).getHora() + "\n" +
                "T1: " + ctl0104b_log.getEntries().get(0).getT1() + "\n" +
                "T2: " + ctl0104b_log.getEntries().get(0).getT2() + "\n" +
                "T3: " + ctl0104b_log.getEntries().get(0).getT3() + "\n" +
                "T4: " + ctl0104b_log.getEntries().get(0).getT4() + "\n" +
                "M5: " + ctl0104b_log.getEntries().get(0).getM5() + "\n" +
                "M6: " + ctl0104b_log.getEntries().get(0).getM6() + "\n" +
                "M7: " + ctl0104b_log.getEntries().get(0).getM7()
        );
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        builder.setTitle("Log Único!");
        runOnUiThread(() -> builder.create().show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (logFilesList.size() == 0) {
            Toast.makeText(this, "Sem arquivos salvos!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean deleteLogFile(String myLogName) {
        File file = read(myLogName);
        return file.delete();
    }

    private Object configFile(File file, String nome, String peso) {
        String allData;
        try {
            allData = getFileContents(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return otherConfigFile(nome, peso, allData);
    }

    private Object otherConfigFile(String nome, String peso, String dataFromFile) {
        //Configura o arquivo PARCELABLE para enviar ao chart

        String[] receivedStrArray = dataFromFile.split("(\\r?\\n|\\r)");
        String lineValue;
        ArrayList<CTL0104A_TermoparLogEntry> ctl0104a_entries = new ArrayList<>();
        ArrayList<CVL0101A_TermoparLogEntry> cvl0101a_entries = new ArrayList<>();
        ArrayList<CTL0104B_TermoparLogEntry> ctl0104b_entries = new ArrayList<>();

        for (int i = 0; i < receivedStrArray.length; i++) {
            if (i >= 2) { // 2 = 3ª linha [valores a partir da 2 linha[0 - @ 05, 1 - Data / Hora /...]
                lineValue = receivedStrArray[i];

                String[] colunas = lineValue.split(" ");

                switch (mModelo) {
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
                    default:
                    case "CTL0104A":
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
        switch (mModelo) {
            case "CVL0101A":
                return new CVL0101A_TermoparLog(nome, peso, cvl0101a_entries);
            case "CTL0104B":
                return new CTL0104B_TermoparLog(nome, peso, ctl0104b_entries);
            case "CTL0104A":
            default:
                return new CTL0104A_TermoparLog(nome, peso, ctl0104a_entries);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //inicia processo de leitura dos logs no SD
        retriveFilesSD();
    }

    private File read(String filename) {
        return new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + mModelo + "/" + filename); //ja contem '.LOG' no filename
    }

    public boolean isFilePresent(String fileName) {
        String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + mModelo + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }

    private void retriveFilesSD() {
        String pathFolder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + mModelo + "/";
        File folder = new File(pathFolder);
        if (folder.exists()) {
            //Get the logFiles
            File[] filesInFolder = folder.listFiles();
            if (filesInFolder != null) {
                if (logFilesList != null && logFilesList.size() == 0) {
                    for (File file : filesInFolder) {
                        String fileName = file.getName();
                        if (fileName.contains(".LOG")) {
                            String peso = String.valueOf(file.length());
                            logFilesList.add(new String[]{fileName, peso});
                            adapter.notifyItemInserted(logFilesList.size());
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Sem arquivos salvos!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Sem arquivos salvos (armazenamento inexistente)!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUISICAO_ACESSO_EXTERNO) {
            if (resultCode != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissões Necessárias NÃO Concedidas!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    //SWIPE
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof RecyclerAdapter.MyViewHolder) {
            if (direction == ItemTouchHelper.LEFT) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ReadSdDataActivity.this, R.style.DialogOpenLogStyle);
                builder.setTitle(getResources().getString(R.string.atencao_));
                builder.setMessage("Deseja realmente excluir o log: " + "\n" + logFilesList.get(viewHolder.getAdapterPosition())[0] + "?");
                builder.setPositiveButton("Excluir", (dialog, which) -> {
                    int myPosition = viewHolder.getAdapterPosition();
                    String myLogName = logFilesList.get(myPosition)[0];
                    if (isFilePresent(myLogName)) {
                        // deletar o arquivo sd
                        deleteLogFile(myLogName);
                        logFilesList.remove(myPosition);
                        runOnUiThread(() -> {
                            logsRecyclerView.setAdapter(null);
                            logsRecyclerView.setAdapter(adapter);
                            adapter.notifyItemRemoved(myPosition);
                        });
                        Toast.makeText(this, "Excluído com sucesso!", Toast.LENGTH_SHORT).show();
                        if (logFilesList.size() < 1) {
                            finish();
                            Toast.makeText(this, "Sem arquivos salvos!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Arquivo não encontrado!", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(viewHolder.getAdapterPosition()));
                builder.setCancelable(false);
                builder.create().show();
            }
        }
    }
}
