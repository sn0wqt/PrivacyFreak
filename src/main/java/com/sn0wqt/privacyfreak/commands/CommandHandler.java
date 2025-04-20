package com.sn0wqt.privacyfreak.commands;

import com.sn0wqt.privacyfreak.services.MessageService;
import com.sn0wqt.privacyfreak.state.UserState;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public class CommandHandler {
    private final MessageService messageService;
    private final UserState userState;

    public CommandHandler(MessageService messageService, UserState userState) {
        this.messageService = messageService;
        this.userState = userState;
    }

    public void handleCommand(Message message) {
        long chatId = message.getChatId();
        String text = message.hasText() ? message.getText().trim() : "";

        switch (text) {
            case "/start" -> messageService.sendText(chatId,
                    "👋 Welcome! I can inspect or strip metadata from files.\n" +
                            "Use /metadata or /strip commands.");
            case "/help" -> sendHelpMessage(chatId);
            case "/metadata" -> {
                userState.setPending(chatId, UserState.Mode.METADATA);
                messageService.sendText(chatId, "Please send a file as a FILE attachment to inspect metadata.");
            }
            case "/strip" -> {
                userState.setPending(chatId, UserState.Mode.STRIP);
                messageService.sendText(chatId,
                        "Please send a JPEG image or video as a FILE attachment to strip metadata.");
            }
            case "/cancel" -> {
                userState.setPending(chatId, UserState.Mode.NONE);
                messageService.sendText(chatId, "Operation cancelled.");
            }
            default -> messageService.sendText(chatId, "Unknown command. Use /help for options.");
        }
    }

    private void sendHelpMessage(long chatId) {
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

        messageService.sendText(chatId, helpText.toString());
    }
}