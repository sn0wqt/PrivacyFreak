package com.sn0wqt.privacyfreak;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class Bot implements LongPollingSingleThreadUpdateConsumer {
    private enum State {
        IDLE, AWAIT_METADATA, AWAIT_STRIP
    }

    private final Map<Long, State> chatStates = new ConcurrentHashMap<>();

    List<BotCommand> commands = List.of(
            new BotCommand("start", "Start the bot"),
            new BotCommand("help", "Show help message"),
            new BotCommand("metadata", "Show image/video metadata"),
            new BotCommand("strip", "Strip metadata from image/video"),
            new BotCommand("cancel", "Cancel the current operation"));

    private final TelegramClient telegramClient;

    private void promptForMetadata(long chatId) {
        sendText(chatId,
                "Please send me a photo or video to get its metadata.");
    }

    private void promptForStrip(long chatId) {
        sendText(chatId,
                "Please send me a photo or video to strip its metadata.");
    }

    private void showHelp(long chatId) {
        sendText(chatId,
                "I can help you with the following commands:\n" +
                        "/metadata - Show metadata of an image/video\n" +
                        "/strip    - Strip metadata from an image/video\n" +
                        "/cancel   - Cancel the current operation");
    }

    private void cancel(long chatId) {
        sendText(chatId, "Operation cancelled.");
    }

    private void sendText(long chatId, String text) {
        State state = chatStates.getOrDefault(chatId, State.IDLE);
        InlineKeyboardMarkup markup = switch (state) {
            case IDLE -> fullMenu();
            case AWAIT_METADATA, AWAIT_STRIP -> cancelOnly();
        };

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(markup)
                .build();
        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup fullMenu() {
        // build your two rows
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder().text("📊 Metadata").callbackData("CMD_METADATA").build());
        row1.add(InlineKeyboardButton.builder().text("✂️ Strip").callbackData("CMD_STRIP").build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(InlineKeyboardButton.builder().text("❓ Help").callbackData("CMD_HELP").build());
        row2.add(InlineKeyboardButton.builder().text("🚫 Cancel").callbackData("CMD_CANCEL").build());

        // **Pass a List** of rows, not the rows as varargs
        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2))
                .build();
    }

    private InlineKeyboardMarkup cancelOnly() {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder().text("🚫 Cancel").callbackData("CMD_CANCEL").build());

        // A single-row list
        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row))
                .build();
    }

    public Bot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        SetMyCommands setCommands = SetMyCommands.builder()
                .commands(commands)
                .build();

        try {
            telegramClient.execute(setCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (data) {
                case "CMD_METADATA" -> {
                    chatStates.put(chatId, State.AWAIT_METADATA);
                    promptForMetadata(chatId);
                }
                case "CMD_STRIP" -> {
                    chatStates.put(chatId, State.AWAIT_STRIP);
                    promptForStrip(chatId);
                }
                case "CMD_HELP" -> {
                    chatStates.put(chatId, State.IDLE);
                    showHelp(chatId);
                }
                case "CMD_CANCEL" -> {
                    chatStates.put(chatId, State.IDLE);
                    cancel(chatId);
                }
            }
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String txt = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();

            switch (txt) {
                case "/start" -> {
                    chatStates.put(chatId, State.IDLE);
                    sendText(chatId,
                            "👋 Welcome!\nChoose an action from the menu below:");
                }
                case "/help" -> {
                    chatStates.put(chatId, State.IDLE);
                    showHelp(chatId);
                }
                case "/metadata" -> {
                    // flip state here too!
                    chatStates.put(chatId, State.AWAIT_METADATA);
                    promptForMetadata(chatId);
                }
                case "/strip" -> {
                    // and here
                    chatStates.put(chatId, State.AWAIT_STRIP);
                    promptForStrip(chatId);
                }
                case "/cancel" -> {
                    chatStates.put(chatId, State.IDLE);
                    cancel(chatId);
                }
                default -> sendText(chatId,
                        "Unknown command. Use /help or /start to see available options.");
            }
        }

    }

}