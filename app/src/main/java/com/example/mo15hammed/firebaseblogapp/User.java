package com.example.mo15hammed.firebaseblogapp;

public class User {

    String name, image, thumb;

    public User() {
    }

    public User(String name, String image, String thumb) {
        this.name = name;
        this.image = image;
        this.thumb = thumb;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", thumb='" + thumb + '\'' +
                '}';
    }


}
