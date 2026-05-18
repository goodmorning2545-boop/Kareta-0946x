package ru.gr0946x.net.db;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;

public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record UserRecord(int id, String username, String password) {}

    public Optional<UserRecord> findByUsername(String username) {
        var list = jdbc.query(
                "SELECT id, username, password FROM users WHERE username = ? COLLATE NOCASE",
                (rs, i) -> new UserRecord(rs.getInt("id"), rs.getString("username"), rs.getString("password")),
                username
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** true = успешно зарегистрирован, false = имя занято */
    public boolean register(String username, String password) {
        try {
            jdbc.update("INSERT INTO users (username, password) VALUES (?, ?)",
                    username.toLowerCase(), password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Integer> findIdByUsername(String username) {
        return findByUsername(username).map(UserRecord::id);
    }
}