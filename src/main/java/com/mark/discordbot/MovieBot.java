package com.mark.discordbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import javax.security.auth.login.LoginException;
//import io.github.cdimascio.dotenv.Dotenv;

/**
 * Class to load the token and API key, initialize JDA, register commands, and handle events
 */
public class MovieBot extends ListenerAdapter
{
    public static void main(String[] args) throws LoginException {
//        //load env var
//        Dotenv dotenv = Dotenv.load();
//        String token = dotenv.get("DISCORD_TOKEN");
//
//        //Initialize JDA
//        JDABuilder.createDefault(token).setActivity(Activity.watching("Movie Nights")).addEventListeners(new MovieBot()).build();
    }

//    @Override
//    public void onSlashCommandInteraction(SlashCommandInteractionEvent event){
//
//    }


}
