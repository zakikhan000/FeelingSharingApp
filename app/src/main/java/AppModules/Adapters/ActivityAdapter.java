package AppModules.Adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import AppModules.Models.ActivityItem;
import com.example.shadowchatapp2.R;

import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {
    private List<ActivityItem> activities;

    public ActivityAdapter(List<ActivityItem> activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityItem item = activities.get(position);
        holder.textView.setText(item.getMessage());
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public void setActivities(List<ActivityItem> newActivities) {
        this.activities = newActivities;
        notifyDataSetChanged();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ActivityViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_activity_message);
        }
    }
}
