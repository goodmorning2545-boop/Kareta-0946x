package ru.gr0946x.net.db;

import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseInitializer {
    private final JdbcTemplate jdbc;

    public DatabaseInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void initialize() {
        // Таблица 1: пользователи
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password TEXT NOT NULL
            )
        """);

        // Таблица 2: сообщения
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_id   INTEGER NOT NULL,
                receiver_id INTEGER,
                text        TEXT NOT NULL,
                sent_at     TEXT NOT NULL,
                delivered   INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (sender_id)   REFERENCES users(id),
                FOREIGN KEY (receiver_id) REFERENCES users(id)
            )
        """);

        System.out.println("[DB] Инициализирована.");
    }
}