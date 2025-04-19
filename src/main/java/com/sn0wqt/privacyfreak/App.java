package com.sn0wqt.privacyfreak;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import io.github.cdimascio.dotenv.Dotenv;

public class App {
    static Dotenv dotenv = Dotenv.load();
    private static final String botToken = dotenv.get("BOT_TOKEN");

    public static void main(String[] args) {
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new Bot(botToken));
            System.out.println("Bot successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
