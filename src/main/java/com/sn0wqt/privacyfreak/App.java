package com.sn0wqt.privacyfreak;

import java.util.List;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
    static Dotenv dotenv = Dotenv.load();
    private static final String botToken = dotenv.get("BOT_TOKEN");

    static List<BotCommand> commands = List.of(
            new BotCommand("start", "Start the bot"),
            new BotCommand("help", "Show help message"),
            new BotCommand("metadata", "Show image/video metadata"),
            new BotCommand("strip", "Strip metadata from image/video"),
            new BotCommand("cancel", "Cancel the current operation"));

    public static void main(String[] args) {
        // 1) Start the long‑polling loop
        try (TelegramBotsLongPollingApplication botsApp = new TelegramBotsLongPollingApplication()) {
            botsApp.registerBot(botToken, new Bot(botToken));

            // 2) register slash‑commands via the same HTTP client:
            var setCommands = SetMyCommands.builder()
                    .commands(commands)
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
