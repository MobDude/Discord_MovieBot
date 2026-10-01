package com.mark.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public class SlotStorage {
    private static final String FILE_PATH = "slots.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<MovieScheduler.WeeklySlot> slots;

    public SlotStorage() {
        this.slots = load();
        if (this.slots.isEmpty()) { // Default slots if file is new
            this.slots.addAll(MovieScheduler.defaultSlots());
            save();
        }
    }

    public synchronized List<MovieScheduler.WeeklySlot> getSlots() { return new ArrayList<>(slots); }

    public synchronized void addSlot(MovieScheduler.WeeklySlot slot) {
        slots.add(slot);
        save();
    }

    public synchronized boolean removeSlot(int index) {
        if (index >= 0 && index < slots.size()) {
            slots.remove(index);
            save();
            return true;
        }
        return false;
    }

    public synchronized boolean updateSlot(int index, MovieScheduler.WeeklySlot slot) {
        if (index >= 0 && index < slots.size()) {
            slots.set(index, slot);
            save();
            return true;
        }
        return false;
    }

    private void save() {
        try (Writer writer = new FileWriter(FILE_PATH)) { GSON.toJson(toDtos(slots), writer); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private List<MovieScheduler.WeeklySlot> load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<SlotDto>>(){}.getType();
            List<SlotDto> loaded = GSON.fromJson(reader, listType);
            return loaded != null ? fromDtos(loaded) : new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private List<SlotDto> toDtos(List<MovieScheduler.WeeklySlot> source) {
        List<SlotDto> result = new ArrayList<>();
        for (MovieScheduler.WeeklySlot slot : source) {
            result.add(new SlotDto(slot.day().name(), slot.time().toString(), slot.longAllowed()));
        }
        return result;
    }

    private List<MovieScheduler.WeeklySlot> fromDtos(List<SlotDto> source) {
        List<MovieScheduler.WeeklySlot> result = new ArrayList<>();
        for (SlotDto dto : source) {
            try {
                result.add(new MovieScheduler.WeeklySlot(
                        DayOfWeek.valueOf(dto.day),
                        LocalTime.parse(dto.time),
                        dto.longAllowed
                ));
            } catch (Exception ignored) {
                // Skip invalid persisted slots instead of preventing the bot from starting.
            }
        }
        return result;
    }

    private record SlotDto(String day, String time, boolean longAllowed) {
    }
}
