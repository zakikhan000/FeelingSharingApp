package AppModules.Models;


public class RSSItem {
    private String title, description, link, imageUrl;

    public RSSItem(String title, String description, String link, String imageUrl) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLink() { return link; }
    public String getImageUrl() { return imageUrl; }
}
