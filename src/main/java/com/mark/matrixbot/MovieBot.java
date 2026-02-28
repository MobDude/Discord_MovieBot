package com.mark.matrixbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry point and event handler for MovieBot.
 * <p>
 * This class initializes the Discord bot, registers slash commands, and handles all user interactions related to movie
 * management, including adding, removing, listing, and scheduling movies.
 * </p>
 */
public class MovieBot
{
    /**
     * Number of movies displayed per page in the movie list.
     */
    private static final int PAGE_SIZE = 5;

    /**
     * Persistent storage for the movie list.
     */
    private final MovieStorage storage;

    /**
     * Client for querying the TMDb API.
     */
    private final TMDb tmdb;

    public MovieBot(String tmdbKey) {
        this.tmdb = new TMDb(tmdbKey);
        this.storage = new MovieStorage();
    }

    private final Map<String, Integer> roomPages = new HashMap<>();

    /**
     * Commands for matrix bot. Parses the message and handles appropriately.
     * @param roomId the room id where the command was sent from
     * @param sender the sender of the command
     * @param message the command message
     */
    public String onMessage(String roomId, String sender, String message){
        if(!message.startsWith("!")){
            return null; //ignore non commands
        }

        String[] parts = message.trim().split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {

            case "!addmovie":
                return handleAddMovieCommand(roomId, parts);

            case "!removemovie":
                return handleRemoveMovieCommand(roomId, parts);

            case "!remove":
                return handleRemoveSelection(roomId, parts);

            case "!movielist":
                roomPages.put(roomId, 0);
                return buildMovieListText(0);

            case "!next":
                return handleNextPage(roomId);

            case "!prev":
                return handlePrevPage(roomId);

            case "!moviehelp":
                return buildHelpText();

            case "!select":
                return handleSelection(roomId, parts);

            default:
                return "Unknown command. Try `!moviehelp`.";
        }
    }

    /**
     * Builder for creating the movie list for matrix using strings.
     * @param page the page of the list
     * @return the message representing a page of the movie list
     */
    private String buildMovieListText(int page){
        List<Movie> movies = storage.getMovies();
        int totalPages = computeTotalPages(movies);

        StringBuilder sb =new StringBuilder();
        sb.append("**Movie List**\n");
        sb.append("Page ").append(page + 1).append(" of ").append(totalPages).append("\n\n");

        if (page == 0 && !movies.isEmpty()){
            Movie next = movies.getFirst();
            sb.append("**Next Up:** ").append(next.getTitle()).append(" (").append(next.getYear()).append(")\n");

            if (next.getPosterURL() != null){
                sb.append(next.getPosterURL()).append("\n");
            }

            sb.append("\nUse `!next` or `!prev`");
            return sb.toString();
        }

        int start = (page == 0) ? 0 : 1 + (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, movies.size());

        for (int i = start; i < end; i++) {
            if (page == 0 && i == 0) continue; // skip "Next Up"
            Movie m = movies.get(i);
            sb.append(i + 1).append(". ").append(m.getTitle()).append(" (").append(m.getYear()).append(")\n");
        }
        return sb.toString();
    }

    private String handleSelection(String roomId, String[] parts) {

        if (!pendingSelections.containsKey(roomId)) {
            return "No pending selection.";
        }

        if (parts.length < 2) {
            return "Usage: `!select <number>`";
        }

        int choice;
        try {
            choice = Integer.parseInt(parts[1]) - 1;
        } catch (NumberFormatException e) {
            return "Invalid number.";
        }

        JsonArray results = pendingSelections.get(roomId);

        if (choice < 0 || choice >= results.size()) {
            return "Invalid selection.";
        }

        Movie movie = buildMovieFromTmdb(results.get(choice).getAsJsonObject());
        storage.addMovie(movie);
        roomPages.remove(roomId);

        pendingSelections.remove(roomId);

        return "Added **" + movie.getTitle() + "** (" + movie.getYear() + ")";
    }

    private String handleNextPage(String roomId) {

        int current = roomPages.getOrDefault(roomId, 0);
        int totalPages = computeTotalPages(storage.getMovies());

        if (current >= totalPages - 1) {
            return "Already at last page.";
        }

        int newPage = current + 1;
        roomPages.put(roomId, newPage);

        return buildMovieListText(newPage);
    }

    private String handlePrevPage(String roomId) {

        int current = roomPages.getOrDefault(roomId, 0);

        if (current == 0) {
            return "Already at first page.";
        }

        int newPage = current - 1;
        roomPages.put(roomId, newPage);

        return buildMovieListText(newPage);
    }

    private String buildHelpText() {
        return """
    **MovieBot Commands**
    
    !addmovie <title> [year]
    !removemovie <name>
    !movielist
    !next
    !prev
    !select <number>
    """;
    }

    private final Map<String, JsonArray> pendingSelections = new HashMap<>();

    private String handleAddMovieCommand(String roomId, String[] parts) {

        if (parts.length < 2) {
            return "Usage: `!addmovie <title> [year]`";
        }

        // Extract title + optional year
        Integer year = null;
        StringBuilder titleBuilder = new StringBuilder();

        for (int i = 1; i < parts.length; i++) {
            if (parts[i].matches("\\d{4}")) {
                year = Integer.parseInt(parts[i]);
            } else {
                titleBuilder.append(parts[i]).append(" ");
            }
        }

        String title = titleBuilder.toString().trim();

        JsonArray results = tmdb.searchMovies(title, year);

        if (results.isEmpty()) {
            return "No movies found for **" + title + "**.";
        }

        if (results.size() == 1) {
            Movie movie = buildMovieFromTmdb(results.get(0).getAsJsonObject());
            storage.addMovie(movie);
            roomPages.remove(roomId);
            return "Added **" + movie.getTitle() + "** (" + movie.getYear() + ")";
        }

        // Multiple results
        StringBuilder sb = new StringBuilder();
        sb.append("Multiple matches found:\n\n");

        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            JsonObject obj = results.get(i).getAsJsonObject();
            String resultTitle = obj.get("title").getAsString();
            String release = obj.has("release_date") ? obj.get("release_date").getAsString() : "Unknown";
            sb.append(i + 1).append(") ").append(resultTitle).append(" (").append(release).append(")\n");
        }

        sb.append("\nReply with `!select <number>`");

        // You will need to store these results per room for later selection
        pendingSelections.put(roomId, results);

        return sb.toString();
    }

    private final Map<String, List<Movie>> pendingRemovals = new HashMap<>();
    private String handleRemoveMovieCommand(String roomId, String[] parts) {

        if (parts.length < 2) {
            return "Usage: `!removemovie <name>`";
        }

        String query = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        List<Movie> allMovies = storage.getMovies();

        List<Movie> matches = new ArrayList<>();
        for (Movie m : allMovies) {
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) {
                matches.add(m);
            }
        }

        if (matches.isEmpty()) {
            return "No movies found matching **" + query + "**.";
        }

        if (matches.size() == 1) {
            Movie movie = matches.getFirst();
            storage.removeMovie(movie);
            roomPages.remove(roomId);
            return "Removed **" + movie.getTitle() + "**.";
        }

        // Multiple matches
        StringBuilder sb = new StringBuilder();
        sb.append("Multiple matches found:\n\n");

        for (int i = 0; i < matches.size(); i++) {
            Movie m = matches.get(i);
            sb.append(i + 1)
                    .append(") ")
                    .append(m.getTitle())
                    .append(" (")
                    .append(m.getYear())
                    .append(")\n");
        }

        sb.append("\nReply with `!remove <number>`");

        pendingRemovals.put(roomId, matches);

        return sb.toString();
    }

    private String handleRemoveSelection(String roomId, String[] parts) {

        if (!pendingRemovals.containsKey(roomId)) {
            return "No pending removal.";
        }

        if (parts.length < 2) {
            return "Usage: `!remove <number>`";
        }

        int choice;
        try {
            choice = Integer.parseInt(parts[1]) - 1;
        } catch (NumberFormatException e) {
            return "Invalid number.";
        }

        List<Movie> matches = pendingRemovals.get(roomId);

        if (choice < 0 || choice >= matches.size()) {
            return "Invalid selection.";
        }

        Movie movie = matches.get(choice);
        storage.removeMovie(movie);
        roomPages.remove(roomId);
        pendingRemovals.remove(roomId);

        return "🗑 Removed **" + movie.getTitle() + "**.";
    }

    private int computeTotalPages(List<Movie> movies){
        if (movies.isEmpty()){
            return  1;
        }
        return Math.max(1, (int) Math.ceil((movies.size() -1) / (double) PAGE_SIZE) + 1);
    }

    private Movie buildMovieFromTmdb(JsonObject movieJson) {
        String title = movieJson.get("title").getAsString();

        int year = 0;
        if (movieJson.has("release_date") && !movieJson.get("release_date").isJsonNull()){
            String release = movieJson.get("release_date").getAsString();
            if (release.length() >= 4){
                year = Integer.parseInt(release.substring(0,4));
            }
        }

        String poster = movieJson.has("poster_path") && !movieJson.get("poster_path").isJsonNull()
                ? "https://image.tmdb.org/t/p/w500" + movieJson.get("poster_path").getAsString()
                : null;

        int runtime = tmdb.getRuntime(movieJson.get("id").getAsInt());

        return new Movie(title, year, poster, runtime);
    }

}
