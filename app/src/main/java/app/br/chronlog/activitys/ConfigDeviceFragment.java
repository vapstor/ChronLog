package app.br.chronlog.activitys;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Objects;

import app.br.chronlog.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {//@link ConfigDeviceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConfigDeviceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfigDeviceFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private int mYear, mMonth, mDay, mHora, mMinute, mSecond;
    private ProgressBar progressBar;
    private Switch switchData, switchHorario;
    private final Calendar c = Calendar.getInstance();
    private boolean sync;
    private EditText horarioInput, dataInput;

    public ConfigDeviceFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ConfigDeviceFragment newInstance(String device) {
        ConfigDeviceFragment fragment = new ConfigDeviceFragment();
        Bundle args = new Bundle();
        args.putString("device", device);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            ((TextView) Objects.requireNonNull(getActivity()).findViewById(R.id.appBar).findViewById(R.id.titleBar)).setText(getArguments().getString("device"));
        }

        horarioInput = Objects.requireNonNull(getActivity()).findViewById(R.id.horaInput);
        dataInput = getActivity().findViewById(R.id.dataInput);

        switchData = Objects.requireNonNull(getActivity()).findViewById(R.id.syncData);
        switchHorario = getActivity().findViewById(R.id.syncHora);
        switchData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                syncData();
            } else {
                dataInput.setText("");
            }
        });
        switchHorario.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                syncHorario();
            } else {
                horarioInput.setText("");
            }
        });


        Spinner spinner = getActivity().findViewById(R.id.spinnerModoAquisicao);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.planets_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

    private void syncData() {
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        EditText dataInput = getActivity().findViewById(R.id.dataInput);
        dataInput.setText(mDay+":"+mMonth+"/"+mYear);
        sync = true;
    }

    private void syncHorario() {
        mHora = c.get(Calendar.HOUR);
        mMinute = c.get(Calendar.MINUTE);
        mSecond = c.get(Calendar.SECOND);
        EditText horaInput = getActivity().findViewById(R.id.horaInput);
        horaInput.setText(mHora+":"+mMinute+":"+mSecond);
        sync = true;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                ((AppCompatActivity) getContext()).getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }
}
