package app.br.chronlog.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.br.chronlog.R;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {
    private boolean adapterApenasString = false;
    private ArrayList<TermoparLog> mDataset;
    private ArrayList<String[]> mDatasetAsString;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView name, peso;
        public ProgressBar progressBarItem;

        public MyViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.logTitle);
            peso = v.findViewById(R.id.peso);
            progressBarItem = v.findViewById(R.id.progressBarItem);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerAdapter(ArrayList<TermoparLog> termoparLogList, ArrayList<String[]> termoparLogListString) {
        if (termoparLogList == null) {
            mDatasetAsString = termoparLogListString;
            adapterApenasString = true;
        } else {
            mDataset = termoparLogList;
        }
        setHasStableIds(true);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_list_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (!adapterApenasString) {
            holder.name.setText(mDataset.get(position).getName());
            holder.peso.setText("(" + mDataset.get(position).getPeso().trim() + " kb)");
        } else {
            holder.name.setText(mDatasetAsString.get(position)[0]);
            holder.peso.setText("(" + mDatasetAsString.get(position)[1].trim() + " kb)");
        }
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (!adapterApenasString) {
            return mDataset.size();
        } else {
            return mDatasetAsString.size();
        }
    }

}
