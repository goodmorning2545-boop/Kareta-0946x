package ru.gr0946x.net.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.time.LocalDateTime;
import java.util.List;

public class MessageRepository {
    private final JdbcTemplate jdbc;

    public MessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record MsgRecord(String senderName, String receiverName, String text, String sentAt) {}

    private final RowMapper<MsgRecord> mapper = (rs, i) -> new MsgRecord(
            rs.getString("sender"),
            rs.getString("receiver"),
            rs.getString("text"),
            rs.getString("sent_at")
    );

    public void save(int senderId, Integer receiverId, String text) {
        jdbc.update("""
            INSERT INTO messages (sender_id, receiver_id, text, sent_at, delivered)
            VALUES (?, ?, ?, ?, 0)
        """, senderId, receiverId, text, LocalDateTime.now().toString());
    }

    /** Последние N сообщений между двумя пользователями */
    public List<MsgRecord> getHistory(int userId, int peerId, int limit) {
        return jdbc.query("""
            SELECT u1.username AS sender,
                   COALESCE(u2.username, 'all') AS receiver,
                   m.text, m.sent_at
            FROM messages m
            JOIN users u1 ON m.sender_id = u1.id
            LEFT JOIN users u2 ON m.receiver_id = u2.id
            WHERE (m.sender_id = ? AND m.receiver_id = ?)
               OR (m.sender_id = ? AND m.receiver_id = ?)
            ORDER BY m.id DESC LIMIT ?
        """, mapper, userId, peerId, peerId, userId, limit);
    }

    /** Поиск по части слова в переписке двух пользователей */
    public List<MsgRecord> search(int userId, int peerId, String keyword) {
        return jdbc.query("""
            SELECT u1.username AS sender,
                   COALESCE(u2.username, 'all') AS receiver,
                   m.text, m.sent_at
            FROM messages m
            JOIN users u1 ON m.sender_id = u1.id
            LEFT JOIN users u2 ON m.receiver_id = u2.id
            WHERE ((m.sender_id = ? AND m.receiver_id = ?)
                OR (m.sender_id = ? AND m.receiver_id = ?))
              AND m.text LIKE ?
            ORDER BY m.id DESC LIMIT 50
        """, mapper, userId, peerId, peerId, userId, "%" + keyword + "%");
    }
}