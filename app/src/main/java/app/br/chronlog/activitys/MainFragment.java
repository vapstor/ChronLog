package app.br.chronlog.activitys;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import app.br.chronlog.R;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


public class MainFragment extends Fragment implements View.OnClickListener {
    ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ConstraintLayout appView = getActivity().findViewById(R.id.appBar);

        ((MaterialTextView) appView.findViewById(R.id.titleBar)).setText(R.string.chronlog);

        MaterialButton icon = appView.findViewById(R.id.iconBar);
        icon.setIcon(getActivity().getDrawable(R.drawable.baseline_sync_24));

        progressBar = appView.findViewById(R.id.progressBarAppBar);

        MaterialButton configuraDeviceBtn = getActivity().findViewById(R.id.configDeviceBtn);
        progressBar.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == INVISIBLE) {
                icon.setEnabled(true);
            } else {
                icon.setEnabled(false);
            }
        });
        icon.setOnClickListener(this);
        configuraDeviceBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        FragmentTransaction transaction;
        switch (v.getId()) {
            case R.id.configDeviceBtn:
                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new ConfigDeviceFragment());
                transaction.addToBackStack(null);
                // Commit the transaction
                transaction.commit();
                break;
            case R.id.iconBar:
                progressBar.setVisibility(VISIBLE);
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new DevicesFragment());
                transaction.addToBackStack(null);
                // Commit the transaction
                transaction.commit();
                break;
            default:
                Toast.makeText(getContext(), "erro", Toast.LENGTH_SHORT).show();
        }
    }
}
