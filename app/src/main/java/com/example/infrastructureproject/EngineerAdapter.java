package com.example.infrastructureproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;



import java.util.List;

public class EngineerAdapter extends ArrayAdapter<CouncilTicketDetailActivity.Engineer> {

    private Context context;
    private List<CouncilTicketDetailActivity.Engineer> engineers;
    private int selectedPosition = -1;

    public EngineerAdapter(Context context, List<CouncilTicketDetailActivity.Engineer> engineers) {
        super(context, 0, engineers);
        this.context = context;
        this.engineers = engineers;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_engineer, parent, false);
        }

        CouncilTicketDetailActivity.Engineer engineer = engineers.get(position);

        RadioButton rbSelect = listItem.findViewById(R.id.rbSelectEngineer);
        TextView tvEngineerName = listItem.findViewById(R.id.tvEngineerName);
        TextView tvEngineerEmail = listItem.findViewById(R.id.tvEngineerEmail);
        TextView tvTotalTickets = listItem.findViewById(R.id.tvTotalTickets);
        TextView tvHighPriority = listItem.findViewById(R.id.tvHighPriority);

        tvEngineerName.setText(engineer.getName());
        tvEngineerEmail.setText(engineer.getEmail());
        tvTotalTickets.setText("Total Reports: " + engineer.getTotalTickets());
        tvHighPriority.setText("High Priority: " + engineer.getHighPriorityTickets());

        rbSelect.setChecked(position == selectedPosition);

        return listItem;
    }
}
