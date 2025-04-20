package com.sn0wqt.privacyfreak.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserState {
    public enum Mode {
        NONE, METADATA, STRIP
    }

    private final Map<Long, Mode> pending = new ConcurrentHashMap<>();

    public Mode getPending(long chatId) {
        return pending.getOrDefault(chatId, Mode.NONE);
    }

    public void setPending(long chatId, Mode mode) {
        pending.put(chatId, mode);
    }

    public boolean isAwaitingMedia(long chatId) {
        return getPending(chatId) != Mode.NONE;
    }
}