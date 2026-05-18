package ru.gr0946x.net.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

public class DatabaseConfig {
    private static final String URL = "jdbc:sqlite:kareta.db";

    public static JdbcTemplate jdbcTemplate() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(URL);
        return new JdbcTemplate(ds);
    }
}