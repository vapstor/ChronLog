package app.br.chronlog.activitys;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import app.br.chronlog.R;

import static android.view.View.INVISIBLE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {//@link ConfigDeviceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConfigDeviceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfigDeviceFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String titulo;
    private ProgressBar progressBar;


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
        if (getArguments() != null) {
            ((MaterialTextView) getActivity().findViewById(R.id.appBar).findViewById(R.id.titleBar)).setText(getArguments().getString("device"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_config_device, container, false);

        ConstraintLayout appBarView = getActivity().findViewById(R.id.appBar);

        ((MaterialTextView) appBarView.findViewById(R.id.titleBar)).setText(R.string.configurando);

        MaterialButton icon = appBarView.findViewById(R.id.iconBar);
        icon.setIcon(getActivity().getDrawable(R.drawable.baseline_home_24));

        progressBar = appBarView.findViewById(R.id.progressBarAppBar);
        progressBar.setVisibility(INVISIBLE);

        icon.setOnClickListener(this);
        return view;
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
                ((AppCompatActivity)getContext()).getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
        }
    }
}
