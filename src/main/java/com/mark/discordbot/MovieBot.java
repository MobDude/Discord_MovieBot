package com.mark.discordbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.security.auth.login.LoginException;


/**
 * Class to load the token and API key, initialize JDA, register commands, and handle events
 */
public class MovieBot extends ListenerAdapter
{
    private static MovieStorage storage;


    public static void main(String[] args) throws LoginException {

        // Load .env
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null) {
            System.out.println("ERROR: DISCORD_TOKEN not found in .env");
            return;
        }

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
        event.reply("Not Implemented yet");
    }

    private void handleRemoveMovie(SlashCommandInteractionEvent event){
        event.reply("Not Implemented yet");
    }

    private void handleMovieList(SlashCommandInteractionEvent event){
        event.reply("Not Implemented yet");
    }

}
