package com.example.mo15hammed.firebaseblogapp;

public class PostID {

    private String postId;

    public <T extends PostID> T withId (final String id) {
        this.postId = id;
        return (T) this;
    }

    public String getId() {
        return postId;
    }
}
