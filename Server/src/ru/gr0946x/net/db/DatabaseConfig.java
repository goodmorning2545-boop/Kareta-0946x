package ru.gr0946x.net.db;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import ru.gr0946x.net.entity.MessageEntity;
import ru.gr0946x.net.entity.UserEntity;
import ru.gr0946x.net.repository.MessageRepository;
import ru.gr0946x.net.repository.UserRepository;
import ru.gr0946x.net.repository.impl.MessageRepositoryImpl;
import ru.gr0946x.net.repository.impl.UserRepositoryImpl;

public class DatabaseConfig {

    private static EntityManagerFactory emf;

    public static EntityManagerFactory entityManagerFactory() {
        if (emf == null) {
            emf = new Configuration()
                    .addAnnotatedClass(UserEntity.class)
                    .addAnnotatedClass(MessageEntity.class)
                    .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                    .setProperty("hibernate.connection.url", "jdbc:sqlite:kareta.db")
                    .setProperty("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect")
                    .setProperty("hibernate.hbm2ddl.auto", "update")
                    .setProperty("hibernate.show_sql", "false")
                    .buildSessionFactory();
        }
        return emf;
    }

    public static UserRepository userRepository() {
        return new UserRepositoryImpl(entityManagerFactory());
    }

    public static MessageRepository messageRepository() {
        return new MessageRepositoryImpl(entityManagerFactory());
    }
}