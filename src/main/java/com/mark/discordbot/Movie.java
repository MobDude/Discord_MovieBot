package com.mark.discordbot;

/**
 * Represents a movie and its associated Discord event data.
 */
public class Movie {

    private final String title;
    private final int year;
    private final String posterURL;
    private final int runtimeMinutes;
    private Long scheduledEventId; // Mutable: set after Discord event creation

    public Movie(String title, int year, String posterURL, int runtimeMinutes) {
        this.title = title;
        this.year = year;
        this.posterURL = posterURL;
        this.runtimeMinutes = runtimeMinutes;
    }

    // Getters
    public String getTitle() { return title; }
    public int getYear() { return year; }
    public String getPosterURL() { return posterURL; }
    public int getRuntimeMinutes() { return runtimeMinutes; }
    public Long getScheduledEventId() { return scheduledEventId; }

    // Setter for Discord Sync
    public void setScheduledEventId(Long id) { this.scheduledEventId = id; }

    /**
     * Helper for consistent naming across embeds and events.
     * Example: "The Matrix (1999)"
     */
    public String getDisplayTitle() {
        return String.format("%s (%s)", title, (year > 0 ? year : "Unknown"));
    }

    /**
     * Formats runtime into a human-readable string.
     * Example: "2h 15m"
     */
    public String getFormattedRuntime() {
        if (runtimeMinutes <= 0) return "Unknown";
        int hours = runtimeMinutes / 60;
        int minutes = runtimeMinutes % 60;
        return (hours > 0) ? String.format("%dh %dm", hours, minutes) : minutes + "m";
    }
}