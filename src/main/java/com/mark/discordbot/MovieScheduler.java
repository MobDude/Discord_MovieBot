package com.mark.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.time.*;
import java.util.*;

/**
 * Handles the creation and scheduling of Discord "Movie Night"  scheduled events.
 * <p>
 *     This class determines the next available weekly timeslot for a movie, ensuring that there are no conflicts with
 *     existing Discord scheduled events and respecting runtime constraints for different days.
 * </p>
 */
public class MovieScheduler {

    /**
     * Extra buffer time (in minutes) added to a movies runtime to account for setup, delays, or discussion afterward.
     */
    private static final int BUFFER_MINUTES = 15;

    /**
     * Time zone used for all scheduling calculations.
     */
    private static final ZoneId ZONE = ZoneId.of("America/Toronto");

    /**
     * The max runtime of a movie during weekday movie slots in minutes.
     */
    private static final int MAX_WEEKDAY_RUNTIME = 150;

    /**
     * The default event duration if an event has no end time in hours.
     */
    private static final int DEFAULT_EVENT_DURATION_HOURS = 3;

    /**
     * The name of the voice channel where the events are scheduled.
     */
    private static final String MOVIE_CHANNEL_NAME = "🍿movie-theatre";
    /**
     * Represents a recurring weekly movie slot
     * @param day day of the week the slot occurs
     * @param time start time of the slot
     * @param longAllowed whether long movies are allowed in this slot
     */
    private record WeeklySlot(DayOfWeek day, LocalTime time, boolean longAllowed) {}

    /**
     * List of all allowed weekly movie slots.
     */
    private final List<WeeklySlot> slots = List.of(
            new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(18, 30), true),
            new WeeklySlot(DayOfWeek.SUNDAY, LocalTime.of(21, 0), false),
            new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(19, 45), false),
            new WeeklySlot(DayOfWeek.THURSDAY, LocalTime.of(19, 45), false)
    );

    /**
     * Finds the next available time slot for a movie that does not conflict with existing scheduled Discord events.
     * @param runtime movie runtime in minutes
     * @param movie the movie being scheduled
     * @param guild the Discord guild where the event will be created
     * @return the start time of the next available slot
     */
    public OffsetDateTime findNextAvailableSlot(int runtime, Movie movie, Guild guild) {

        ZonedDateTime searchBase = ZonedDateTime.now(ZONE);

        List<ScheduledEvent> events = guild.retrieveScheduledEvents().complete();

        while (true) {

            for (WeeklySlot slot : slots) {

                if (!slot.longAllowed && runtime > MAX_WEEKDAY_RUNTIME)
                    continue;

                OffsetDateTime start = nextOccurrence(slot.day, slot.time, searchBase);
                OffsetDateTime end = start.plusMinutes(runtime + BUFFER_MINUTES);

                if (!conflicts(events, guild, movie, start, end)) {
                    return start;
                }
            }

            //move search one week forward
            searchBase = searchBase.plusWeeks(1);
        }
    }

    /**
     * Calculates the next date and time a given weekly slot occurs relative to a base date.
     * @param day desired day of the week
     * @param time desired start time
     * @param base base date and time to search from
     * @return the next occurrence as an {@link OffsetDateTime}
     */
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

    /**
     * Checks whether a proposed movie event conflicts with existing scheduled events.
     * <p>
     *     Conflicts include overlapping time ranges, duplicate movie titles, or missing required Discord channels.
     * </p>
     * @param events list of existing scheduled events
     * @param guild the Discord guild
     * @param movie the movie being scheduled
     * @param start proposed start time
     * @param end proposed end time
     * @return {@code true} if a conflict exists, {@code false} otherwise
     */
    private boolean conflicts(List<ScheduledEvent> events, Guild guild, Movie movie, OffsetDateTime start, OffsetDateTime end) {

        var movieChannel = getMovieChannel(guild);

        //If the channel does not exist, blocks scheduling to avoid unsafe overlaps.
        if (movieChannel == null){
            System.err.println("Error: Could not find " + MOVIE_CHANNEL_NAME + " voice channel.");
            return true;
        }

        for (ScheduledEvent event : events) {
            var eventChannel = event.getChannel();

            //ignore events for other channels
            if (eventChannel == null || !event.getChannel().getId().equals(movieChannel.getId()))
                continue;

            //prevent duplicate movies
            if (event.getName().equals("Movie Night - " + movie.getTitle()))
                return true;

            OffsetDateTime eStart = event.getStartTime();
            OffsetDateTime eEnd = event.getEndTime();

            if( eEnd == null){
                eEnd = eStart.plusHours(DEFAULT_EVENT_DURATION_HOURS); //assume 3 hours if dc has no end time
            }

            if (overlaps(start, end, eStart, eEnd)) {
                System.out.println("Slot blocked by existing event: " + event.getName());
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether two time intervals overlap.
     * @param start1 the start of event one
     * @param end1 the end of event one
     * @param start2 the start of event two
     * @param end2 the end of event two
     * @return {@code true} if the two events overlap, {@code false} otherwise
     */
    private boolean overlaps(OffsetDateTime start1, OffsetDateTime end1,
                             OffsetDateTime start2, OffsetDateTime end2) {
        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }

    /**
     * Creates a scheduled Discord event for a movie night.
     * @param guild the Discord guild where the event will be created
     * @param movie the movie being shown
     * @param start event start time
     * @param end event end time
     */
    public void createDiscordEvent(Guild guild, Movie movie, OffsetDateTime start, OffsetDateTime end) {

        var channel = getMovieChannel(guild);

        if (channel == null) {
            System.err.println("Error: Could not find " + MOVIE_CHANNEL_NAME +" voice channel.");
            return;
        }

        var action = guild.createScheduledEvent("Movie Night - " + movie.getTitle(), channel, start)
                .setEndTime(end)
                .setDescription("Movie Night: " + movie.getTitle() + " (" + movie.getYear() + ")");

                action.queue(
                        event ->{
                            movie.setScheduledEventId(event.getIdLong());
                            System.out.println("Created event for: " + movie.getTitle() + " (eventId=" + event.getId() + ")");
                        },
                        error ->{
                            System.err.println("Failed to create event for " + movie.getTitle());
                            error.printStackTrace();
                        }
                );
    }

    /**
     * Retrieves the movie theatre voice channel from the guild.
     *
     * @param guild the Discord guild
     * @return the movie theatre voice channel, or {@code null} if not found
     */
    private VoiceChannel getMovieChannel(Guild guild) {
        return guild.getVoiceChannels().stream()
                .filter(vc -> vc.getName().equals(MOVIE_CHANNEL_NAME))
                .findFirst()
                .orElse(null);
    }
}
