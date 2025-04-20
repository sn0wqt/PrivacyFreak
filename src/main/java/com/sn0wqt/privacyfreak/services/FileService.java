package com.sn0wqt.privacyfreak.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;

public class FileService {
    private final OkHttpTelegramClient telegramClient;
    private final String botToken;

    public FileService(OkHttpTelegramClient telegramClient, String botToken) {
        this.telegramClient = telegramClient;
        this.botToken = botToken;
    }

    /**
     * Downloads a Telegram file (document) into memory.
     */
    public ByteArrayInputStream downloadFileInMemory(String fileId) throws Exception {
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