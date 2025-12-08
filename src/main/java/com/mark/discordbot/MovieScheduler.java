package com.mark.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ScheduledEvent;

import java.time.*;
import java.util.*;

public class MovieScheduler {

    private static final int BUFFER_MINUTES = 15;
    private static final ZoneId ZONE = ZoneId.of("America/Toronto");

    private record WeeklySlot(DayOfWeek day, LocalTime time, boolean longAllowed) {}

    private final List<WeeklySlot> slots = List.of(
            new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(18, 30), true),
            new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(21, 0), false),
            new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(19, 45), false),
            new WeeklySlot(DayOfWeek.THURSDAY, LocalTime.of(19, 45), false)
    );

    public OffsetDateTime findNextAvailableSlot(int runtime, Movie movie, Guild guild) {

        ZonedDateTime searchBase = ZonedDateTime.now(ZONE);

        List<ScheduledEvent> events = guild.retrieveScheduledEvents().complete();

        while (true) {

            for (WeeklySlot s : slots) {

                if (!s.longAllowed && runtime > 150) //max duration of 150min on weekdays
                    continue; // skip invalid slot for long movies

                OffsetDateTime start = nextOccurrence(s.day, s.time, searchBase);
                OffsetDateTime end = start.plusMinutes(runtime + BUFFER_MINUTES);

                if (!conflicts(events, guild, movie, start, end)) {
                    return start;
                }
            }

            //move search one week forward
            searchBase = searchBase.plusWeeks(1);
        }
    }

    private OffsetDateTime nextOccurrence(DayOfWeek day, LocalTime time, ZonedDateTime base) {


        LocalDate date = base.toLocalDate();

        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }

        ZonedDateTime candidate = ZonedDateTime.of(date, time, ZONE);

        // If this week's time already passed then use next week
        if (candidate.isBefore(base)) {
            candidate = candidate.plusWeeks(1);
        }

        return candidate.toOffsetDateTime();
    }

    private boolean conflicts(List<ScheduledEvent> events, Guild guild, Movie movie, OffsetDateTime start, OffsetDateTime end) {

        var movieChannel = guild.getVoiceChannels().stream()
                .filter(voiceChannel -> voiceChannel.getName().equals("🍿movie-theatre")).findFirst().orElse(null);

        if (movieChannel == null){
            System.err.println("Error: Could not find 🍿movie-theatre voice channel.");
            return true; // safer to block than to accidentally overlap
        }

        for (ScheduledEvent event : events) {

            //ignore events for other channels
            if (!event.getChannel().getId().equals(movieChannel.getId()))
                continue;

            //prevent duplicate movies
            if (event.getName().equals("Movie Night - " + movie.getTitle()))
                return true;

            OffsetDateTime eStart = event.getStartTime();
            OffsetDateTime eEnd = event.getEndTime();

            if( eEnd == null){
                eEnd = eStart.plusHours(3); //assume 3 hours if dc has no end time
            }

            boolean overlap =
                    !(end.isBefore(eStart) || start.isAfter(eEnd));

            if (overlap) {
                System.out.println("Slot blocked by existing event: " + event.getName());
                return true;
            }
        }

        return false;
    }

    public void createDiscordEvent(Guild guild, Movie movie, OffsetDateTime start, OffsetDateTime end) {

        var channel = guild.getVoiceChannels().stream()
                .filter(vc -> vc.getName().equals("🍿movie-theatre"))
                .findFirst()
                .orElse(null);

        if (channel == null) {
            System.err.println("Error: Could not find 🍿movie-theatre voice channel.");
            return;
        }



        var action = guild.createScheduledEvent("Movie Night - " + movie.getTitle(), channel, start)
                .setEndTime(end)
                .setDescription("Movie Night: " + movie.getTitle() + " (" + movie.getYear() + ")");

                /*
                try (InputStream stream = new URL(movie.getPosterURL()).openStream()) {

                    Icon icon = Icon.from(stream);
                    action = action.setImage(icon);

                } catch (Exception e) {
                    System.err.println("Failed to load poster for " + movie.getTitle());
                    e.printStackTrace();
                }
                */

                action.queue(
                        success -> System.out.println("Created event for: " + movie.getTitle()),
                        Throwable::printStackTrace
                );
    }
}
