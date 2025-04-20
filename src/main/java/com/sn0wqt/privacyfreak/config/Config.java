package com.sn0wqt.privacyfreak.config;

import java.util.List;
import java.util.Set;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

public class Config {
    // Bot commands
    public static final List<BotCommand> COMMANDS = List.of(
            new BotCommand("start", "Start the bot"),
            new BotCommand("help", "Show help message"),
            new BotCommand("metadata", "Show image/video metadata"),
            new BotCommand("strip", "Strip metadata from image (jpg/jpeg)"),
            new BotCommand("cancel", "Cancel the current operation"));

    // Supported formats
    public static final Set<String> SUPPORTED_METADATA_FORMATS = Set.of(
            "jpg", "jpeg", "tiff", "tif",
            "arw", "cr2", "nef", "orf", "rw2", "raf",
            "psd", "png", "bmp", "gif", "ico", "pcx", "webp",
            "avi", "wav", "mov", "qt", "mp4", "m4v", "mp3", "eps",
            "heic", "heif", "avif");

    public static final Set<String> SUPPORTED_STRIPPING_FORMATS = Set.of(
            "jpg", "jpeg");
}