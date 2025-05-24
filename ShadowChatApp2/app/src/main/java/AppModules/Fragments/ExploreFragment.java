package AppModules.Fragments;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.shadowchatapp2.R;
import AppModules.Models.RSSItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import AppModules.Adapters.RSSAdapters;

public class ExploreFragment extends Fragment {

    private static final String TAG = "ExploreFragment";
    private static final int TIMEOUT_MS = 5000; // 5 seconds timeout
    private static final int THREAD_POOL_SIZE = 5; // Number of parallel feeds to load

    RecyclerView recyclerView;
    ArrayList<RSSItem> rssItems;
    RSSAdapters adapter;
    ExecutorService executorService;

    // RSS Feeds
    String[] rssUrls = {
            "https://www.theverge.com/rss/index.xml",
            "https://mashable.com/feed",
            "https://www.forbes.com/business/feed/",
            "https://feeds.bbci.co.uk/news/rss.xml",
            "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"
    };

    public ExploreFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewExplore);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        rssItems = new ArrayList<>();
        adapter = new RSSAdapters(rssItems, getContext());
        recyclerView.setAdapter(adapter);

        // Initialize thread pool
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Load all feeds in parallel
        for (String url : rssUrls) {
            new FastLoadRSSFeed().executeOnExecutor(executorService, url);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private class FastLoadRSSFeed extends AsyncTask<String, Void, ArrayList<RSSItem>> {
        @Override
        protected ArrayList<RSSItem> doInBackground(String... urls) {
            ArrayList<RSSItem> items = new ArrayList<>(10); // Pre-allocate capacity

            try {
                Document doc = Jsoup.connect(urls[0])
                        .timeout(TIMEOUT_MS)
                        .maxBodySize(0) // No limit on response size
                        .ignoreContentType(true)
                        .get();

                Elements entries = doc.select("item");

                // Process first 10 items only for performance
                int limit = Math.min(10, entries.size());
                for (int i = 0; i < limit; i++) {
                    Element entry = entries.get(i);
                    String title = quickSelect(entry, "title");
                    String link = quickSelect(entry, "link");
                    String imageUrl = quickImageExtract(entry);

                    // Skip description to save time (if not needed)
                    items.add(new RSSItem(title, "", link, imageUrl));
                }

            } catch (IOException e) {
                Log.e(TAG, "Error loading: " + urls[0], e);
            }
            return items;
        }

        private String quickSelect(Element entry, String tag) {
            Element el = entry.selectFirst(tag);
            return el != null ? el.text() : "";
        }

        private String quickImageExtract(Element entry) {
            // Check most common patterns first
            Element img;

            // 1. Check media thumbnail (BBC)
            if ((img = entry.selectFirst("media|thumbnail")) != null) {
                return img.attr("url");
            }

            // 2. Check content encoded (Mashable)
            Element content = entry.selectFirst("content|encoded");
            if (content != null) {
                String html = content.text();
                int imgStart = html.indexOf("<img");
                if (imgStart > 0) {
                    int srcStart = html.indexOf("src=\"", imgStart);
                    if (srcStart > 0) {
                        srcStart += 5;
                        int srcEnd = html.indexOf("\"", srcStart);
                        if (srcEnd > srcStart) {
                            return html.substring(srcStart, srcEnd);
                        }
                    }
                }
            }

            // 3. Check enclosure
            if ((img = entry.selectFirst("enclosure[url]")) != null) {
                return img.attr("url");
            }

            // 4. Quick check in description
            Element desc = entry.selectFirst("description");
            if (desc != null) {
                String descText = desc.text();
                int imgPos = descText.indexOf("<img");
                if (imgPos > 0) {
                    int srcPos = descText.indexOf("src=\"", imgPos);
                    if (srcPos > 0) {
                        srcPos += 5;
                        int srcEnd = descText.indexOf("\"", srcPos);
                        if (srcEnd > srcPos) {
                            return descText.substring(srcPos, srcEnd);
                        }
                    }
                }
            }

            return "";
        }

        @Override
        protected void onPostExecute(ArrayList<RSSItem> result) {
            if (result != null && !result.isEmpty()) {
                synchronized (rssItems) {
                    rssItems.addAll(result);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }
}