package AppModules.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shadowchatapp2.UserMessages.Rss_ExploererHelper.FeedDetailActivity;
import com.example.shadowchatapp2.R;
import AppModules.Models.RSSItem;

import java.util.ArrayList;


public class RSSAdapters extends RecyclerView.Adapter<RSSAdapters.ViewHolder> {
    private ArrayList<RSSItem> items;
    private Context context;

    public RSSAdapters(ArrayList<RSSItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;
        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.rss_title);
            image = itemView.findViewById(R.id.rss_image);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rss_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RSSItem item = items.get(position);
        holder.title.setText(item.getTitle());

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, FeedDetailActivity.class);
            i.putExtra("title", item.getTitle());
            i.putExtra("description", item.getDescription());
            i.putExtra("link", item.getLink());
            i.putExtra("imageUrl", item.getImageUrl());
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
