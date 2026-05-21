package ru.gr0946x.net.repository.impl;

import org.mindrot.jbcrypt.BCrypt;
import jakarta.persistence.EntityManagerFactory;
import ru.gr0946x.net.entity.UserEntity;
import ru.gr0946x.net.repository.UserRepository;

import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {

    private final EntityManagerFactory emf;

    public UserRepositoryImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Optional<UserEntity> findByUsernameIgnoreCase(String username) {
        var em = emf.createEntityManager();
        try {
            var list = em.createQuery(
                            "SELECT u FROM UserEntity u WHERE LOWER(u.username) = LOWER(:name)",
                            UserEntity.class)
                    .setParameter("name", username)
                    .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } finally {
            em.close();
        }
    }

    @Override
    public boolean existsByUsernameIgnoreCase(String username) {
        return findByUsernameIgnoreCase(username).isPresent();
    }

    @Override
    public UserEntity save(UserEntity user) {
        var em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            UserEntity saved = em.merge(user);
            em.getTransaction().commit();
            return saved;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}