package com.sn0wqt.privacyfreak;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.File;

/**
 * The Bot class handles incoming updates and dispatches commands.
 */
public class Bot implements LongPollingSingleThreadUpdateConsumer {

    private final OkHttpTelegramClient telegramClient;
    private final String botToken;

    public Bot(String botToken) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String txt = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();

        switch (txt) {
            case "/start" -> send(chatId,
                    "👋 Welcome! I can inspect or strip metadata from images/videos.\n" +
                            "Use /metadata or /strip commands.");
            case "/help" -> send(chatId,
                    "Available commands:\n" +
                            "/metadata - Show metadata of an image/video\n" +
                            "/strip    - Strip metadata from an image/video\n" +
                            "/cancel   - Cancel current operation");
            case "/metadata" -> send(chatId,
                    "Please send a photo or video to inspect its metadata.");
            case "/strip" -> send(chatId,
                    "Please send a photo or video to strip its metadata.");
            case "/cancel" -> send(chatId, "Operation cancelled.");
            default -> send(chatId, "Unknown command. Use /help for options.");
        }
    }

    /**
     * Sends a text message to the specified chat.
     */
    private void send(long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads a Telegram file (photo/video) into memory.
     * Returns a ByteArrayInputStream for further processing.
     */
    private ByteArrayInputStream downloadFileInMemory(String fileId) throws Exception {
        // Request file information
        GetFile getFile = GetFile.builder()
                .fileId(fileId)
                .build();
        File tgFile = telegramClient.execute(getFile);

        // Build URL for downloading
        URI uri = new URI(tgFile.getFileUrl(botToken));
        URL url = uri.toURL();

        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            is = url.openStream();
            baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return new ByteArrayInputStream(baos.toByteArray());
        } finally {
            if (is != null) {
                is.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
    }

    /**
     * Handles metadata extraction from a file.
     */

}
