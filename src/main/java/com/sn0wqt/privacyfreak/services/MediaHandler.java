package com.sn0wqt.privacyfreak.services;

import com.drew.imaging.ImageProcessingException;
import com.sn0wqt.privacyfreak.state.UserState.Mode;
import com.sn0wqt.privacyfreak.utils.FileUtils;
import com.sn0wqt.privacyfreak.utils.MetadataUtils;

import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class MediaHandler {
    private final MessageService messageService;
    private final FileService fileService;
    private final Set<String> supportedMetadataFormats;
    private final Set<String> supportedStrippingFormats;

    public MediaHandler(MessageService messageService, FileService fileService,
            Set<String> supportedMetadataFormats, Set<String> supportedStrippingFormats) {
        this.messageService = messageService;
        this.fileService = fileService;
        this.supportedMetadataFormats = supportedMetadataFormats;
        this.supportedStrippingFormats = supportedStrippingFormats;
    }

    public void handleMedia(long chatId, Message msg, Mode mode) {
        try {
            // Check input based on mode
            if (!isValidInput(chatId, msg, mode)) {
                return;
            }

            // Extract file details
            String fileId;
            String filename;
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

            String ext = FileUtils.getFileExtension(filename).toLowerCase();

            // Check format compatibility
            if (!isFormatSupported(chatId, ext, mode)) {
                return;
            }

            // Download and process file
            InputStream in = fileService.downloadFileInMemory(fileId);

            if (mode == Mode.METADATA) {
                processMetadataRequest(chatId, in, filename);
            } else {
                processStrippingRequest(chatId, in, filename);
            }

        } catch (ImageProcessingException e) {
            messageService.sendText(chatId, "⚠️ Metadata error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            messageService.sendText(chatId, "❌ Error: " + e.getMessage());
        }
    }

    private boolean isValidInput(long chatId, Message msg, Mode mode) {
        boolean validInput = (mode == Mode.METADATA) ? (msg.hasDocument() || msg.hasVideo()) : msg.hasDocument();

        if (!validInput) {
            String errorMsg = (mode == Mode.METADATA)
                    ? "Please send a file as a FILE attachment (image or video) for metadata extraction, or /cancel."
                    : "Please send a JPEG image as a FILE attachment to strip metadata, or /cancel.";
            messageService.sendText(chatId, errorMsg);
            return false;
        }
        return true;
    }

    private boolean isFormatSupported(long chatId, String ext, Mode mode) {
        Set<String> supportedFormats = (mode == Mode.METADATA) ? supportedMetadataFormats : supportedStrippingFormats;

        if (!supportedFormats.contains(ext)) {
            String formatMsg = (mode == Mode.METADATA)
                    ? "⚠️ This file format (\"" + ext + "\") is not supported for metadata reading.\n" +
                            "Use /help to see supported formats."
                    : "⚠️ This file format (\"" + ext + "\") is not supported for metadata stripping.\n" +
                            "Only JPEG/JPG images can be stripped.\n" +
                            "Use /help to see supported formats.";
            messageService.sendText(chatId, formatMsg);
            return false;
        }
        return true;
    }

    private void processMetadataRequest(long chatId, InputStream in, String filename)
            throws IOException, ImageProcessingException, TelegramApiException {
        // Extract metadata
        String meta = MetadataUtils.readMetadata(in, filename);
        if (meta.isEmpty()) {
            meta = "No metadata found in this file.";
        }

        // Send as text file
        ByteArrayInputStream metaStream = new ByteArrayInputStream(
                meta.getBytes(StandardCharsets.UTF_8));
        messageService.sendDocument(chatId, metaStream, "metadata_" + filename + ".txt",
                "Metadata extracted from " + filename);
    }

    private void processStrippingRequest(long chatId, InputStream in, String filename)
            throws IOException, TelegramApiException {
        // Strip metadata (JPEG only)
        InputStream clean = MetadataUtils.stripImageMetadata(in);
        messageService.sendDocument(chatId, clean, "stripped_" + filename,
                "Here's your file without metadata.");
    }
}