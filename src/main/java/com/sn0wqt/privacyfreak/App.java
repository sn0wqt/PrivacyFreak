package com.sn0wqt.privacyfreak;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;

import com.sn0wqt.privacyfreak.config.Config;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
    public static void main(String[] args) {
        // Load environment variables
        Dotenv dotenv = Dotenv.load();
        String botToken = dotenv.get("BOT_TOKEN");

        try (TelegramBotsLongPollingApplication botsApp = new TelegramBotsLongPollingApplication()) {
            // Start the long‑polling loop
            botsApp.registerBot(botToken, new Bot(botToken));

            // Register slash‑commands
            var setCommands = SetMyCommands.builder()
                    .commands(Config.COMMANDS)
                    .build();

            var client = new OkHttpTelegramClient(botToken);
            client.execute(setCommands);

            System.out.println("Bot successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}