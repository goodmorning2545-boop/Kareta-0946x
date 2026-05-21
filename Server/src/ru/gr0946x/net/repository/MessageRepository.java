package ru.gr0946x.net.repository;

import ru.gr0946x.net.entity.MessageEntity;
import ru.gr0946x.net.entity.UserEntity;
import java.util.List;

public interface MessageRepository {
    MessageEntity save(MessageEntity message);
    List<MessageEntity> findHistory(UserEntity u1, UserEntity u2, int limit);
    List<MessageEntity> searchMessages(UserEntity u1, UserEntity u2, String keyword);
}