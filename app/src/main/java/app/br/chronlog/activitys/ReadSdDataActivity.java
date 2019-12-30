package app.br.chronlog.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import app.br.chronlog.R;
import app.br.chronlog.utils.ItemClickSupport;
import app.br.chronlog.utils.RecyclerAdapter;
import app.br.chronlog.utils.RecyclerItemTouchHelper;
import app.br.chronlog.utils.TermoparLog;
import app.br.chronlog.utils.TermoparLogEntry;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static app.br.chronlog.utils.Utils.TAG_LOG;
import static app.br.chronlog.utils.Utils.getFileContents;

public class ReadSdDataActivity extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private TermoparLog selectedLog;
    private RecyclerView logsRecyclerView;
    private RecyclerAdapter adapter;
    private AlertDialog.Builder builder;
    private MaterialButton buttonShare;
    private final int REQUISICAO_ACESSO_EXTERNO = 0;
    private ArrayList<String[]> logFilesList;
    private ProgressBar progressBarContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_sd_data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUISICAO_ACESSO_EXTERNO);
            }
        }

        logsRecyclerView = findViewById(R.id.logsListView);
        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        logsRecyclerView.addItemDecoration(decoration);

        progressBarContainer = findViewById(R.id.progressBar);

        logFilesList = new ArrayList<>();
        adapter = new RecyclerAdapter(null, logFilesList, progressBarContainer);
        adapter.setHasStableIds(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        logsRecyclerView.setLayoutManager(linearLayoutManager);

        logsRecyclerView.setAdapter(adapter);


        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(logsRecyclerView);

        ItemClickSupport.addTo(logsRecyclerView).setOnItemClickListener((recyclerView, position, v) -> {
            if (isFilePresent(logFilesList.get(position)[0] + logFilesList.get(position)[1])) {
                File file = read(logFilesList.get(position)[0] + logFilesList.get(position)[1]);
                selectedLog = configFile(file, logFilesList.get(position)[0], logFilesList.get(position)[1]);

                if (selectedLog != null) {
                    if (selectedLog.getEntries().size() > 1) {
                        runOnUiThread(() -> Toast.makeText(this, "Registros resgatados com sucesso!", Toast.LENGTH_SHORT).show());

                        ArrayList<TermoparLog> termoparLog = new ArrayList<>();
                        termoparLog.add(selectedLog);

                        Intent intent = new Intent(this, ChartViewActivity.class);
                        intent.putParcelableArrayListExtra("selectedLog", termoparLog);
                        startActivity(intent);

                    } else if (selectedLog.getEntries().size() == 1) {
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
                } else {
                    Toast.makeText(this, "Falhou ao recuperar informações do arquivo!", Toast.LENGTH_SHORT).show();
                }
            }

        });
        ItemClickSupport.addTo(logsRecyclerView).setOnItemLongClickListener((RecyclerView recyclerView, int position, View v) -> {
            if (isFilePresent(logFilesList.get(position)[0] + logFilesList.get(position)[1])) {
                try {
                    String allData = getFileContents(read(logFilesList.get(position)[0] + logFilesList.get(position)[1]));
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Log: " + logFilesList.get(position)[0]);
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, allData);
                    startActivity(Intent.createChooser(sharingIntent, "Compartilhar Via"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        });

        setFinishOnTouchOutside(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (logFilesList.size() == 0) {
            Toast.makeText(this, "Sem arquivos salvos!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean deleteLogFile(String filename) {
        File file = read(filename);
        return file.delete();

    }

    private TermoparLog configFile(File file, String nome, String peso) {
        String allData;
        try {
            allData = getFileContents(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String[] allValues = allData.split("(\\r?\\n|\\r)");
        String lineValue;

        ArrayList<TermoparLogEntry> entries = new ArrayList<>();

        for (int i = 0; i < allValues.length; i++) {
            if (i > 2) { // 2 = 3ª linha [valores a partir da 2 linha[0 - @ 05, 1 - Data / Hora /...]
                lineValue = allValues[i];

                String[] colunas = lineValue.split(" ");
                colunas = Arrays.copyOf(colunas, 6);

                for (int j = 0; j < colunas.length; j++) {
                    if (colunas[i] == null || colunas[i].contains("�") || colunas[i].contains("OVUV")) {
                        colunas[i] = "OPEN";
                    }
                }
                entries.add(new TermoparLogEntry(colunas[0], colunas[1], colunas[2], colunas[3], colunas[4], colunas[5]));
            }
        }
        return new TermoparLog(nome, peso, entries);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //inicia processo de leitura dos logs no SD
        retriveFilesSD();
    }

    private File read(String filename) {
        return new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + filename); //ja contem '.LOG' no filename
    }

    public boolean isFilePresent(String fileName) {
        String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }

    private void retriveFilesSD() {
        File folder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (folder != null) {
            //Get the logFiles
            File[] filesInFolder = folder.listFiles();
            if (filesInFolder != null) {
                for (File file : filesInFolder) {
                    String fileName = file.getName();
                    if (fileName.contains(".LOG")) {
                        if (fileName.contains(" ")) {
                            int index = fileName.indexOf(" ");
                            String nome = fileName.substring(0, index);
                            String peso = fileName.substring(index);

                            Log.d(TAG_LOG, "TermoparLog on SD Nome: " + nome);
                            Log.d(TAG_LOG, "TermoparLog on SD Peso: " + peso);

                            logFilesList.add(new String[]{nome, peso});
                            adapter.notifyItemInserted(logFilesList.size());
                        } else {
                            logFilesList.add(new String[]{fileName, "?"});
                            adapter.notifyItemInserted(logFilesList.size());
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Sem Arquivos Salvos!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Falhou ao Localizar Armazenamento!", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (direction == ItemTouchHelper.LEFT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ReadSdDataActivity.this, R.style.DialogOpenLogStyle);
            builder.setTitle(getResources().getString(R.string.atencao_));
            builder.setMessage("Deseja realmente excluir o log: " + "\n" + logFilesList.get(viewHolder.getAdapterPosition())[0] + "?");
            builder.setPositiveButton("Excluir", (dialog, which) -> {

                int myPosition = viewHolder.getAdapterPosition();
                String myLogName = logFilesList.get(myPosition)[0];
                String myLogPeso = logFilesList.get(myPosition)[1];
                if (isFilePresent(myLogName + myLogPeso)) {
                    /** deletar o arquivo sd*/
                    if (deleteLogFile(myLogName + myLogPeso)) {
                        logFilesList.remove(myPosition);
                        adapter.notifyItemRemoved(myPosition);
                        Toast.makeText(this, "Excluído com sucesso!", Toast.LENGTH_SHORT).show();
                        if (logFilesList.size() < 1) {
                            finish();
                            Toast.makeText(this, "Sem arquivos salvos!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        adapter.notifyItemChanged(myPosition);
                        Toast.makeText(this, "Falhou ao excluir!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    adapter.notifyItemChanged(myPosition);
                    Toast.makeText(this, "Arquivo não encontrado!", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyItemChanged(viewHolder.getAdapterPosition()));
            builder.setCancelable(false);
            builder.create().show();
        }
    }
}
