package com.sn0wqt.privacyfreak;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.drew.imaging.ImageProcessingException;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot implements LongPollingSingleThreadUpdateConsumer {

    private final OkHttpTelegramClient telegramClient;
    private final String botToken;

    private enum Mode {
        NONE, METADATA, STRIP
    }

    private final Map<Long, Mode> pending = new ConcurrentHashMap<>();

    public Bot(String botToken) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage())
            return;
        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        String text = msg.hasText() ? msg.getText().trim() : "";

        // 1) are we awaiting media for a prior /metadata or /strip?
        Mode mode = pending.getOrDefault(chatId, Mode.NONE);
        if (mode != Mode.NONE) {
            handleMedia(chatId, msg, mode);
            return;
        }

        // 2) otherwise treat as a command
        switch (text) {
            case "/start" -> sendText(chatId,
                    "👋 Welcome! I can inspect or strip metadata from images/videos.\n" +
                            "Use /metadata or /strip commands.");
            case "/help" -> sendText(chatId,
                    "Commands:\n" +
                            "/metadata – inspect metadata\n" +
                            "/strip    – strip metadata\n" +
                            "/cancel   – cancel");
            case "/metadata" -> {
                pending.put(chatId, Mode.METADATA);
                sendText(chatId, "Please send a photo, video, or file to inspect metadata.");
            }
            case "/strip" -> {
                pending.put(chatId, Mode.STRIP);
                sendText(chatId, "Please send a photo, video, or file to strip metadata.");
            }
            case "/cancel" -> {
                pending.put(chatId, Mode.NONE);
                sendText(chatId, "Operation cancelled.");
            }
            default -> sendText(chatId, "Unknown command. Use /help for options.");
        }
    }

    private void handleMedia(long chatId, Message msg, Mode mode) {
        try {
            // Determine fileId, filename, and media type
            String fileId;
            String filename;
            boolean isPhoto = false;
            boolean isVideo = false;
            boolean isDocument = false;

            if (msg.hasPhoto()) {
                List<PhotoSize> photos = msg.getPhoto();
                PhotoSize photo = photos.get(photos.size() - 1);
                fileId = photo.getFileId();
                filename = photo.getFileId() + ".jpg";
                isPhoto = true;
            } else if (msg.hasVideo()) {
                Video video = msg.getVideo();
                fileId = video.getFileId();
                String orig = video.getFileName();
                String ext = (orig != null && orig.contains("."))
                        ? orig.substring(orig.lastIndexOf('.') + 1)
                        : "mp4";
                filename = video.getFileId() + "." + ext;
                isVideo = true;
            } else if (msg.hasDocument()) {
                Document doc = msg.getDocument();
                fileId = doc.getFileId();
                filename = doc.getFileName();
                isDocument = true;
            } else {
                sendText(chatId, "Please send a photo, video, or file, or /cancel.");
                return;
            }

            // Download the file into memory
            InputStream in = downloadFileInMemory(fileId);

            if (mode == Mode.METADATA) {
                // Read metadata from any supported container
                String meta = Utils.readMetadata(in, filename);
                ByteArrayInputStream metaStream = new ByteArrayInputStream(meta.getBytes(StandardCharsets.UTF_8));
                InputFile txtFile = new InputFile(metaStream, "metadata.txt");

                SendDocument sendDoc = SendDocument.builder()
                        .chatId(chatId)
                        .document(txtFile)
                        .caption("Metadata was too long, so here’s a text file.")
                        .build();
                telegramClient.execute(sendDoc);
                // sendText(chatId, "```\n" + meta + "\n```");
            } else {
                // Strip metadata
                InputStream clean;
                String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

                if (isPhoto) {
                    clean = Utils.stripImageMetadata(in);
                    sendPhoto(chatId, clean, "stripped_" + filename);
                } else if (isVideo) {
                    clean = Utils.stripVideoMetadata(in, ext);
                    sendVideo(chatId, clean, "stripped_" + filename);
                } else if (isDocument) { // document fallback (could be image or video as file)
                    if (ext.matches("jpe?g|png|gif")) {
                        clean = Utils.stripImageMetadata(in);
                        sendDocument(chatId, clean, "stripped_" + filename);
                    } else {
                        clean = Utils.stripVideoMetadata(in, ext);
                        sendDocument(chatId, clean, "stripped_" + filename);
                    }
                }
            }
        } catch (ImageProcessingException e) {
            sendText(chatId, "⚠️ Metadata error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "❌ Error: " + e.getMessage());
        } finally {
            pending.put(chatId, Mode.NONE);
        }
    }

    private void sendText(long chatId, String text) {
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

    private void sendPhoto(long chatId, InputStream in, String filename) throws TelegramApiException {
        InputFile file = new InputFile(in, filename);
        SendPhoto send = SendPhoto.builder()
                .chatId(chatId)
                .photo(file)
                .caption("Here’s your image without metadata.")
                .build();
        telegramClient.execute(send);
    }

    private void sendVideo(long chatId, InputStream in, String filename) throws TelegramApiException {
        InputFile file = new InputFile(in, filename);
        SendVideo send = SendVideo.builder()
                .chatId(chatId)
                .video(file)
                .caption("Here’s your video without metadata.")
                .build();
        telegramClient.execute(send);
    }

    private void sendDocument(long chatId, InputStream in, String filename) throws TelegramApiException {
        InputFile file = new InputFile(in, filename);
        SendDocument send = SendDocument.builder()
                .chatId(chatId)
                .document(file)
                .caption("Here’s your file without metadata.")
                .build();
        telegramClient.execute(send);
    }

    /**
     * Downloads a Telegram file (photo, video, or document) into memory.
     */
    private ByteArrayInputStream downloadFileInMemory(String fileId) throws Exception {
        GetFile getFile = GetFile.builder()
                .fileId(fileId)
                .build();
        File tgFile = telegramClient.execute(getFile);

        URI uri = new URI(tgFile.getFileUrl(botToken));
        URL url = uri.toURL();

        try (InputStream is = url.openStream();
                var baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8_192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }
}
