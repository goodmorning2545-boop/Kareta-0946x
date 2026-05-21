package ru.gr0946x.net.repository.impl;

import jakarta.persistence.EntityManagerFactory;
import ru.gr0946x.net.entity.MessageEntity;
import ru.gr0946x.net.entity.UserEntity;
import ru.gr0946x.net.repository.MessageRepository;

import java.util.List;

public class MessageRepositoryImpl implements MessageRepository {

    private final EntityManagerFactory emf;

    public MessageRepositoryImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public MessageEntity save(MessageEntity message) {
        var em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            MessageEntity saved = em.merge(message);
            em.getTransaction().commit();
            return saved;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public List<MessageEntity> findHistory(UserEntity u1, UserEntity u2, int limit) {
        var em = emf.createEntityManager();
        try {
            return em.createQuery("""
                SELECT m FROM MessageEntity m
                WHERE (m.sender = :u1 AND m.receiver = :u2)
                   OR (m.sender = :u2 AND m.receiver = :u1)
                ORDER BY m.sentAt DESC
            """, MessageEntity.class)
                    .setParameter("u1", u1)
                    .setParameter("u2", u2)
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<MessageEntity> searchMessages(UserEntity u1, UserEntity u2, String keyword) {
        var em = emf.createEntityManager();
        try {
            return em.createQuery("""
                SELECT m FROM MessageEntity m
                WHERE ((m.sender = :u1 AND m.receiver = :u2)
                    OR (m.sender = :u2 AND m.receiver = :u1))
                  AND LOWER(m.text) LIKE LOWER(:keyword)
                ORDER BY m.sentAt DESC
            """, MessageEntity.class)
                    .setParameter("u1", u1)
                    .setParameter("u2", u2)
                    .setParameter("keyword", "%" + keyword + "%")
                    .setMaxResults(50)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}