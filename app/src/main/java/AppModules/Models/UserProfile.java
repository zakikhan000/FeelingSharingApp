package AppModules.Models;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName("username")
    private String username;

    @SerializedName("name")
    private String name;

    @SerializedName("profile_image")
    private String profileImageBase64;

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }
}

