package app.br.chronlog.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.Arrays;

import app.br.chronlog.R;
import app.br.chronlog.utils.ItemClickSupport;
import app.br.chronlog.utils.RecyclerAdapter;
import app.br.chronlog.utils.TermoparLog;
import app.br.chronlog.utils.TermoparLogEntry;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

public class ReadSdDataActivity extends AppCompatActivity {

    private TermoparLog selectedLog;
    private RecyclerView logsRecyclerView;
    private RecyclerAdapter adapter;
    private AlertDialog.Builder builder;
    private MaterialButton buttonShare;
    private final int REQUISICAO_ACESSO_EXTERNO = 0;
    private ArrayList<String[]> logFilesList;

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

        logFilesList = new ArrayList<>();
        adapter = new RecyclerAdapter(null, logFilesList);
        adapter.setHasStableIds(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        logsRecyclerView.setLayoutManager(linearLayoutManager);

        logsRecyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (swipeDir == ItemTouchHelper.LEFT) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ReadSdDataActivity.this, R.style.DialogOpenLogStyle);
                    builder.setTitle(getResources().getString(R.string.atencao_));
                    builder.setMessage("Deseja realmente excluir o log: " + "\n" + logFilesList.get(viewHolder.getAdapterPosition())[0] + "?");
                    builder.setPositiveButton("Excluir", (dialog, which) -> {

                        /**TODO
                         * deletar o arquivo sd*/
                        //deleteLogFile(logsList.get(viewHolder.getAdapterPosition()).getName());
                        adapter.notifyDataSetChanged();
                    });
                    builder.setNegativeButton("Cancelar", (dialog, which) -> adapter.notifyDataSetChanged());
                    builder.create().show();
                }
                // Remove item from backing list here
                adapter.notifyDataSetChanged();
            }
        });
        itemTouchHelper.attachToRecyclerView(logsRecyclerView);
        ItemClickSupport.addTo(logsRecyclerView).setOnItemClickListener((recyclerView, position, v) -> {
            File file = read(logFilesList.get(position)[0]);
            selectedLog = configFile(file, logFilesList.get(position)[0], logFilesList.get(position)[1]);

            builder = new AlertDialog.Builder(this, R.style.DialogOpenLogStyle);
            builder.setView(R.layout.open_log_dialog);

            /**TODO
             * Enviar para o CHART
             * */
            //readFile(selectedLog, false);
        });
        ItemClickSupport.addTo(logsRecyclerView).setOnItemLongClickListener((RecyclerView recyclerView, int position, View v) -> {
            builder = new AlertDialog.Builder(ReadSdDataActivity.this, R.style.DialogOpenLogStyle);
            builder.setView(R.layout.share_save_item_dialog);

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            //TODO COMPARTILHAR
//            buttonShare = alertDialog.findViewById(R.id.btnShareLog);
//            if (buttonShare != null) {
//                buttonShare.setOnClickListener(v1 -> {
//                    Intent sharingIntent = new Intent(
//                            android.content.Intent.ACTION_SEND);
//                    sharingIntent.setType("text/plain");
//
//                    String shareBody = receivedData.replace("@05", "");
//                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
//                            "Log: " + logsList.get(position).getName());
//                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
//                    startActivity(Intent.createChooser(sharingIntent, "Compartilhar Via"));
//                });
//            }

            return true;
        });

        setFinishOnTouchOutside(true);
    }

    private TermoparLog configFile(File file, String nome, String peso) {
        String allData = file.toString();
        String[] allValues = allData.split("(\\r?\\n|\\r)");
        String lineValue;

        ArrayList<TermoparLogEntry> entries = new ArrayList<>();

        for (int i = 0; i < allValues.length; i++) {
            if (i >= 2) { // 2 = 3ª linha [valores a partir da 2 linha[0 - @ 05, 1 - Data / Hora /...]
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
        String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + fileName;
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
                        //FIXME peso não aparece
//                        int index = fileName.indexOf(" ");

                        String nome = fileName.replace(".LOG", "");
//                        String peso = fileName.substring(index);
//
//                        Log.d(TAG_LOG, "TermoparLog Nome: " + nome);
//                        Log.d(TAG_LOG, "TermoparLog Peso: " + peso);

                        logFilesList.add(new String[]{nome, "peso"});
                        adapter.notifyItemInserted(logFilesList.size());
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
}
