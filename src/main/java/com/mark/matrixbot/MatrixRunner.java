package com.mark.matrixbot;

import io.github.ma1uta.matrix.bot.api.MatrixBotApi;
import io.github.ma1uta.matrix.bot.api.callback.MessageCallback;
import io.github.ma1uta.matrix.bot.api.event.RoomMessage;
import io.github.ma1uta.matrix.client.exception.MatrixException;

public class MatrixRunner {

    public static void main(String[] args) {
        // Load from env or config
        String homeserver = System.getenv("MATRIX_HOMESERVER");
        String accessToken = System.getenv("MATRIX_ACCESS_TOKEN");

        MatrixBotApi bot = MatrixBotApi.builder()
                .baseUrl(homeserver)
                .accessToken(accessToken)
                .build();

        MovieBot movieBot = new MovieBot(System.getenv("TMDB_KEY"));

        // Register message handler
        bot.on(RoomMessage.class, (event, context) -> {
            String roomId = context.roomId();
            String sender = event.getSender();
            String message = event.getContent().getBody();

            String reply = movieBot.onMessage(roomId, sender, message);
            if (reply != null) {
                try {
                    bot.sendText(roomId, reply);
                } catch (MatrixException e) {
                    e.printStackTrace();
                }
            }
        });

        bot.startSyncLoop();
    }
}
