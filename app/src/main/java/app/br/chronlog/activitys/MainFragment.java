package app.br.chronlog.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.Objects;

import app.br.chronlog.R;

import static app.br.chronlog.utils.Utils.startActivityWithExplosion;


public class MainFragment extends Fragment implements View.OnClickListener {
    private ProgressBar progressBar;
    private View btnGerenciarDados;
    private ImageButton syncButton;
    private Button configuraDeviceBtn, analisaDadosBtn, eepromBtn;
    private View appBarView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, container, false);
        btnGerenciarDados = view.findViewById(R.id.gerenciarDadosBtn);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appBarView = getActivity().findViewById(R.id.appBar);
        ((TextView) appBarView.findViewById(R.id.titleBar)).setText(R.string.chronlog);
        syncButton = appBarView.findViewById(R.id.iconBar);
        syncButton.setImageDrawable(getActivity().getDrawable(R.drawable.baseline_sync_alt_white_24dp));

        progressBar = appBarView.findViewById(R.id.progressBarAppBar);

        configuraDeviceBtn = view.findViewById(R.id.configDeviceBtn);
        analisaDadosBtn = view.findViewById(R.id.analiseDeDadosBtn);
        eepromBtn = view.findViewById(R.id.eeppromBtn);

        syncButton.setOnClickListener(this);
        configuraDeviceBtn.setOnClickListener(this);
        analisaDadosBtn.setOnClickListener(this);
        eepromBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        FragmentTransaction transaction;
        switch (v.getId()) {
            case R.id.configDeviceBtn:
                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack
                assert getFragmentManager() != null;
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new ConfigDeviceFragment());
                transaction.addToBackStack(null);
                // Commit the transaction
                transaction.commit();
                break;
            case R.id.iconBar:
                assert getFragmentManager() != null;
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new DevicesFragment());
                transaction.addToBackStack(null);
                // Commit the transaction
                transaction.commit();
                break;
            case R.id.gerenciarDadosBtn:
                gerenciarDados();
                break;
            case R.id.analiseDeDadosBtn:
                startActivityWithExplosion(Objects.requireNonNull(getContext()), new Intent(getActivity(), ChartViewActivity.class));
                break;
            case R.id.eeppromBtn:
                Toast.makeText(getContext(), "EEPROM [?]", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(getContext(), "erro", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void gerenciarDados() {
        Toast.makeText(getContext(), "Abriria Modal..", Toast.LENGTH_SHORT).show();
//        SerialService service = new SerialService();
//        service.connect();
    }


}