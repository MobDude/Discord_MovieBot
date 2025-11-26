package com.mark.discordbot;

public class Movie {
    private String title;
    private int year;
    private String posterURL;

    public Movie(String title, int year, String posterURL){
        this.title = title;
        this.year = year;
        this.posterURL = posterURL;
    }

    //Getters
    public String getTitle() {
        return title;
    }
    public int getYear() {
        return year;
    }
    public String getPosterURL() {
        return posterURL;
    }
}
