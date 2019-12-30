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
    private final ProgressBar progressBarContainer;
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
        public View viewForeground, viewBackground;

        public MyViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.logTitle);
            peso = v.findViewById(R.id.peso);
            progressBarItem = v.findViewById(R.id.progressBarItem);
            viewBackground = v.findViewById(R.id.viewBackground);
            viewForeground = v.findViewById(R.id.viewForeground);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerAdapter(ArrayList<TermoparLog> termoparLogList, ArrayList<String[]> termoparLogListString, ProgressBar progressBarContainer) {
        if (termoparLogList == null) {
            mDatasetAsString = termoparLogListString;
            adapterApenasString = true;
        } else {
            mDataset = termoparLogList;
        }
        this.progressBarContainer = progressBarContainer;
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
        //After that display progressbar at the below
        if (!adapterApenasString) {
            holder.name.setText(mDataset.get(position).getName());
            holder.peso.setText("(" + mDataset.get(position).getPeso().trim() + " bytes)");
        } else {
            holder.name.setText(mDatasetAsString.get(position)[0]);
            holder.peso.setText("(" + mDatasetAsString.get(position)[1].trim() + " bytes)");
        }
        if(progressBarContainer != null && progressBarContainer.getVisibility() == View.VISIBLE) {
            progressBarContainer.setVisibility(View.GONE);
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

    public void removeItem(int position) {
        if (!adapterApenasString)
            mDataset.remove(position);
        else
            mDatasetAsString.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    public void restoreItem(TermoparLog item, int position) {
        if (!adapterApenasString)
            mDataset.add(position, item);
        else
            mDatasetAsString.add(position, new String[]{item.getName(), item.getPeso()});
        // notify item added by position
        notifyItemInserted(position);
    }

}
