package com.mark.discordbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to load the token and API key, initialize JDA, register commands, and handle events
 */
public class MovieBot extends ListenerAdapter
{
    private static final int PAGE_SIZE = 5;
    private final MovieStorage storage;
    private final TMDb tmdb;
    private final MovieScheduler scheduler;

    public MovieBot(String tmdbKey) {
        this.tmdb = new TMDb(tmdbKey);
        this.storage = new MovieStorage();
        this.scheduler = new MovieScheduler();
    }

    public static void main(String[] args) throws InterruptedException {

        // Load .env
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null) {
            System.out.println("ERROR: DISCORD_TOKEN not found in .env");
            return;
        }

        String tmdbKey = System.getenv("TMDB_KEY");
        if (tmdbKey == null) {
            System.out.println("ERROR: TMDB_KEY not found in .env");
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
            e.printStackTrace();
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
                        Commands.slash("moviehelp", "Displays command help for the Movie Bot.")
                )
                .queue();

        System.out.println("MovieBot is now running!");

        Thread.currentThread().join();
    }


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
        }
    }

    private void handleMovieHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("MovieBot Help");
        embed.setDescription("Possible commands: ");

        embed.addField(
                "/addmovie", """
                        Adds a movie to the movie list.
                        **Options:**
                        'name' (required) - Movie Title
                        'year' (optional) - Release Year
                        """, false
        );

        embed.addField(
                "/removemovie",
                """
                        Removes a movie from the list using its name.
                        **Options:**
                        `query` (required) – Movie Title""", false
        );

        embed.addField(
                "/movielist", "Shows all movies currently in the list.", false
        );

        embed.addField(
                "/moviehelp", "Displays this help message.", false
        );

        embed.setFooter("MovieBot");
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleAddMovie(SlashCommandInteractionEvent event){
        String name = event.getOption("name").getAsString();
        Integer year = event.getOption("year") != null ? event.getOption("year").getAsInt() : null;

        event.deferReply().setEphemeral(true).queue();

        JsonArray results = tmdb.searchMovies(name, year);

        if (results.isEmpty()){
            event.getHook().sendMessage("No movies found with that name.").setEphemeral(true).queue();
            return;
        }

        if (results.size() == 1){
            JsonObject m = results.get(0).getAsJsonObject();

            String title = m.get("title").getAsString();
            int releaseYear = (m.has("release_date") && !m.get("release_date").getAsString().isEmpty())
                    ? Integer.parseInt(m.get("release_date").getAsString().substring(0, 4))
                    : 0;

            String poster = m.get("poster_path").isJsonNull() ? null :
                    "https://image.tmdb.org/t/p/w500" + m.get("poster_path").getAsString();

            int id = m.get("id").getAsInt();
            int runtime = tmdb.getRuntime(id);

            Movie movie = new Movie(title, releaseYear, poster, runtime);
            storage.addMovie(movie);

            Guild guild = event.getGuild();
            OffsetDateTime start = scheduler.findNextAvailableSlot(runtime, movie, guild);

            if( start != null){
                OffsetDateTime end = start.plusMinutes(runtime + 15);
                scheduler.createDiscordEvent(guild, movie, start, end);
            }

            event.getHook().sendMessage("Added **" + title + "** (" + releaseYear + ")").setEphemeral(true).queue();
            return;

        }

        sendMovieSelectionMenu(event, results, name);


    }

    private void handleRemoveMovie(SlashCommandInteractionEvent event){
        String query = event.getOption("query").getAsString();
        List<Movie> allMovies = storage.getMovies();

        event.deferReply().setEphemeral(true).queue(); // ACKNOWLEDGE ONCE

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

        // If only one match → delete immediately
        if (matchingIndexes.size() == 1) {
            Movie movie = allMovies.get(matchingIndexes.getFirst());
            storage.removeMovie(movie);

            event.getHook()
                    .sendMessage("Removed **" + movie.getTitle() + "** from the movie list.").setEphemeral(true)
                    .queue();
            return;
        }

        // MULTIPLE MATCHES → build dropdown
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

    private void handleMovieList(SlashCommandInteractionEvent event) {
        List<Movie> movies = storage.getMovies();

        if (movies.isEmpty()) {
            event.reply("The movie list is currently empty.").queue();
            return;
        }

        int page = 0; // always start at page 0

        var embed = buildMovieListEmbed(page);
        var buttons = buildPageButtons(page);

        event.replyEmbeds(embed)
                .addComponents(ActionRow.of(buttons.get(0), buttons.get(1)))
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

            menu.addOption(title + "(" + year + ")", id);
        }

        event.getHook()
                .sendMessage("I found multiple results for **" + query + "**:")
                .addComponents(ActionRow.of(menu.build())).setEphemeral(true)
                .queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event){

        event.deferReply().setEphemeral(true).queue();

        String id = event.getComponentId();

        if (id.equals("remove-movie-select")) {

            // Payload looks like: "remove:7"
            String raw = event.getValues().getFirst();
            int index = Integer.parseInt(raw.replace("remove:", ""));

            List<Movie> movies = storage.getMovies();

            if (index < 0 || index >= movies.size()) {
                event.reply("That movie no longer exists.").setEphemeral(true).setEphemeral(true).queue();
                return;
            }

            Movie movie = movies.get(index);
            storage.removeMovie(movie);

            event.reply("🗑Removed **" + movie.getTitle() + "**.").setEphemeral(true).queue();
            return;
        }

        if (!event.getComponentId().equals("movie_select")) return;

        String selectedMovieId = event.getValues().getFirst();

        //get selected movie details
        JsonObject movie = fetchMovieById(selectedMovieId);

        if (movie == null) {
            event.reply("Could not load movie data.").setEphemeral(true).queue();
            return;
        }

        String title = movie.get("title").getAsString();
        int year = movie.has("release_date") && !movie.get("release_date").isJsonNull()
                ? Integer.parseInt(movie.get("release_date").getAsString().substring(0, 4))
                : 0;

        String poster = movie.has("poster_path") && !movie.get("poster_path").isJsonNull()
                ? "https://image.tmdb.org/t/p/w500" + movie.get("poster_path").getAsString()
                : null;

        int runtime = tmdb.getRuntime(movie.get("id").getAsInt());
        Movie m = new Movie(title, year, poster, runtime);
        storage.addMovie(m);

        Guild guild = event.getGuild();
        OffsetDateTime start = scheduler.findNextAvailableSlot(runtime, m, guild);

        if (start != null) {
            OffsetDateTime end = start.plusMinutes(runtime + 15);
            scheduler.createDiscordEvent(guild, m, start, end);
        }

        event.getHook().sendMessage("Added **" + title + "** (" + year + ") to the list!").setEphemeral(true).queue();
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

    // Build prev/next buttons for a given current page. Returns List<Button>
    private List<Button> buildPageButtons(int currentPage) {
        var movies = storage.getMovies();
        int totalPages = computeTotalPages(movies);

        Button prev = Button.primary("movie_page_prev_" + currentPage, "◀ Previous")
                .withDisabled(currentPage == 0);

        Button next = Button.primary("movie_page_next_" + currentPage, "Next ▶")
                .withDisabled(currentPage >= totalPages - 1);

        return List.of(prev, next);
    }

    @Override
    public void onButtonInteraction(
            net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {

        String id = event.getComponentId();
        if (!id.startsWith("movie_page_")) return;

        // Extract type and page:
        // movie_page_prev_2  → ["movie","page","prev","2"]
        String[] parts = id.split("_");
        String action = parts[2];         // "prev" or "next"
        int currentPage = Integer.parseInt(parts[3]);

        var movies = storage.getMovies();
        int totalPages = computeTotalPages(movies);

        int newPage = action.equals("prev")
                ? Math.max(0, currentPage - 1)
                : Math.min(totalPages - 1, currentPage + 1);

        var embed = buildMovieListEmbed(newPage);
        var buttons = buildPageButtons(newPage);

        event.editMessageEmbeds(embed)
                .setComponents(ActionRow.of(buttons.get(0), buttons.get(1)))
                .queue();
    }

    private int computeTotalPages(List<Movie> movies){
        if (movies.isEmpty()){
            return  1;
        }
        return Math.max(1, (int) Math.ceil((movies.size() -1) / (double) PAGE_SIZE) + 1);
    }


}
