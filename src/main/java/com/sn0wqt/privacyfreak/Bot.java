package com.sn0wqt.privacyfreak;

import java.util.Set;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.sn0wqt.privacyfreak.commands.CommandHandler;
import com.sn0wqt.privacyfreak.services.FileService;
import com.sn0wqt.privacyfreak.services.MediaHandler;
import com.sn0wqt.privacyfreak.services.MessageService;
import com.sn0wqt.privacyfreak.state.UserState;

public class Bot implements LongPollingSingleThreadUpdateConsumer {
    private final OkHttpTelegramClient telegramClient;
    private final UserState userState;
    private final CommandHandler commandHandler;
    private final MediaHandler mediaHandler;

    // Set of supported file extensions for metadata reading
    private static final Set<String> SUPPORTED_METADATA_FORMATS = Set.of(
            "jpg", "jpeg", "tiff", "tif",
            "arw", "cr2", "nef", "orf", "rw2", "raf",
            "psd", "png", "bmp", "gif", "ico", "pcx", "webp",
            "avi", "wav", "mov", "qt", "mp4", "m4v", "mp3", "eps",
            "heic", "heif", "avif");

    // Set of supported file extensions for metadata stripping
    private static final Set<String> SUPPORTED_STRIPPING_FORMATS = Set.of(
            "jpg", "jpeg");

    public Bot(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);

        // Initialize components
        MessageService messageService = new MessageService(telegramClient);
        FileService fileService = new FileService(telegramClient, botToken);
        this.userState = new UserState();
        this.commandHandler = new CommandHandler(messageService, userState);
        this.mediaHandler = new MediaHandler(messageService, fileService,
                SUPPORTED_METADATA_FORMATS, SUPPORTED_STRIPPING_FORMATS);
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage())
            return;
        Message msg = update.getMessage();
        long chatId = msg.getChatId();

        // Check if we're awaiting media for a prior command
        if (userState.isAwaitingMedia(chatId)) {
            mediaHandler.handleMedia(chatId, msg, userState.getPending(chatId));
            userState.setPending(chatId, UserState.Mode.NONE); // Reset state after handling
            return;
        }

        // Otherwise handle as a command
        if (msg.hasText()) {
            commandHandler.handleCommand(msg);
        }
    }
}