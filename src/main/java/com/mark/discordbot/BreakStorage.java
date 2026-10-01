package com.mark.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public class BreakStorage {

    private static final String FILE_PATH = "breaks.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Set<LocalDate> breaks;

    public BreakStorage() {
        this.breaks = load();
    }

    public synchronized Set<LocalDate> getBreaks() {
        return new HashSet<>(breaks);
    }

    public synchronized boolean addBreak(LocalDate date) {
        if (breaks.add(date)) {
            save();
            return true;
        }
        return false;
    }

    public synchronized boolean removeBreak(LocalDate date) {
        if (breaks.remove(date)) {
            save();
            return true;
        }
        return false;
    }

    public synchronized boolean hasBreak(LocalDate date) {
        return breaks.contains(date);
    }

    private void save() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            List<String> dates = breaks.stream()
                    .sorted()
                    .map(LocalDate::toString)
                    .toList();

            GSON.toJson(dates, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<LocalDate> load() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return new HashSet<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> loaded = GSON.fromJson(reader, listType);

            Set<LocalDate> result = new HashSet<>();

            if (loaded != null) {
                for (String date : loaded) {
                    try {
                        result.add(LocalDate.parse(date));
                    } catch (Exception ignored) {
                        // Ignore invalid persisted break dates.
                    }
                }
            }

            return result;

        } catch (Exception e) {
            return new HashSet<>();
        }
    }
}