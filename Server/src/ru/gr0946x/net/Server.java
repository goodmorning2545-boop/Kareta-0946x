package ru.gr0946x.net;

import ru.gr0946x.net.db.MessageRepository;
import ru.gr0946x.net.db.UserRepository;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    private boolean isActive;

    public Server(int port, UserRepository userRepo, MessageRepository msgRepo) {
        isActive = true;
        new Thread(() -> {
            try (var serverSocket = new ServerSocket(port)) {
                System.out.println("[Server] Запущен на порту " + port);
                while (isActive) {
                    try {
                        var socket = serverSocket.accept();
                        System.out.println("[Server] Новое подключение: " + socket.getInetAddress());
                        var client = new ConnectedClient(socket, userRepo, msgRepo);
                        client.start();
                    } catch (Exception e) {
                        System.err.println("[Server] Ошибка подключения: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[Server] Ошибка запуска: " + e.getMessage());
            }
        }).start();
    }
}