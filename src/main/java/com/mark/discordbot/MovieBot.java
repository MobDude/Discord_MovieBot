package com.mark.discordbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Main entry point and event handler for MovieBot.
 * <p>
 * This class initializes the Discord bot, registers slash commands, and handles all user interactions related to movie
 * management, including adding, removing, listing, and scheduling movies.
 * </p>
 */
public class MovieBot extends ListenerAdapter
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
     * Persistent storage for movie breaks.
     */
    private final BreakStorage breakStorage;

    /**
     * Client for querying the TMDb API.
     */
    private final TMDb tmdb;

    /**
     * Scheduler used to determine movie night times and create Discord scheduled events.
     */
    private final MovieScheduler scheduler;

    private final SlotStorage slotStorage;

    private int maxMovies = 16;


    public MovieBot(String tmdbKey) {
        this.tmdb = new TMDb(tmdbKey);
        this.storage = new MovieStorage();
        this.scheduler = new MovieScheduler();
        this.slotStorage = new SlotStorage();
        this.breakStorage = new BreakStorage();
        this.scheduler.setSlots(slotStorage.getSlots());

        for (LocalDate date : breakStorage.getBreaks()){
            this.scheduler.addBreak(date);
        }
    }


    /**
     * Application entry point.
     * <p>
     * Loads environment variables, initializes JDA, registers slash commands, and starts the bot.
     * </p>
     */
    public static void main(String[] args) throws InterruptedException {

        String token = getConfigValue("DISCORD_TOKEN");
        if (token == null) {
            System.out.println("ERROR: DISCORD_TOKEN not found in environment variables or .env");
            return;
        }

        String tmdbKey = getConfigValue("TMDB_KEY");
        if (tmdbKey == null) {
            System.out.println("ERROR: TMDB_KEY not found in environment variables or .env");
            return;
        }


        // Build JDA bot
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.watching("/movielist"))
                .addEventListeners(new MovieBot(tmdbKey))
                .build();
        try {
            jda.awaitReady(); // blocks until connected
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Bot startup interrupted");
        }
        jda.updateCommands()
                .addCommands(
                        //add movie slash command
                        Commands.slash("addmovie", "Adds a movie to the list")
                                .addOption(OptionType.STRING, "name", "Movie title", true)
                                .addOption(OptionType.INTEGER, "year", "Release year", false),

                        //remove movie slash command
                        Commands.slash("removemovie", "Removes a movie from the list")
                                .addOption(OptionType.STRING, "query", "Part of the movie name", true),

                        //show list slash command
                        Commands.slash("movielist", "Shows the movie list"),

                        //add help command
                        Commands.slash("moviehelp", "Displays command help for the Movie Bot."),

                        Commands.slash("maxmovies", "Set the maximum number of movies in the move list.")
                                .addOption(OptionType.INTEGER, "max", "Maximum number of movies", true),

                        Commands.slash("timeslots", "Lists the movie scheduling time slots."),

                        Commands.slash("addtimeslot", "Adds a movie scheduling time slot.")
                                .addOption(OptionType.STRING, "day", "Day of week, for example sunday or tue", true)
                                .addOption(OptionType.STRING, "time", "24-hour time in HH:mm format, for example 19:45", true)
                                .addOption(OptionType.BOOLEAN, "long_allowed", "Whether movies over 150 minutes can use this slot", true),

                        Commands.slash("removetimeslot", "Removes a movie scheduling time slot.")
                                .addOption(OptionType.INTEGER, "index", "Slot number from /timeslots", true),

                        Commands.slash("edittimeslot", "Edits a movie scheduling time slot.")
                                .addOption(OptionType.INTEGER, "index", "Slot number from /timeslots", true)
                                .addOption(OptionType.STRING, "day", "Day of week, for example sunday or tue", true)
                                .addOption(OptionType.STRING, "time", "24-hour time in HH:mm format, for example 19:45", true)
                                .addOption(OptionType.BOOLEAN, "long_allowed", "Whether movies over 150 minutes can use this slot", true),

                        Commands.slash("movemovie", "Moves a movie to a different scheduling position.")
                                .addOption(OptionType.INTEGER, "from", "Current movie number from /movielist", true)
                                .addOption(OptionType.INTEGER, "to", "New movie number in the schedule", true),

                        Commands.slash("swapmovies", "Swaps two movies in the scheduling order.")
                                .addOption(OptionType.INTEGER, "first", "First movie number from /movielist", true)
                                .addOption(OptionType.INTEGER, "second", "Second movie number from /movielist", true),

                        Commands.slash("break", "Skips a movie night on a specific date.")
                                .addOption(OptionType.STRING, "date", "Date to skip in YYYY-MM-DD format", true),

                        Commands.slash("removebreak", "Removes a scheduled movie-night break.")
                                .addOption(OptionType.STRING, "date", "Date to restore in YYYY-MM-DD format", true),

                        Commands.slash("breaks", "List all scheduled movie-night breaks.")

                )
                .queue();

        System.out.println("MovieBot is now running!");

        Thread.currentThread().join();
    }

    private static String getConfigValue(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return getDotEnvValue(key);
    }

    private static String getDotEnvValue(String key) {
        Path envPath = findDotEnvPath();
        if (envPath == null) {
            return null;
        }

        try {
            for (String rawLine : Files.readAllLines(envPath)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }

                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }

                String name = line.substring(0, equalsIndex).trim();
                if (!name.equals(key)) {
                    continue;
                }

                String value = line.substring(equalsIndex + 1).trim();
                return stripOptionalQuotes(value);
            }
        } catch (IOException e) {
            System.err.println("Failed to read .env: " + e.getMessage());
        }

        return null;
    }

    private static Path findDotEnvPath() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }

            current = current.getParent();
        }

        return null;
    }

    private static String stripOptionalQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }

    /**
     * Routes incoming slash commands to their respective handlers.
     * @param event the slash command interaction event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event){
        switch (event.getName()){
            case "addmovie":
                handleAddMovie(event);
                break;

            case "removemovie":
                handleRemoveMovie(event);
                break;

            case "movielist":
                handleMovieList(event);
                break;

            case "moviehelp":
                handleMovieHelp(event);
                break;

            case "maxmovies":
                handleMaxMovies(event);
                break;

            case "timeslots":
                handleTimeSlots(event);
                break;

            case "addtimeslot":
                handleAddTimeSlot(event);
                break;

            case "removetimeslot":
                handleRemoveTimeSlot(event);
                break;

            case "edittimeslot":
                handleEditTimeSlot(event);
                break;

            case "movemovie":
                handleMoveMovie(event);
                break;

            case "swapmovies":
                handleSwapMovies(event);
                break;

            case "break":
                handleBreak(event);
                break;

            case "removebreak":
                handleRemoveBreak(event);
                break;

            case "breaks":
                handleBreaks(event);
                break;

            default:
                event.reply("Unknown command.").setEphemeral(true).queue();
        }
    }

    /**
     * Sets the maximum allowed movies in the movie list.
     * @param event the slash command interaction event.
     */
    private void handleMaxMovies(SlashCommandInteractionEvent event) {

        var opt = event.getOption("max");
        int num = opt != null ? opt.getAsInt() : 0;

        if (!requireAdmin(event)) return;

        //If inputted number is invalid
        if (num < 0){
            event.reply("Please input an number greater than or equal to 0.").setEphemeral(true).queue();
            return;
        }

        maxMovies = num;
        event.reply("Set max movies to " + maxMovies + ".").setEphemeral(true).queue();
    }

    private void handleTimeSlots(SlashCommandInteractionEvent event) {
        if (!requireGuildForReply(event)) return;

        List<MovieScheduler.WeeklySlot> slots = slotStorage.getSlots();
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            description.append(i + 1)
                    .append(". ")
                    .append(formatSlot(slots.get(i)))
                    .append('\n');
        }

        if (description.isEmpty()) {
            description.append("No time slots are configured.");
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Movie Time Slots");
        embed.setColor(0x570000);
        embed.setDescription(description.toString());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleAddTimeSlot(SlashCommandInteractionEvent event) {
        if (!requireAdmin(event)) return;

        MovieScheduler.WeeklySlot slot = parseSlotOptions(event);
        if (slot == null) return;

        slotStorage.addSlot(slot);
        refreshSchedulerSlots();
        resyncScheduledEvents(event.getGuild());

        event.reply("Added time slot: " + formatSlot(slot)).setEphemeral(true).queue();
    }

    private void handleRemoveTimeSlot(SlashCommandInteractionEvent event) {
        if (!requireAdmin(event)) return;

        List<MovieScheduler.WeeklySlot> slots = slotStorage.getSlots();
        if (slots.size() <= 1) {
            event.reply("At least one time slot must remain configured.").setEphemeral(true).queue();
            return;
        }

        int index = Objects.requireNonNull(event.getOption("index")).getAsInt() - 1;
        if (index < 0 || index >= slots.size()) {
            event.reply("Invalid slot number. Use `/timeslots` to see the current slot numbers.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        MovieScheduler.WeeklySlot removed = slots.get(index);
        slotStorage.removeSlot(index);
        refreshSchedulerSlots();
        resyncScheduledEvents(event.getGuild());

        event.reply("Removed time slot: " + formatSlot(removed)).setEphemeral(true).queue();
    }

    private void handleEditTimeSlot(SlashCommandInteractionEvent event) {
        if (!requireAdmin(event)) return;

        int index = Objects.requireNonNull(event.getOption("index")).getAsInt() - 1;
        List<MovieScheduler.WeeklySlot> slots = slotStorage.getSlots();
        if (index < 0 || index >= slots.size()) {
            event.reply("Invalid slot number. Use `/timeslots` to see the current slot numbers.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        MovieScheduler.WeeklySlot slot = parseSlotOptions(event);
        if (slot == null) return;

        slotStorage.updateSlot(index, slot);
        refreshSchedulerSlots();
        resyncScheduledEvents(event.getGuild());

        event.reply("Updated time slot " + (index + 1) + " to: " + formatSlot(slot))
                .setEphemeral(true)
                .queue();
    }

    private void handleMoveMovie(SlashCommandInteractionEvent event) {
        if (!requireAdmin(event)) return;

        List<Movie> movies = storage.getMovies();
        if (movies.isEmpty()) {
            event.reply("The movie list is currently empty.").setEphemeral(true).queue();
            return;
        }

        int fromIndex = Objects.requireNonNull(event.getOption("from")).getAsInt() - 1;
        int toIndex = Objects.requireNonNull(event.getOption("to")).getAsInt() - 1;
        if (!isValidMovieIndex(fromIndex, movies) || !isValidMovieIndex(toIndex, movies)) {
            event.reply("Invalid movie number. Use `/movielist` to see the current movie numbers.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Movie movie = movies.get(fromIndex);
        if (fromIndex == toIndex) {
            event.reply("That movie is already in position " + (toIndex + 1) + ".")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        storage.moveMovie(fromIndex, toIndex);
        resyncScheduledEvents(event.getGuild());

        event.reply("Moved **" + movie.getTitle() + "** from #" + (fromIndex + 1) + " to #" + (toIndex + 1) + ".")
                .setEphemeral(true)
                .queue();
    }

    private void handleSwapMovies(SlashCommandInteractionEvent event) {
        if (!requireAdmin(event)) return;

        List<Movie> movies = storage.getMovies();
        if (movies.isEmpty()) {
            event.reply("The movie list is currently empty.").setEphemeral(true).queue();
            return;
        }

        int firstIndex = Objects.requireNonNull(event.getOption("first")).getAsInt() - 1;
        int secondIndex = Objects.requireNonNull(event.getOption("second")).getAsInt() - 1;
        if (!isValidMovieIndex(firstIndex, movies) || !isValidMovieIndex(secondIndex, movies)) {
            event.reply("Invalid movie number. Use `/movielist` to see the current movie numbers.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Movie firstMovie = movies.get(firstIndex);
        Movie secondMovie = movies.get(secondIndex);
        if (firstIndex == secondIndex) {
            event.reply("Pick two different movie positions to swap.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        storage.swapMovies(firstIndex, secondIndex);
        resyncScheduledEvents(event.getGuild());

        event.reply("Swapped #" + (firstIndex + 1) + " **" + firstMovie.getTitle() + "** with #" +
                        (secondIndex + 1) + " **" + secondMovie.getTitle() + "**.")
                .setEphemeral(true)
                .queue();
    }

    private void handleMovieHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("MovieBot Help");
        embed.setDescription("Possible commands: ");

        embed.addField(
                "/addmovie", """
                        Adds a movie to the movie list.
                        
                        **Options:**
                        `name` (required) - Movie Title
                        `year` (optional) - Release Year
                        """, false
        );

        embed.addField(
                "/removemovie",
                """
                        Removes a movie from the list using its name.
                        
                        **Options:**
                        `query` (required) - Movie Title""", false
        );

        embed.addField(
                "/movielist", "Shows all movies currently in the list.", false
        );

        embed.addField(
                "/moviehelp", "Displays this help message.", false
        );

        embed.addField(
                "/maxmovies", """
                                    Set the max number of movies in the movie list.
                                    
                                    **Options**
                                    'max' (required) - Max number of Movies
                                    """, false

        );

        embed.addField(
                "/timeslots", "Shows the configured movie scheduling slots.", false
        );

        embed.addField(
                "/addtimeslot", """
                        Adds a scheduling slot. Admin only.
                        
                        **Options:**
                        `day` (required) - Day of week
                        `time` (required) - 24-hour HH:mm time
                        `long_allowed` (required) - Whether long movies can use this slot
                        """, false
        );

        embed.addField(
                "/removetimeslot", """
                        Removes a scheduling slot. Admin only.
                        
                        **Options:**
                        `index` (required) - Slot number from /timeslots
                        """, false
        );

        embed.addField(
                "/edittimeslot", """
                        Edits a scheduling slot. Admin only.
                        
                        **Options:**
                        `index` (required) - Slot number from /timeslots
                        `day` (required) - Day of week
                        `time` (required) - 24-hour HH:mm time
                        `long_allowed` (required) - Whether long movies can use this slot
                        """, false
        );

        embed.addField(
                "/movemovie", """
                        Moves a movie to a different scheduling position. Admin only.
                        
                        **Options:**
                        `from` (required) - Current movie number from /movielist
                        `to` (required) - New movie number in the schedule
                        """, false
        );

        embed.addField(
                "/swapmovies", """
                        Swaps two movies in the scheduling order. Admin only.
                        
                        **Options:**
                        `first` (required) - First movie number from /movielist
                        `second` (required) - Second movie number from /movielist
                        """, false
        );

        embed.addField(
                "/break", """
                        Skip a movie night on a specific date.
                        
                        **Options:**
                        'date' (required) Date to skip formatted as YYYY-MM-DD
                        """, false
        );

        embed.addField(
                "/removebreak", """
                        Restore a previously skipped movie night on a specific date.
                        
                        **Options:**
                        'date' (required) Date to restore formatted as YYYY-MM-DD
                        """, false
        );

        embed.addField(
                "/breaks", """
                        List scheduled movie-night breaks.
                      
                        """, false
        );

        embed.setFooter("MovieBot");
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    /**
     * Handles the /addmovie slash command.
     * <p>
     * Searches TMDb for matching movies, allows the user to select the correct on if multiple results are found,
     * stores the movie, and schedules a Discord event if possible.
     * </p>
     */
    private void handleAddMovie(SlashCommandInteractionEvent event){

        event.deferReply().setEphemeral(true).queue();

        if(storage.getMovies().size() >= maxMovies){
            event.getHook().sendMessage("Maximum number of movies are scheduled. Please try again later.").setEphemeral(true).queue();
            return;
        }

        String name = Objects.requireNonNull(event.getOption("name")).getAsString();
        Integer year = event.getOption("year") != null ? Objects.requireNonNull(event.getOption("year")).getAsInt() : null;

        if (!requireGuild(event)) return;

        JsonArray results = tmdb.searchMovies(name, year);

        if (results == null || results.isEmpty()){
            event.getHook().sendMessage("No movies found with that name.").setEphemeral(true).queue();
            return;
        }

        if (results.size() == 1){

            Movie movie = buildMovieFromTmdb(results.get(0).getAsJsonObject());

            boolean exists = storage.getMovies().stream()
                    .anyMatch(existing ->
                            existing.getTitle().equalsIgnoreCase(movie.getTitle()) &&
                                    existing.getYear() == movie.getYear()
                    );

            if (exists) {
                event.getHook().sendMessage("That movie is already in the list.").queue();
                return;
            }

            addMovieAndSchedule(movie, event.getGuild());

            event.getHook().sendMessage("Added **" + movie.getTitle() + "** (" + movie.getYear() + ")").setEphemeral(true).queue();
            return;

        }

        sendMovieSelectionMenu(event, results, name);

    }

    /**
     * Handles the /removemovie slash command.
     * <p>
     * Removes a movie from the stored list, prompting the user
     * to disambiguate if multiple matches are found.
     * </p>
     */
    private void handleRemoveMovie(SlashCommandInteractionEvent event){
        String query = Objects.requireNonNull(event.getOption("query")).getAsString();
        List<Movie> allMovies = storage.getMovies();

        event.deferReply().setEphemeral(true).queue(); // ACKNOWLEDGE ONCE

        if (!requireGuild(event)) return;

        // Search for movies containing the query (case-insensitive)
        List<Integer> matchingIndexes = new ArrayList<>();
        for (int i = 0; i < allMovies.size(); i++) {
            Movie m = allMovies.get(i);
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) {
                matchingIndexes.add(i);
            }
        }

        if (matchingIndexes.isEmpty()) {
            event.getHook().sendMessage("I couldn't find any movies matching **" + query + "**.").setEphemeral(true).queue();
            return;
        }

        // If only one match, delete immediately.
        if (matchingIndexes.size() == 1) {
            Movie movie = allMovies.get(matchingIndexes.getFirst());
            deleteScheduledEventIfPresent(movie, event.getGuild()); //remove scheduled event before deleting movie
            storage.removeMovie(movie);
            resyncScheduledEvents(event.getGuild());

            event.getHook()
                    .sendMessage("Removed **" + movie.getTitle() + "** from the movie list.").setEphemeral(true)
                    .queue();
            return;
        }

        // Multiple matches, build dropdown.
        StringSelectMenu.Builder menu = StringSelectMenu.create("remove-movie-select");

        for (int index : matchingIndexes) {
            Movie m = allMovies.get(index);
            menu.addOption(
                    m.getTitle() + " (" + m.getYear() + ")",
                    "remove:" + index
            );
        }


        event.getHook()
                .editOriginal("I found multiple movies:")
                .setComponents(ActionRow.of(menu.build()))
                .queue();
    }

    /**
     * Handles the /movielist slash command.
     * <p>
     * Displays the current movie list with pagination controls.
     * </p>
     */
    private void handleMovieList(SlashCommandInteractionEvent event) {
        List<Movie> movies = storage.getMovies();

        if (event.getGuild() == null) {
            event.reply("This command can only be used inside a server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (movies.isEmpty()) {
            event.reply("The movie list is currently empty.").queue();
            return;
        }

        int page = 0; // always start at page 0

        var embed = buildMovieListEmbed(page);
        var buttons = buildPageButtons(page);

        event.replyEmbeds(embed)
                .addComponents(ActionRow.of(buttons.get(0), buttons.get(1), buttons.get(2)))
                .queue();
    }

    /**
     * Handles adding a break.
     */
    private void handleBreak(SlashCommandInteractionEvent event){
        if (!requireAdmin(event)) return;
        if (!requireGuildForReply(event)) return;

        String dateText = Objects.requireNonNull(event.getOption("date")).getAsString();

        LocalDate date;

        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            event.reply(
                    "Invalid date. Use `YYYY-MM-DD`, for example `2026-10-06`."
            ).setEphemeral(true).queue();
            return;
        }

        if (scheduler.isBreak(date)) {
            event.reply("There is already a movie-night break on " + date + ".")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        breakStorage.addBreak(date);
        scheduler.addBreak(date);

        resyncScheduledEvents(event.getGuild());

        event.reply(
                "Skipped movie night on **" + date + "**. " +
                        "The remaining movie schedule has been shifted forward."
        ).setEphemeral(true).queue();
    }

    /**
     * Handles restoring a break.
     */
    private void handleRemoveBreak(SlashCommandInteractionEvent event) {
        if (!requireAdmin(event)) return;
        if (!requireGuildForReply(event)) return;

        String dateText = Objects.requireNonNull(event.getOption("date")).getAsString();

        LocalDate date;

        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            event.reply(
                    "Invalid date. Use `YYYY-MM-DD`, for example `2026-10-06`."
            ).setEphemeral(true).queue();
            return;
        }

        if (!scheduler.isBreak(date)) {
            event.reply("There is no movie-night break on " + date + ".")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        breakStorage.removeBreak(date);
        scheduler.removeBreak(date);

        resyncScheduledEvents(event.getGuild());

        event.reply(
                "Removed the movie-night break on **" + date + "**. " +
                        "The movie schedule has been restored."
        ).setEphemeral(true).queue();
    }

    private void handleBreaks(SlashCommandInteractionEvent event){
        if (!requireAdmin(event)) return;
        if (!requireGuildForReply(event)) return;

        Set<LocalDate> breaks = breakStorage.getBreaks();

        if (breaks.isEmpty()) {
            event.reply("There are no scheduled movie-night breaks.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        StringBuilder message = new StringBuilder(
                "**Scheduled Movie-Night Breaks**\n\n"
        );

        breaks.stream()
                .sorted()
                .forEach(date ->
                        message.append("• `")
                                .append(date)
                                .append("`\n")
                );

        event.reply(message.toString())
                .setEphemeral(true)
                .queue();
    }

    private void sendMovieSelectionMenu(SlashCommandInteractionEvent event, JsonArray results, String query){
        StringSelectMenu.Builder menu = StringSelectMenu.create("movie_select").setPlaceholder("Select the correct movie");

        for (int i = 0; i < Math.min(results.size(), 25); i++){ //max of 25 options allowed by discord
            var movie = results.get(i).getAsJsonObject();

            String title = movie.get("title").getAsString();
            String releaseDate = movie.has("release_date") && !movie.get("release_date").isJsonNull()
                    ? movie.get("release_date").getAsString()
                    : "Unknown";

            int year = releaseDate.length() >=4 ? Integer.parseInt(releaseDate.substring(0,4)) : 0;
            String id = movie.get("id").getAsString();

            menu.addOption(title + " (" + year + ")", id);
        }

        event.getHook()
                .sendMessage("I found multiple results for **" + query + "**:")
                .addComponents(ActionRow.of(menu.build())).setEphemeral(true)
                .queue();
    }

    /**
     * Handles dropdown menu interactions for movie selection
     * and movie removal.
     *
     * @param event the string select interaction event
     */
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event){
        String id = event.getComponentId();
        if (!id.equals("remove-movie-select") && !id.equals("movie_select")) {
            return;
        }

        event.deferReply().setEphemeral(true).queue();

        //failsafe to prevent users from using dms, which would result in null guild.
        Guild guild = event.getGuild();
        if (guild == null){
            event.getHook().sendMessage("This action can only be used inside a server.").setEphemeral(true).queue();
            return;
        }

        if (id.equals("remove-movie-select")) {

            String raw = event.getValues().getFirst();
            int index = Integer.parseInt(raw.replace("remove:", ""));

            List<Movie> movies = storage.getMovies();

            if (index < 0 || index >= movies.size()) {
                event.getHook().sendMessage("That movie no longer exists.").setEphemeral(true).queue();
                return;
            }

            Movie movie = movies.get(index);
            deleteScheduledEventIfPresent(movie, guild); //remove scheduled event before deleting movie
            storage.removeMovie(movie);
            resyncScheduledEvents(guild);

            event.getHook().sendMessage("Removed **" + movie.getTitle() + "**.").setEphemeral(true).queue();
            return;
        }

        if(storage.getMovies().size() >= maxMovies){
            event.getHook().sendMessage("Maximum number of movies are scheduled. Please try again later.").setEphemeral(true).queue();
            return;
        }

        String selectedMovieId = event.getValues().getFirst();

        //get selected movie details
        JsonObject movieJson = fetchMovieById(selectedMovieId);

        if (movieJson == null) {
            event.getHook().sendMessage("Could not load movie data.").setEphemeral(true).queue();
            return;
        }

        Movie m = buildMovieFromTmdb(movieJson);

        boolean exists = storage.getMovies().stream()
                .anyMatch(existing ->
                        existing.getTitle().equalsIgnoreCase(m.getTitle()) &&
                                existing.getYear() == m.getYear()
                );

        if (exists) {
            event.getHook().sendMessage("That movie is already in the list.").queue();
            return;
        }

        addMovieAndSchedule(m, guild);

        event.getHook().sendMessage("Added **" + m.getTitle() + "** (" + m.getYear() + ") to the list!").setEphemeral(true).queue();
    }

    public JsonObject fetchMovieById(String id) {
        return tmdb.getMovieById(id);
    }

    // Build a MessageEmbed for a page (5 movies per page)
    private MessageEmbed buildMovieListEmbed(int page) {
        var movies = storage.getMovies();
        int totalPages = computeTotalPages(movies);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Movie List");
        eb.setColor(0x570000);
        eb.setFooter("Page " + (page + 1) + " of " + totalPages);

        if (movies.isEmpty()) {
            eb.setDescription("The list is empty.");
            return eb.build();
        }

        if(page == 0){
            Movie next = movies.getFirst();

            eb.setDescription("Next Up: " + next.getTitle() + " (" + next.getYear() + ")");
            if (next.getPosterURL() != null && !next.getPosterURL().isBlank()){
                eb.setImage(next.getPosterURL()); //set image
            }

            return eb.build();

        }

            int start = 1 + (page -1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, movies.size());

            for (int i = start; i < end; i++) {

                Movie m = movies.get(i);
                String heading = (i + 1) + ". " + m.getTitle();
                StringBuilder value = new StringBuilder("Year: " + m.getYear());

                if (m.getPosterURL() != null && !m.getPosterURL().isBlank()) {
                    value.append("\n[Poster](").append(m.getPosterURL()).append(")");
                    // you could also set the thumbnail to the first movie on page if you like
                }
                eb.addField(heading, value.toString(), false);
            }


        return eb.build();
    }

    // Build prev/next/refresh buttons for a given current page.
    private List<Button> buildPageButtons(int currentPage) {
        var movies = storage.getMovies();
        int totalPages = computeTotalPages(movies);

        Button prev = Button.primary("movie_page_prev_" + currentPage, "Previous")
                .withDisabled(currentPage == 0);

        Button next = Button.primary("movie_page_next_" + currentPage, "Next")
                .withDisabled(currentPage >= totalPages - 1);

        Button refresh = Button.secondary("movie_page_refresh_" + currentPage, "Refresh");

        return List.of(prev, next, refresh);
    }

    /**
     * Handles pagination button interactions for the movie list.
     *
     * @param event the button interaction event
     */
    @Override
    public void onButtonInteraction(
            net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {

        String id = event.getComponentId();
        if (!id.startsWith("movie_page_")) return;

        // Extract type and page:
        // movie_page_refresh_2 -> ["movie","page","refresh","2"]
        String[] parts = id.split("_");
        String action = parts[2];
        int currentPage = Integer.parseInt(parts[3]);

        var movies = storage.getMovies();
        int totalPages = computeTotalPages(movies);

        int newPage = switch (action) {
            case "prev" -> Math.max(0, currentPage - 1);
            case "next" -> Math.min(totalPages - 1, currentPage + 1);
            case "refresh" -> Math.min(currentPage, totalPages - 1);
            default -> currentPage;
        };

        var embed = buildMovieListEmbed(newPage);
        var buttons = buildPageButtons(newPage);

        event.editMessageEmbeds(embed)
                .setComponents(ActionRow.of(buttons.get(0), buttons.get(1), buttons.get(2)))
                .queue();
    }

    private int computeTotalPages(List<Movie> movies){
        if (movies.isEmpty()){
            return  1;
        }
        return Math.max(1, (int) Math.ceil((movies.size() -1) / (double) PAGE_SIZE) + 1);
    }

    private void addMovieAndSchedule(Movie movie, Guild guild) {
        storage.addMovie(movie);
        resyncScheduledEvents(guild);
    }

    private void resyncScheduledEvents(Guild guild) {
        if (guild == null) {
            return;
        }

        scheduler.resyncAllEvents(guild, storage.getMovies());
    }

    private void refreshSchedulerSlots() {
        scheduler.setSlots(slotStorage.getSlots());
    }

    private boolean isValidMovieIndex(int index, List<Movie> movies) {
        return index >= 0 && index < movies.size();
    }

    private MovieScheduler.WeeklySlot parseSlotOptions(SlashCommandInteractionEvent event) {
        String dayText = Objects.requireNonNull(event.getOption("day")).getAsString();
        String timeText = Objects.requireNonNull(event.getOption("time")).getAsString();
        boolean longAllowed = Objects.requireNonNull(event.getOption("long_allowed")).getAsBoolean();

        DayOfWeek day;
        try {
            day = parseDayOfWeek(dayText);
        } catch (IllegalArgumentException e) {
            event.reply("Invalid day. Use a weekday like `sunday`, `monday`, or `thu`.")
                    .setEphemeral(true)
                    .queue();
            return null;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(timeText);
        } catch (DateTimeParseException e) {
            event.reply("Invalid time. Use 24-hour `HH:mm` format, for example `19:45`.")
                    .setEphemeral(true)
                    .queue();
            return null;
        }

        return new MovieScheduler.WeeklySlot(day, time, longAllowed);
    }

    private DayOfWeek parseDayOfWeek(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "monday", "mon" -> DayOfWeek.MONDAY;
            case "tuesday", "tue", "tues" -> DayOfWeek.TUESDAY;
            case "wednesday", "wed" -> DayOfWeek.WEDNESDAY;
            case "thursday", "thu", "thur", "thurs" -> DayOfWeek.THURSDAY;
            case "friday", "fri" -> DayOfWeek.FRIDAY;
            case "saturday", "sat" -> DayOfWeek.SATURDAY;
            case "sunday", "sun" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Unknown day: " + value);
        };
    }

    private String formatSlot(MovieScheduler.WeeklySlot slot) {
        return formatDay(slot.day()) + " " + slot.time() +
                " - long movies " + (slot.longAllowed() ? "allowed" : "not allowed");
    }

    private String formatDay(DayOfWeek day) {
        String name = day.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
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

        int runtime = 0;
        try {
            runtime = tmdb.getRuntime(movieJson.get("id").getAsInt());
        }catch (Exception e){
            System.err.println("Failed to fetch runtime.");
        }

        return new Movie(title, year, poster, runtime);
    }

    private boolean requireGuild(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.getHook()
                    .sendMessage("This command can only be used inside a server.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    private boolean requireGuildForReply(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("This command can only be used inside a server.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    private boolean requireAdmin(SlashCommandInteractionEvent event) {
        var member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    /**
     * Deletes a scheduled event if it exists.
     * @param movie the movie to remove
     * @param guild the guild to remove the movie from
     */
    private void deleteScheduledEventIfPresent(Movie movie, Guild guild){
        Long eventId = movie.getScheduledEventId();
        if(eventId == null) return;

        guild.retrieveScheduledEventById(eventId).queue(
                event -> event.delete().queue(
                        success -> System.out.println("Deleted event for " +movie.getTitle()),
                        error -> System.out.println("Failed to delete event for " + movie.getTitle())
                ),
                error -> System.err.println("Scheduled event not found for " + movie.getTitle())
        );
    }
}
