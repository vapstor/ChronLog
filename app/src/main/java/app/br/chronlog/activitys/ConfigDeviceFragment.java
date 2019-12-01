package app.br.chronlog.activitys;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import app.br.chronlog.R;

import static app.br.chronlog.activitys.MainActivity.universalBtController;

public class ConfigDeviceFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private String mYear, mMonth, mDay, mHora, mMinute, mSecond, modoTermopar;
    private ProgressBar progressBar;
    private Switch switchData, switchHorario;
    private EditText horarioInput, dataInput, aquisitionInput;
    private TextWatcher dataInputListener, horaInputListener;
    private View btnSend;
    private String[] todosModosTermopar;

    public ConfigDeviceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        todosModosTermopar = getResources().getStringArray(R.array.modosTermopar);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            ((TextView) Objects.requireNonNull(getActivity()).findViewById(R.id.appBar).findViewById(R.id.titleBar)).setText(getArguments().getString("device"));
        }

        horarioInput = Objects.requireNonNull(getActivity()).findViewById(R.id.horaInput);
        dataInput = getActivity().findViewById(R.id.dataInput);
        aquisitionInput = getActivity().findViewById(R.id.aquisitionInput);

        setDataListener();
        dataInput.addTextChangedListener(dataInputListener);
        setHorarioListener();
        horarioInput.addTextChangedListener(horaInputListener);

        btnSend = getActivity().findViewById(R.id.sendButton);
        btnSend.setOnClickListener((v) -> sendInfos());

        switchData = Objects.requireNonNull(getActivity()).findViewById(R.id.syncData);
        switchHorario = getActivity().findViewById(R.id.syncHora);
        switchData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                dataInput.setEnabled(false);
                syncData();
            } else {
                dataInput.setEnabled(true);
                dataInput.setText("");
                dataInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });


        Spinner spinner = Objects.requireNonNull(getActivity()).findViewById(R.id.spinnerModoAquisicao);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(Objects.requireNonNull(getContext()), R.array.modosTermopar, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

    private void syncData() {
        final Calendar c = Calendar.getInstance();
        mYear = String.valueOf(c.get(Calendar.YEAR));
        mMonth = String.valueOf(c.get(Calendar.MONTH)+1); //calendario começa do 0
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config_device, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iconBar:
                ((AppCompatActivity) Objects.requireNonNull(getContext())).getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
            case R.id.sendButton:
                sendInfos();
                break;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        modoTermopar = todosModosTermopar[pos];
    }

    public void onNothingSelected(AdapterView<?> parent) {
        modoTermopar = "Modo";
    }

    private void setDataListener() {
        dataInputListener = new TextWatcher() {
            private String current = "";
            private String ddmmyyyy = "ddmmyyyy";
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

                    if (clean.length() < 8) {
                        clean = clean + hhmmss.substring(clean.length());
                    } else {
                        //This part makes sure that when we finish entering numbers
                        //the date is correct, fixing it otherwise
                        int hours = Integer.parseInt(clean.substring(0, 2));
                        int minutes = Integer.parseInt(clean.substring(2, 4));
                        int seconds = Integer.parseInt(clean.substring(4, 6));

                        minutes = (minutes < 1) ? 1 : minutes > 59 ? 00 : minutes;
                        cal.set(Calendar.MINUTE, minutes);
                        seconds = seconds < 1 ? 1 : seconds > 59 ? 00 : minutes;
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

    private void sendInfos() {
        String infoHorario = horarioInput.getText().toString();
        String infoData = dataInput.getText().toString();
        String infoTempoAquisicao = aquisitionInput.getText().toString();

        String protocolSetData, protocolSetHorario, protocolConfiguration;

        if (!infoData.equals("") && !infoData.equals("dd/mm/yyyy")) {
            /**
             * @01YYYYMMDDCRLF YYYY year, MM month, DD day CR carriage return, LF line feed
             * */
            configDateToSend();
            protocolSetData = "@01" + mYear + mMonth + mDay + "0000";
            new Thread(() -> universalBtController.send(protocolSetData)).start();
        }
        if (!infoHorario.equals("") && !infoHorario.equals("hh:mm:ss")) {
            /**
             * @02HHMMSSRRCRLF HH hour, MM minute, SS second, RR reserved for future
             * */
            configHoursToSend();
            protocolSetHorario = "@02" + mHora + mMinute + mSecond + "00" + "0000";
            new Thread(() -> universalBtController.send(protocolSetHorario)).start();

        }
        if (!infoTempoAquisicao.equals("") && infoTempoAquisicao.length() != 3 && !modoTermopar.equals("Modo")) {
            /**
            *@03TTNNNRRRCRLF TT termocouple type, NNN acquisition time in seconds, RRR reserved for future*
            * */
//            configTempoAquisicaoToSend();
            protocolConfiguration = "@03" + modoTermopar + infoTempoAquisicao + "000"+"0000";
            new Thread(() -> universalBtController.send(protocolConfiguration)).start();

        }
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

}