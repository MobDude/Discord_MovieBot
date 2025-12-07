package com.mark.discordbot;

public class Movie {
    private final String title;
    private final int year;
    private final String posterURL;
    private final int runtimeMinutes;

    public Movie(String title, int year, String posterURL, int runtimeMinutes){
        this.title = title;
        this.year = year;
        this.posterURL = posterURL;
        this.runtimeMinutes = runtimeMinutes;

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
    public int getRuntimeMinutes(){
        return runtimeMinutes;
    }
}
