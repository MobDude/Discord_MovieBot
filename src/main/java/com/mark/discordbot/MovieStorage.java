package com.mark.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 *Handles the storage of {@link Movie} objects using a JSON file.
 * <p>
 * Movies are loaded from disk on construction adn written back whenever the collection is modified.
 * </p>
 */
public class MovieStorage {

    /**
     * Path to the JSON file used for movie storage.
     */
    private static final String FILE_PATH = "movies.json";

    /**
     * Gson instance is configured to be human-readable JSon output.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * In-memory list of stored movies.
     */
    private final List<Movie> movies;

    /**
     * Constructs a {@code MovieStorage} instance and loads any existing movies from persistent storage.
     */
    public MovieStorage() {
        movies = load();
    }

    /**
     * Returns the list of currently stored movies.
     * @return the list of movies
     */
    public List<Movie> getMovies() {
        return movies;
    }

    /**
     * Adds a movie to storage and saves the change to disk.
     * @param movie the movie to add
     */
    public void addMovie(Movie movie) {
        movies.add(movie);
        save();
    }

    /**
     * Removes a movie from storage and saves the change to disk.
     * @param movie the movie to remove
     */
    public void removeMovie(Movie movie) {
        movies.remove(movie);
        save();
    }

    /**
     * Loads the movie list from the JSON file.
     * <p>
     *     If the file does not exist or cannot be read, an empty list is returned.
     * </p>
     * @return a list of loaded movies, or an empty list on failure
     */
    private List<Movie> load() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<Movie>>(){}.getType();
            List<Movie> loaded = GSON.fromJson(reader, listType);

            //gson may return null of the file is empty or broken
            return loaded != null ? loaded : new ArrayList<>();

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Saves the current movie list to the JSON file.
     */
    private void save() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            GSON.toJson(movies, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
