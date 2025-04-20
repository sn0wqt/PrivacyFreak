package com.sn0wqt.privacyfreak.services;

import java.io.InputStream;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MessageService {
    private final OkHttpTelegramClient telegramClient;

    public MessageService(OkHttpTelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public void sendText(long chatId, String text) {
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

    public void sendDocument(long chatId, InputStream in, String filename, String caption)
            throws TelegramApiException {
        InputFile file = new InputFile(in, filename);
        SendDocument send = SendDocument.builder()
                .chatId(chatId)
                .document(file)
                .caption(caption)
                .build();
        telegramClient.execute(send);
    }
}