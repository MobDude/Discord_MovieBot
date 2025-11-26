package com.mark.discordbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
                .build();

        System.out.println("MovieBot is now running!");
    }

    public static MovieStorage getStorage(){
        return  storage;
    }

}
