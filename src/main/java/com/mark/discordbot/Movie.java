package com.mark.discordbot;

/**
 * Stores data related to a movie into a {@code Movie}.
 * Stores the movie's title, release year, poster URL, and runtime in minutes.
 */
public class Movie {

    /**
     * The title of the movie.
     */
    private final String title;

    /**
     * The release year of the movie.
     */
    private final int year;

    /**
     * The URL to the movie's poster.
     */
    private final String posterURL;

    /**
     * The movie's runtime in minutes.
     */
    private final int runtimeMinutes;

    /**
     * Constructor to make a {@code Movie}.
     * @param title the movies title
     * @param year the movies release year
     * @param posterURL the URl to the movies poster
     * @param runtimeMinutes the movies runtime in minutes
     */
    public Movie(String title, int year, String posterURL, int runtimeMinutes){
        this.title = title;
        this.year = year;
        this.posterURL = posterURL;
        this.runtimeMinutes = runtimeMinutes;

    }

    /**
     * Returns the title of the movie.
     * @return the title to return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the release year of the movie.
     * @return the year to return
     */
    public int getYear() {
        return year;
    }

    /**
     * Returns the URL of the movie's poster.
     * @return the URL to return
     */
    public String getPosterURL() {
        return posterURL;
    }

    /**
     * Returns the runtime of the movies in minutes.
     * @return the runtime to return
     */
    public int getRuntimeMinutes(){
        return runtimeMinutes;
    }
}
