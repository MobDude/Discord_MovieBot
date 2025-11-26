package com.mark.discordbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.components.actionrow.ActionRow;

/**
 * Class to load the token and API key, initialize JDA, register commands, and handle events
 */
public class MovieBot extends ListenerAdapter
{
    private static MovieStorage storage;
    private static TMDb tmdb;

    public static void main(String[] args) throws LoginException {

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

        tmdb = new TMDb(tmdbKey);
        storage = new MovieStorage();

        // Build JDA bot
        JDABuilder.createDefault(token)
                .setActivity(Activity.watching("Movie Nights"))
                .addEventListeners(new MovieBot())
                .build()
                .updateCommands()
                .addCommands(
                        //add movie slash command
                        Commands.slash("addmovie", "Adds a movie to the list")
                                .addOption(OptionType.STRING, "name", "Movie title", true)
                                .addOption(OptionType.INTEGER, "year", "Release year", false),

                        //remove movie slash command
                        Commands.slash("removemovie", "Removes a movie from the list")
                                .addOption(OptionType.STRING, "query", "Part of the movie name", true),

                        //show list slash command
                        Commands.slash("movielist", "Shows the movie list")
                )
                .queue();


        System.out.println("MovieBot is now running!");
    }

    public static MovieStorage getStorage(){
        return  storage;
    }

    public static TMDb getTmdb(){
        return tmdb;
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
        }
    }

    private void handleAddMovie(SlashCommandInteractionEvent event){
        String name = event.getOption("name").getAsString();
        Integer year = event.getOption("year") != null ? event.getOption("year").getAsInt() : null;

        event.deferReply().queue();

        var results = MovieBot.getTmdb().searchMovies(name, year);

        if (results.size() == 0){
            event.getHook().sendMessage("No movies found with that name.").queue();
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

            Movie movie = new Movie(title, releaseYear, poster);
            MovieBot.getStorage().addMovie(movie);

            event.getHook().sendMessage("Added **" + title + "** (" + releaseYear + ")").queue();
            return;

        }

        event.deferReply().queue(hook -> {
            // MANY RESULTS → show dropdown
            sendMovieSelectionMenu(event, results, name);
        });

    }

    private void handleRemoveMovie(SlashCommandInteractionEvent event){
        event.reply("Not Implemented yet");
    }

    private void handleMovieList(SlashCommandInteractionEvent event){
        event.reply("Not Implemented yet");
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
                .addComponents(ActionRow.of(menu.build()))
                .queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event){
        if (!event.getComponentId().equals("movie_select")) return;

        String selectedMovieId = event.getValues().get(0);

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

        Movie m = new Movie(title, year, poster);
        MovieBot.getStorage().addMovie(m);

        event.reply("Added **" + title + "** (" + year + ") to the list!").queue();
    }

    public static JsonObject fetchMovieById(String id) {
        return tmdb.getMovieById(id);
    }


}
