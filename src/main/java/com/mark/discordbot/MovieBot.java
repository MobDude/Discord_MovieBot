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

    public static void main(String[] args) throws LoginException {

        System.out.println("Working dir: " + System.getProperty("user.dir"));
        System.out.println(".env exists: " + new java.io.File(".env").exists());

        // Load .env
        String token = System.getenv("DISCORD_TOKEN");

        if (token == null) {
            System.out.println("ERROR: DISCORD_TOKEN not found in .env");
            return;
        }

        // Build JDA bot
        JDABuilder.createDefault(token)
                .setActivity(Activity.watching("Movie Nights"))
                .addEventListeners(new MovieBot())
                .build();

        System.out.println("MovieBot is now running!");
    }

}
