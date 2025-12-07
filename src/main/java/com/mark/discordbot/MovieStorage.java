package com.mark.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MovieStorage {

    private static final String FILE_PATH = "movies.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<Movie> movies;

    public MovieStorage() {
        movies = load();
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void addMovie(Movie movie) {
        movies.add(movie);
        save();
    }

    public void removeMovie(Movie movie) {
        movies.remove(movie);
        save();
    }

    private List<Movie> load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<Movie>>(){}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void save() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(movies, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
