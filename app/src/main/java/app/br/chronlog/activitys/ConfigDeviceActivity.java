package app.br.chronlog.activitys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import app.br.chronlog.R;
import app.br.chronlog.utils.Utils;
import app.br.chronlog.utils.bluetooth.BluetoothController;
import app.br.chronlog.utils.bluetooth.SerialListener;
import app.br.chronlog.utils.bluetooth.SerialService;

import static app.br.chronlog.activitys.DevicesActivity.deviceName;
import static app.br.chronlog.utils.Utils.TAG_LOG;
import static app.br.chronlog.utils.Utils.isDeviceConnected;
import static app.br.chronlog.utils.Utils.myBluetoothController;
import static app.br.chronlog.utils.Utils.send;
import static app.br.chronlog.utils.Utils.serialSocket;
import static app.br.chronlog.utils.Utils.setStatus;
import static app.br.chronlog.utils.bluetooth.BluetoothController.recebido;
import static java.lang.Thread.sleep;

public class ConfigDeviceActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, SerialListener, ServiceConnection {
    private String mYear, mMonth, mDay, mHora, mMinute, mSecond, modoTermopar;
    private ProgressBar progressBar;
    private Switch switchData, switchHorario;
    private EditText horarioInput, dataInput, aquisitionInput;
    private TextWatcher dataInputListener, horaInputListener;
    private View btnCOnfigurarTermopar;
    private String[] todosModosTermopar;
    final public static Object lockObject = new Object();
    private Thread mySendThread;
    private Button btnConfigurarData, btnConfigurarHorario;
    private ImageButton refreshButton;
    private TextView statusView;

    private SerialService service;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_device);

        if (myBluetoothController != null) {
            myBluetoothController.setActivity(this);
        } else {
            myBluetoothController = new BluetoothController(this);
        }

        //titulo
        ((TextView) findViewById(R.id.titleBar)).setText(R.string.ajustes);
        progressBar = findViewById(R.id.progressBarAppBar);
        statusView = findViewById(R.id.status);
        refreshButton = findViewById(R.id.iconBar);

        todosModosTermopar = getResources().getStringArray(R.array.modosTermopar);

        horarioInput = Objects.requireNonNull(this).findViewById(R.id.horaInput);
        dataInput = this.findViewById(R.id.dataInput);
        aquisitionInput = this.findViewById(R.id.aquisitionInput);

        setDataListener();
        dataInput.addTextChangedListener(dataInputListener);
        setHorarioListener();
        horarioInput.addTextChangedListener(horaInputListener);

        btnCOnfigurarTermopar = this.findViewById(R.id.configTermoparBtn);
        btnCOnfigurarTermopar.setOnClickListener((v) -> sendInfoTermopar());

        btnConfigurarData = this.findViewById(R.id.configDataBtn);
        btnConfigurarData.setOnClickListener((v) -> sendInfoTermopar());

        btnConfigurarHorario = this.findViewById(R.id.configHorarioBtn);
        btnConfigurarHorario.setOnClickListener((v) -> sendInfoTermopar());

        switchData = Objects.requireNonNull(this).findViewById(R.id.syncData);
        switchHorario = this.findViewById(R.id.syncHora);
        switchData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                dataInput.setEnabled(false);
                syncData();
            } else {
                dataInput.setEnabled(true);
                dataInput.setText("");
                dataInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });
        switchHorario.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                horarioInput.setEnabled(false);
                syncHorario();
            } else {
                horarioInput.setEnabled(true);
                horarioInput.setText("");
                horarioInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });


        Spinner spinner = Objects.requireNonNull(this).findViewById(R.id.spinnerModoAquisicao);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.modosTermopar, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iconBar:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.configTermoparBtn:
                sendInfoTermopar();
                break;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        modoTermopar = todosModosTermopar[pos];
    }

    public void onNothingSelected(AdapterView<?> parent) {
        modoTermopar = "Modo";
    }


    private void syncData() {
        final Calendar c = Calendar.getInstance();
        mYear = String.valueOf(c.get(Calendar.YEAR));
        mMonth = String.valueOf(c.get(Calendar.MONTH) + 1); //calendario começa do 0
        if (mMonth.length() == 1) {
            mMonth = "0" + mMonth;
        }
        mDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
        if (mDay.length() == 1) {
            mDay = "0" + mDay;
        }
        dataInput.setText(mDay + "/" + mMonth + "/" + mYear);
    }

    private void syncHorario() {
        final Calendar c = Calendar.getInstance();
        mHora = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
        if (mHora.length() == 1) {
            mHora = "0" + mHora;
        }
        mMinute = String.valueOf(c.get(Calendar.MINUTE));
        if (mMinute.length() == 1) {
            mMinute = "0" + mMinute;
        }
        mSecond = String.valueOf(c.get(Calendar.SECOND));
        if (mSecond.length() == 1) {
            mSecond = "0" + mSecond;
        }
        horarioInput.setText(mHora + ":" + mMinute + ":" + mSecond);
    }

    private void setDataListener() {
        dataInputListener = new TextWatcher() {
            private String current = "";
            private String ddmmyyyy = "ddmmaaaa";
            private Calendar cal = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]|\\.", "");
                    String cleanC = current.replaceAll("[^\\d.]|\\.", "");

                    int cl = clean.length();
                    int sel = cl;
                    for (int i = 2; i <= cl && i < 6; i += 2) {
                        sel++;
                    }
                    //Fix for pressing delete next to a forward slash
                    if (clean.equals(cleanC)) sel--;

                    if (clean.length() < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length());
                    } else {
                        //This part makes sure that when we finish entering numbers
                        //the date is correct, fixing it otherwise
                        int day = Integer.parseInt(clean.substring(0, 2));
                        int mon = Integer.parseInt(clean.substring(2, 4));
                        int year = Integer.parseInt(clean.substring(4, 8));

                        mon = mon < 1 ? 1 : mon > 12 ? 12 : mon;
                        cal.set(Calendar.MONTH, mon - 1);
                        year = (year < 1900) ? 1900 : (year > 2100) ? 2100 : year;
                        cal.set(Calendar.YEAR, year);
                        // ^ first set year for the line below to work correctly
                        //with leap years - otherwise, date e.g. 29/02/2012
                        //would be automatically corrected to 28/02/2012

                        day = (day > cal.getActualMaximum(Calendar.DATE)) ? cal.getActualMaximum(Calendar.DATE) : day;
                        clean = String.format("%02d%02d%02d", day, mon, year);
                    }

                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 8));

                    sel = sel < 0 ? 0 : sel;
                    current = clean;
                    dataInput.setText(current);
                    dataInput.setSelection(sel < current.length() ? sel : current.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    private void setHorarioListener() {
        horaInputListener = new TextWatcher() {
            private String current = "";
            private String hhmmss = "hhmmss";
            private Calendar cal = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]|\\.", "");
                    String cleanC = current.replaceAll("[^\\d.]|\\.", "");

                    int cl = clean.length();
                    int sel = cl;
                    for (int i = 2; i <= cl && i < 6; i += 2) {
                        sel++;
                    }
                    //Fix for pressing delete next to a forward slash
                    if (clean.equals(cleanC)) sel--;

                    if (clean.length() < 6) {
                        clean = clean + hhmmss.substring(clean.length());
                    } else {
                        //This part makes sure that when we finish entering numbers
                        //the date is correct, fixing it otherwise
                        int hours = Integer.parseInt(clean.substring(0, 2));
                        int minutes = Integer.parseInt(clean.substring(2, 4));
                        int seconds = Integer.parseInt(clean.substring(4, 6));

                        minutes = (minutes < 1) ? 0 : minutes > 59 ? 00 : minutes;
                        cal.set(Calendar.MINUTE, minutes);
                        seconds = seconds < 1 ? 0 : seconds > 59 ? 00 : minutes;
                        cal.set(Calendar.SECOND, seconds);
                        // ^ first set year for the line below to work correctly
                        //with leap years - otherwise, date e.g. 29/02/2012
                        //would be automatically corrected to 28/02/2012

                        hours = (hours > cal.getActualMaximum(Calendar.HOUR_OF_DAY)) ? cal.getActualMaximum(Calendar.HOUR_OF_DAY) : hours;
                        clean = String.format(Locale.getDefault(), "%02d%02d%02d", hours, minutes, seconds);
                    }

                    clean = String.format(Locale.getDefault(), "%s:%s:%s", clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 6));

                    sel = sel < 0 ? 0 : sel;
                    current = clean;
                    horarioInput.setText(current);
                    horarioInput.setSelection(sel < current.length() ? sel : current.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }


    @SuppressWarnings("UnstableApiUsage")
    public void sendInfoTermopar() {
        new Thread(() -> {
            recebido = false;
            String infoData = dataInput.getText().toString();

            if (!infoData.equals("") && !infoData.contains("d") && !infoData.contains("m") && !infoData.contains("a")) {

                String infoHorario = horarioInput.getText().toString();
                String infoTempoAquisicao = aquisitionInput.getText().toString();
                final String[] protocolSetData = new String[1];
                final String[] protocolSetHorario = new String[1];
                final String[] protocolConfiguration = new String[1];


                ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
                ListenableFuture<Boolean> asyncTask = executor.submit(() -> {
                    runOnUiThread(() -> {
                        btnConfigurarData.setEnabled(false);
                        btnConfigurarData.setText(R.string.configurando___);
                    });

                    /**
                     * @01YYYYMMDDCRLF YYYY year, MM month, DD day CR carriage return, LF line feed
                     * */
                    Log.d(TAG_LOG, "infos data:" + infoData);
                    configDateToSend();
                    protocolSetData[0] = "@01" + mYear + mMonth + mDay + "0000";
                    Log.d(TAG_LOG, "mySendFlag == 0 [Config Class]");
                    send(protocolSetData[0], this, this);

                    sleep(2000);

                    return recebido;
                });


                Futures.addCallback(asyncTask, new FutureCallback<Boolean>() {
                    public void onSuccess(Boolean result) {
                        if (result) {
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "Configurado com Sucesso!", Toast.LENGTH_SHORT).show();
                                btnConfigurarData.setEnabled(true);
                                btnConfigurarData.setText(R.string.configurar);
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "Falhou ao Configurar!", Toast.LENGTH_SHORT).show();
                                btnConfigurarData.setEnabled(true);
                                btnConfigurarData.setText(R.string.configurar);
                            });
                        }
                        System.out.println(result);
                        executor.shutdown();
                    }

                    public void onFailure(Throwable thrown) {
                        recebido = true;
                        //erro
                        thrown.printStackTrace();
                        executor.shutdown();
                    }
                }, executor);

                try {
                    Boolean result = asyncTask.get();
                    System.out.println(result);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                //
//        synchronized (lockObject) {
//            try {
//
//                Log.d(TAG_LOG, "wait data");
//                lockObject.wait();
//                Log.d(TAG_LOG, "libera data");
//
//                new Thread(() -> {
//                    if (!infoHorario.equals("") && !infoHorario.equals("hh:mm:ss")) {
//                        Log.d(TAG_LOG, "infos horario:" + infoHorario);
//                        /**
//                         * @02HHMMSSRRCRLF HH hour, MM minute, SS second, RR reserved for future
//                         * */
//                        configHoursToSend();
//                        protocolSetHorario[0] = "@02" + mHora + mMinute + mSecond + "00" + "0000";
//                        Log.d(TAG_LOG, "mySendFlag == 0 [Config Class]");
//                        mySendFlag = 0;
//                        universalBtController.send(protocolSetHorario[0]);
//                    }
//                }).start();
//
//                Log.d(TAG_LOG, "wait horario");
//                lockObject.wait();
//                Log.d(TAG_LOG, "libera horario");
//
//                new Thread(() -> {
//                    if (!infoTempoAquisicao.equals("") && infoTempoAquisicao.length() != 3 && !modoTermopar.equals("Modo")) {
//                        Log.d(TAG_LOG, "infos termopar:" + infoTempoAquisicao);
//                        /**
//                         *@03TTNNNRRRCRLF TT termocouple type, NNN acquisition time in seconds, RRR reserved for future*
//                         * */
//                        //configTempoAquisicaoToSend();
//                        protocolConfiguration[0] = "@03" + modoTermopar + infoTempoAquisicao + "000" + "0000";
//                        Log.d(TAG_LOG, "mySendFlag == 0 [Config Class]");
//                        mySendFlag = 0;
//                        universalBtController.send(protocolConfiguration[0]);
//                    }
//                }).start();
//
//                Log.d(TAG_LOG, "wait termopar");
//                lockObject.wait();
//                Log.d(TAG_LOG, "libera termopar");
//                Log.d(TAG_LOG, "finalizou envios");
//
//                Objects.requireNonNull(this).runOnUiThread(() -> Toast.makeText(getContext(), "Ajustes no termopar efetuados com sucesso!", Toast.LENGTH_SHORT).show());
//            } catch (InterruptedException ex) {
//                ex.printStackTrace();
//            }
//        }
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Data Inválida!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
//    private void configTempoAquisicaoToSend(String s) {
//        if
//    }

    private void configDateToSend() {
        String inputValue = dataInput.getText().toString().replace("/", "");
        mDay = inputValue.substring(0, 2);
        mMonth = inputValue.substring(2, 4);
        mYear = inputValue.substring(4);
    }

    private void configHoursToSend() {
        String inputValue = horarioInput.getText().toString().replace(":", "");
        mHora = inputValue.substring(0, 2);
        mMinute = inputValue.substring(2, 4);
        mSecond = inputValue.substring(4);
    }


    @Override
    protected void onStart() {
        if (service != null) {
            service.attach(this);
        } else {
            bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
        }

        if (isDeviceConnected == Utils.Connected.True) {
            statusView.setText(deviceName);
        }

        super.onStart();
    }

    @Override
    public void onStop() {
//        try {
//            this.unbindService(this);
//        } catch (Exception ignored) {
//        }
//        if (!this.isChangingConfigurations())
//            service.detach();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (deviceIsConnected != Utils.Connected.False)
//            disconnect();
//        this.stopService(new Intent(this, SerialService.class));
    }

    private void disconnect() {
        isDeviceConnected = Utils.Connected.False;
        service.disconnect();
        serialSocket.disconnect();
        serialSocket = null;
        startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }


    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
//        deviceIsConnected = Utils.Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        if (!e.getMessage().contains("ja conectado")) {
            setStatus("Conexão Falhou", this);
            disconnect();
        }
    }

    @Override
    public void onSerialRead(byte[] data) {
        if (!recebido) {
            Toast.makeText(this, "READ", Toast.LENGTH_SHORT).show();
            Log.d(TAG_LOG, "recebeu: " + new String(data));
            recebido = true;
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        setStatus("Conexão Perdida", this);
        disconnect();
    }
}
