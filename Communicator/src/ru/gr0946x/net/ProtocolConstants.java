package ru.gr0946x.net;

public final class ProtocolConstants {
    public static final int DEFAULT_PORT = 9460;
    public static final String SEP = ":";
    public static final String LIST_SEP = ",";
    public static final int HISTORY_LIMIT = 20;

    // Оставляем старые константы чтобы не ломать Communicator.java
    public static final String COMMAND_SEPARATOR = ":";
    public static final String AUTHOR_SEPARATOR = ":";

    private ProtocolConstants() {}

    // ── Билдеры ──────────────────────────────────────────────────────────

    public static String login(String u, String p) {
        return MessageType.LOGIN + SEP + u + SEP + p;
    }
    public static String register(String u, String p) {
        return MessageType.REGISTER + SEP + u + SEP + p;
    }
    public static String authOk(String username) {
        return MessageType.AUTH_OK + SEP + username;
    }
    public static String authFail(String reason) {
        return MessageType.AUTH_FAIL + SEP + reason;
    }
    public static String msg(String from, String to, String text) {
        return MessageType.MSG + SEP + from + SEP + to + SEP + sanitize(text);
    }
    public static String broadcast(String from, String text) {
        return MessageType.BROADCAST + SEP + from + SEP + sanitize(text);
    }
    public static String userList(String csv) {
        return MessageType.USER_LIST + SEP + csv;
    }
    public static String historyRequest(String peer) {
        return MessageType.HISTORY_REQUEST + SEP + peer;
    }
    public static String historyItem(String from, String to, String text, String dt) {
        return MessageType.HISTORY_ITEM + SEP + from + SEP + to + SEP + sanitize(text) + SEP + dt;
    }
    public static String historyEnd() {
        return MessageType.HISTORY_END + SEP;
    }
    public static String searchRequest(String peer, String keyword) {
        return MessageType.SEARCH_REQUEST + SEP + peer + SEP + sanitize(keyword);
    }
    public static String searchItem(String from, String to, String text, String dt) {
        return MessageType.SEARCH_ITEM + SEP + from + SEP + to + SEP + sanitize(text) + SEP + dt;
    }
    public static String searchEnd() {
        return MessageType.SEARCH_END + SEP;
    }
    public static String disconnect() {
        return MessageType.DISCONNECT + SEP;
    }

    // ── Парсер ───────────────────────────────────────────────────────────

    public static MessageType parseType(String raw) {
        if (raw == null || raw.isBlank()) return MessageType.ERROR;
        try {
            return MessageType.valueOf(raw.split(SEP, 2)[0].trim());
        } catch (IllegalArgumentException e) {
            return MessageType.ERROR;
        }
    }

    /** Всё после первого ":" */
    public static String payload(String raw) {
        if (raw == null) return "";
        int i = raw.indexOf(SEP);
        return i >= 0 ? raw.substring(i + 1) : "";
    }

    /** Разбить payload на maxParts частей (последняя вберёт остаток) */
    public static String[] parts(String raw, int maxParts) {
        return payload(raw).split(SEP, maxParts);
    }

    /** Убрать переносы строк из текста (сообщение — одна строка) */
    private static String sanitize(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", "");
    }
}