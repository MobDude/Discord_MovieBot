package com.mark.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import java.time.*;
import java.util.*;

public class MovieScheduler {
    private static final int BUFFER_MINUTES = 15;
    private static final int DEFAULT_RUNTIME = 120;
    private static final ZoneId ZONE = ZoneId.of("America/Toronto");
    private static final String MOVIE_CHANNEL_NAME = "🍿movie-theatre";

    public record WeeklySlot(DayOfWeek day, LocalTime time, boolean longAllowed) {}

    private List<WeeklySlot> slots = defaultSlots();
    private final Set<LocalDate> skippedDates = new HashSet<>();

    public static List<WeeklySlot> defaultSlots() {
        return new ArrayList<>(List.of(
            new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(18, 30), true),
            new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(21, 0), false),
            new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(19, 45), false),
            new WeeklySlot(DayOfWeek.THURSDAY, LocalTime.of(19, 45), false)
        ));
    }

    public void setSlots(List<WeeklySlot> newSlots) {
        this.slots = new ArrayList<>(newSlots);
    }

    /**
     * Resynchronizes all Discord events to match the order of the movie list.
     */
    public void resyncAllEvents(Guild guild, List<Movie> movies) {
        // We start searching from 'now'
        ZonedDateTime currentSearchPointer = ZonedDateTime.now(ZONE);

        for (Movie movie : movies) {
            OffsetDateTime start = findNextSlotForMovie(movie, currentSearchPointer);

            if (start != null) {
                int duration = movie.getRuntimeMinutes() > 0 ? movie.getRuntimeMinutes() : DEFAULT_RUNTIME;
                OffsetDateTime end = start.plusMinutes(duration + BUFFER_MINUTES);

                manageDiscordEvent(guild, movie, start, end);

                // CRITICAL: Move the pointer to the START of this movie so the
                // NEXT movie in the list finds the NEXT available slot after this one.
                currentSearchPointer = start.atZoneSameInstant(ZONE).plusMinutes(1);
            }
        }
    }

    private OffsetDateTime findNextSlotForMovie(Movie movie, ZonedDateTime startFrom) {
        int runtime = movie.getRuntimeMinutes() > 0 ? movie.getRuntimeMinutes() : DEFAULT_RUNTIME;
        OffsetDateTime earliest = null;

        // Search through the next 52 weeks
        for (int i = 0; i < 52; i++) {
            ZonedDateTime searchBase = startFrom.plusWeeks(i);
            for (WeeklySlot slot : slots) {
                // Skip if movie is too long for a "short" slot (e.g., > 150 mins)
                if (!slot.longAllowed() && runtime > 150) continue;

                OffsetDateTime occurrence = getNextOccurrence(slot, searchBase);
                LocalDate occurrenceDate = occurrence.atZoneSameInstant(ZONE).toLocalDate();

                //This movie night has been skipped
                if (skippedDates.contains(occurrenceDate)){
                    continue;
                }

                if (occurrence.isAfter(startFrom.toOffsetDateTime()) &&
                        (earliest == null || occurrence.isBefore(earliest))) {
                    earliest = occurrence;
                }
            }
        }
        return earliest;
    }

    private void manageDiscordEvent(Guild guild, Movie movie, OffsetDateTime start, OffsetDateTime end) {
        if (movie.getScheduledEventId() != null) {
            // Update existing
            guild.retrieveScheduledEventById(movie.getScheduledEventId()).queue(
                    event -> {
                        // Only update if time actually changed to save API hits
                        if (!event.getStartTime().equals(start)) {
                            event.getManager().setStartTime(start).setEndTime(end).queue();
                        }
                    },
                    error -> createDiscordEvent(guild, movie, start, end) // Re-create if deleted by user
            );
        } else {
            createDiscordEvent(guild, movie, start, end);
        }
    }

    private OffsetDateTime getNextOccurrence(WeeklySlot slot, ZonedDateTime base) {
        LocalDate date = base.toLocalDate();
        while (date.getDayOfWeek() != slot.day()) {
            date = date.plusDays(1);
        }

        ZonedDateTime candidate = ZonedDateTime.of(date, slot.time(), ZONE);
        if (candidate.isBefore(base)) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate.toOffsetDateTime();
    }

    public void createDiscordEvent(Guild guild, Movie movie, OffsetDateTime start, OffsetDateTime end) {
        VoiceChannel channel = getMovieChannel(guild);
        if (channel == null) return;

        guild.createScheduledEvent("Movie Night - " + movie.getTitle(), channel, start)
                .setEndTime(end)
                .setDescription(movie.getTitle() + " - Enjoy the show!")
                .queue(event -> movie.setScheduledEventId(event.getIdLong()));
    }

    private VoiceChannel getMovieChannel(Guild guild) {
        return guild.getVoiceChannels().stream()
                .filter(vc -> vc.getName().equals(MOVIE_CHANNEL_NAME))
                .findFirst()
                .orElse(null);
    }

    public void addBreak(LocalDate date){
        skippedDates.add(date);
    }

    public void removeBreak(LocalDate date) {
        skippedDates.remove(date);
    }

    public boolean isBreak(LocalDate date) {
        return skippedDates.contains(date);
    }

    public Set<LocalDate> getBreaks() {
        return Collections.unmodifiableSet(skippedDates);
    }
}
