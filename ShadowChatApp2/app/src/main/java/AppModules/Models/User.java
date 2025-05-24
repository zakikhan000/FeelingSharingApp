package AppModules.Models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("name")
    private String name;

    @SerializedName("First_Name")
    private String firstName;

    @SerializedName("Last_Name")
    private String lastName;

    @SerializedName("Anonymous_name")
    private String anonymousName;

    @SerializedName("profile_image")
    private String profileImage;

    @SerializedName("Name_visibility")
    private String nameVisibility;

    @SerializedName("Profile_visibility")
    private String profileVisibility;

    @SerializedName("PersonalInfo_visibility")
    private String personalInfoVisibility;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("bio")
    private String bio;

    @SerializedName("location")
    private String location;

    @SerializedName("join_date")
    private String joinDate;

    @SerializedName("is_online")
    private boolean isOnline;

    @SerializedName("followers_count")
    private int followersCount;

    @SerializedName("following_count")
    private int followingCount;

    @SerializedName("posts_count")
    private int postsCount;

    // Default constructor
    public User() {
    }

    // Constructor with basic info
    public User(String id, String username, String name) {
        this.id = id;
        this.username = username;
        this.name = name;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAnonymousName() {
        return anonymousName;
    }

    public void setAnonymousName(String anonymousName) {
        this.anonymousName = anonymousName;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getNameVisibility() {
        return nameVisibility;
    }

    public void setNameVisibility(String nameVisibility) {
        this.nameVisibility = nameVisibility;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(String profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    public String getPersonalInfoVisibility() {
        return personalInfoVisibility;
    }

    public void setPersonalInfoVisibility(String personalInfoVisibility) {
        this.personalInfoVisibility = personalInfoVisibility;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public int getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(int postsCount) {
        this.postsCount = postsCount;
    }

    // Helper method to get display name based on visibility
    public String getDisplayName() {
        if ("visible".equals(nameVisibility)) {
            return firstName + " " + lastName;
        } else {
            return anonymousName;
        }
    }

    // Helper method to check if profile image should be shown
    public boolean shouldShowProfileImage() {
        return "visible".equals(profileVisibility) && profileImage != null && !profileImage.isEmpty();
    }

    // Helper method to check if personal info should be shown
    public boolean shouldShowPersonalInfo() {
        return "visible".equals(personalInfoVisibility);
    }

    // Method to convert User object to JSON string
    public String toJson() {
        return new com.google.gson.Gson().toJson(this);
    }

    // Method to create User object from JSON string
    public static User fromJson(String json) {
        return new com.google.gson.Gson().fromJson(json, User.class);
    }
}