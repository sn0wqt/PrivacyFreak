package com.sn0wqt.privacyfreak;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.drew.imaging.ImageProcessingException;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
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

    // Set of supported file extensions for metadata reading
    private final Set<String> SUPPORTED_METADATA_FORMATS = Set.of(
            "jpg", "jpeg", "tiff", "tif",
            "arw", "cr2", "nef", "orf", "rw2", "raf",
            "psd", "png", "bmp", "gif", "ico", "pcx", "webp",
            "avi", "wav", "mov", "qt", "mp4", "m4v", "mp3", "eps",
            "heic", "heif", "avif");

    // Set of supported file extensions for metadata stripping
    private final Set<String> SUPPORTED_STRIPPING_FORMATS = Set.of(
            "jpg", "jpeg");

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
                    "👋 Welcome! I can inspect or strip metadata from files.\n" +
                            "Use /metadata or /strip commands.");
            case "/help" -> {
                StringBuilder helpText = new StringBuilder();
                helpText.append("Commands:\n")
                        .append("/metadata – inspect metadata\n")
                        .append("/strip    – strip metadata\n")
                        .append("/cancel   – cancel\n\n")
                        .append("⚠️ Note: Send media as FILE attachments only.\n")
                        .append("Regular photo/video messages are already compressed by Telegram.\n\n");

                helpText.append("Supported formats for metadata reading:\n")
                        .append("• Images: JPEG, TIFF, PNG, BMP, GIF, WebP, PSD, HEIF/AVIF\n")
                        .append("• RAW Files: ARW (Sony), CR2 (Canon), NEF (Nikon), ORF (Olympus), RW2 (Panasonic), RAF (Fujifilm)\n")
                        .append("• Videos: MP4, QuickTime/MOV, AVI\n")
                        .append("• Audio: WAV, MP3\n")
                        .append("• Other: EPS, ICO, PCX\n\n");

                helpText.append("Supported formats for metadata stripping:\n")
                        .append("• Images Only: JPEG/JPG only\n");

                sendText(chatId, helpText.toString());
            }
            case "/metadata" -> {
                pending.put(chatId, Mode.METADATA);
                sendText(chatId, "Please send a file as a FILE attachment to inspect metadata.");
            }
            case "/strip" -> {
                pending.put(chatId, Mode.STRIP);
                sendText(chatId, "Please send a JPEG image or video as a FILE attachment to strip metadata.");
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
            // Check input based on mode
            boolean validInput = (mode == Mode.METADATA) ? (msg.hasDocument() || msg.hasVideo()) : msg.hasDocument();

            if (!validInput) {
                String errorMsg = (mode == Mode.METADATA)
                        ? "Please send a file as a FILE attachment (image or video) for metadata extraction, or /cancel."
                        : "Please send a JPEG image as a FILE attachment to strip metadata, or /cancel.";
                sendText(chatId, errorMsg);
                pending.put(chatId, Mode.NONE);
                return;
            }

            // Extract file details
            String fileId, filename;
            if (msg.hasVideo()) {
                Video video = msg.getVideo();
                fileId = video.getFileId();
                filename = video.getFileName();
                if (filename == null || filename.isEmpty()) {
                    filename = video.getFileId() + ".mp4";
                }
            } else {
                Document doc = msg.getDocument();
                fileId = doc.getFileId();
                filename = doc.getFileName();
                if (filename == null || filename.isEmpty()) {
                    filename = doc.getFileId();
                }
            }

            String ext = getFileExtension(filename).toLowerCase();

            // Check format compatibility
            Set<String> supportedFormats = (mode == Mode.METADATA) ? SUPPORTED_METADATA_FORMATS
                    : SUPPORTED_STRIPPING_FORMATS;

            if (!supportedFormats.contains(ext)) {
                String formatMsg = (mode == Mode.METADATA)
                        ? "⚠️ This file format (\"" + ext + "\") is not supported for metadata reading.\n" +
                                "Use /help to see supported formats."
                        : "⚠️ This file format (\"" + ext + "\") is not supported for metadata stripping.\n" +
                                "Only JPEG/JPG images can be stripped.\n" +
                                "Use /help to see supported formats.";
                sendText(chatId, formatMsg);
                pending.put(chatId, Mode.NONE);
                return;
            }

            // Download and process file
            InputStream in = downloadFileInMemory(fileId);

            if (mode == Mode.METADATA) {
                // Extract metadata
                String meta = Utils.readMetadata(in, filename);
                if (meta.isEmpty()) {
                    meta = "No metadata found in this file.";
                }

                // Send as text file
                ByteArrayInputStream metaStream = new ByteArrayInputStream(
                        meta.getBytes(StandardCharsets.UTF_8));
                InputFile txtFile = new InputFile(metaStream, "metadata_" + filename + ".txt");

                telegramClient.execute(SendDocument.builder()
                        .chatId(chatId)
                        .document(txtFile)
                        .caption("Metadata extracted from " + filename)
                        .build());
            } else {
                // Strip metadata (JPEG only)
                InputStream clean = Utils.stripImageMetadata(in);
                sendDocument(chatId, clean, "stripped_" + filename);
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

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
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

    private void sendDocument(long chatId, InputStream in, String filename) throws TelegramApiException {
        InputFile file = new InputFile(in, filename);
        SendDocument send = SendDocument.builder()
                .chatId(chatId)
                .document(file)
                .caption("Here's your file without metadata.")
                .build();
        telegramClient.execute(send);
    }

    /**
     * Downloads a Telegram file (document) into memory.
     */
    private ByteArrayInputStream downloadFileInMemory(String fileId) throws Exception {
        GetFile getFile = GetFile.builder()
                .fileId(fileId)
                .build();
        File tgFile = telegramClient.execute(getFile);

        URI uri = new URI(tgFile.getFileUrl(botToken));
        URL url = uri.toURL();

        try (InputStream is = url.openStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buf = new byte[8_192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }
}