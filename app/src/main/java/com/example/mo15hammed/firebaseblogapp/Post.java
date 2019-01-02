package com.example.mo15hammed.firebaseblogapp;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Post extends PostID {

    private String image, thumb, description, authorID;
    private Date timestamp;
    private User author;
    private boolean isLiked;

    public Post() {
    }

    public Post(String image, String thumb, String description, String authorID, Date timestamp, User author) {
        this.image = image;
        this.thumb = thumb;
        this.description = description;
        this.authorID = authorID;
        this.timestamp = timestamp;
        this.author = author;
    }

    @Exclude
    public boolean isLiked() {
        return isLiked;
    }
    @Exclude
    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    @Exclude
    public User getAuthor() {
        return author;
    }
    @Exclude
    public void setAuthor(User author) {
        this.author = author;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthorID() {
        return authorID;
    }

    public void setAuthorID(String authorID) {
        this.authorID = authorID;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Post{" +
                "image='" + image + '\'' +
                ", thumb='" + thumb + '\'' +
                ", description='" + description + '\'' +
                ", authorID='" + authorID + '\'' +
                ", timestamp=" + timestamp +
                ", author=" + author +
                '}';
    }
}


