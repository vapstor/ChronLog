package app.br.chronlog.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.br.chronlog.R;
import app.br.chronlog.activitys.models.CTL0104A.CTL0104A_TermoparLog;
import app.br.chronlog.activitys.models.CTL0104B.CTL0104B_TermoparLog;
import app.br.chronlog.activitys.models.CVL0101A.CVL0101A_TermoparLog;

import static app.br.chronlog.activitys.DevicesActivity.modelo;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {
    private final ProgressBar progressBarContainer;
    private boolean adapterApenasString = false;
    private ArrayList<CVL0101A_TermoparLog> mDatasetCVL0101A;
    private ArrayList<CTL0104A_TermoparLog> mDatasetCTL0104A;
    private ArrayList<CTL0104B_TermoparLog> mDatasetCTL0104B;
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
    public RecyclerAdapter(ArrayList<?> termoparLogList, ArrayList<String[]> termoparLogListString, ProgressBar progressBarContainer) {
        if (termoparLogList == null) {
            mDatasetAsString = termoparLogListString;
            adapterApenasString = true;
        } else {
            switch (modelo) {
                case "CVL0101A":
                    mDatasetCVL0101A = (ArrayList<CVL0101A_TermoparLog>) termoparLogList;
                    break;
                case "CTL0104B":
                    mDatasetCTL0104B = (ArrayList<CTL0104B_TermoparLog>) termoparLogList;
                    break;
                case "CTL0104A":
                default:
                    mDatasetCTL0104A = (ArrayList<CTL0104A_TermoparLog>) termoparLogList;
                    break;
            }
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
            switch (modelo) {
                case "CVL0101A":
                    holder.name.setText(mDatasetCVL0101A.get(position).getName());
                    holder.peso.setText("(" + mDatasetCVL0101A.get(position).getPeso().trim() + " bytes)");
                    break;
                case "CTL0104B":
                    holder.name.setText(mDatasetCTL0104B.get(position).getName());
                    holder.peso.setText("(" + mDatasetCTL0104B.get(position).getPeso().trim() + " bytes)");
                    break;
                case "CTL0104A":
                default:
                    holder.name.setText(mDatasetCTL0104A.get(position).getName());
                    holder.peso.setText("(" + mDatasetCTL0104A.get(position).getPeso().trim() + " bytes)");
                    break;
            }

        } else {
            holder.name.setText(mDatasetAsString.get(position)[0]);
            holder.peso.setText("(" + mDatasetAsString.get(position)[1].trim() + " bytes)");
        }
        if (progressBarContainer != null && progressBarContainer.getVisibility() == View.VISIBLE) {
            progressBarContainer.setVisibility(View.GONE);
        }
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (!adapterApenasString) {
            switch (modelo) {
                case "CVL0101A":
                    return mDatasetCVL0101A.size();
                case "CTL0104B":
                    return mDatasetCTL0104B.size();
                case "CTL0104A":
                default:
                    return mDatasetCTL0104A.size();
            }
        } else {
            return mDatasetAsString.size();
        }
    }

    public void removeItem(int position) {
//        if (!adapterApenasString) {
//            switch (modelo) {
//                case "CTL0104B":
//                    mDatasetCTL0104B.remove(position);
//                    break;
//                case "CTL0104A":
//                default:
//                    mDatasetCTL0104A.remove(position);
//                    break;
//            }
//        } else {
//            mDatasetAsString.remove(position);
//        }
//        // notify the item removed by position
//        // to perform recycler view delete animations
//        // NOTE: don't call notifyDataSetChanged()
//        notifyItemRemoved(position);
    }

    public void restoreItem(CTL0104A_TermoparLog item, int position) {
//        if (!adapterApenasString)
//            mDataset.add(position, item);
//        else
//            mDatasetAsString.add(position, new String[]{item.getName(), item.getPeso()});
//        // notify item added by position
//        notifyItemInserted(position);
    }

}
