package com.example.mo15hammed.firebaseblogapp;

import com.google.firebase.firestore.Exclude;

public class Comment {

    private String comment, userID;
    private long timestamp;
    private User author;

    public Comment() {

    }

    public Comment(String comment, String userID, long timestamp, User author) {
        this.comment = comment;
        this.userID = userID;
        this.timestamp = timestamp;
        this.author = author;
    }

    @Exclude
    public User getAuthor() {
        return author;
    }
    @Exclude
    public void setAuthor(User author) {
        this.author = author;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "comment='" + comment + '\'' +
                ", userID='" + userID + '\'' +
                ", timestamp=" + timestamp +
                ", author=" + author +
                '}';
    }
}
