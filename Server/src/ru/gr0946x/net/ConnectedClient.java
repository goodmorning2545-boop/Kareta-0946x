package ru.gr0946x.net;

import org.mindrot.jbcrypt.BCrypt;
import ru.gr0946x.net.entity.MessageEntity;
import ru.gr0946x.net.entity.UserEntity;
import ru.gr0946x.net.repository.MessageRepository;
import ru.gr0946x.net.repository.UserRepository;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConnectedClient {

    private final Communicator communicator;
    private static final List<ConnectedClient> clients = new ArrayList<>();

    private String username = null;
    private UserEntity userEntity = null;

    private final UserRepository userRepo;
    private final MessageRepository msgRepo;

    public ConnectedClient(Socket socket,
                           UserRepository userRepo,
                           MessageRepository msgRepo) throws IOException {
        this.userRepo = userRepo;
        this.msgRepo  = msgRepo;
        communicator  = new Communicator(socket);
        communicator.addDataListener(this::parseData);
        synchronized (clients) {
            clients.add(this);
        }
    }

    public void start() {
        communicator.start();
        sendData(ProtocolConstants.authFail("Требуется вход. LOGIN:user:pass или REGISTER:user:pass"));
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    private void parseData(String raw) {
        System.out.println("[Server] Получено от " + username + ": " + raw);
        MessageType type = ProtocolConstants.parseType(raw);

        if (username == null) {
            switch (type) {
                case LOGIN    -> handleLogin(raw);
                case REGISTER -> handleRegister(raw);
                default       -> sendData(ProtocolConstants.authFail(
                        "Сначала выполните вход (LOGIN) или регистрацию (REGISTER)"));
            }
        } else {
            switch (type) {
                case MSG              -> handleMsg(raw);
                case BROADCAST        -> handleBroadcast(raw);
                case HISTORY_REQUEST  -> handleHistory(raw);
                case SEARCH_REQUEST   -> handleSearch(raw);
                case USER_LIST_REQUEST -> broadcastUserList();
                case DISCONNECT       -> stop();
                default               -> sendData(ProtocolConstants.authFail("Неизвестная команда"));
            }
        }
    }

    private void handleLogin(String raw) {
        String[] p = ProtocolConstants.parts(raw, 2);
        if (p.length < 2) { sendData(ProtocolConstants.authFail("Формат: LOGIN:имя:пароль")); return; }
        String name = p[0].trim();
        String pass = p[1].trim();

        Optional<UserEntity> opt = userRepo.findByUsernameIgnoreCase(name);
        if (opt.isEmpty() || !BCrypt.checkpw(pass, opt.get().getPassword())) {
            sendData(ProtocolConstants.authFail("Неверный логин или пароль"));
            return;
        }
        if (isOnline(name)) {
            sendData(ProtocolConstants.authFail("Пользователь уже в сети"));
            return;
        }
        userEntity = opt.get();
        username   = userEntity.getUsername();
        finishAuth();
    }

    private void handleRegister(String raw) {
        String[] p = ProtocolConstants.parts(raw, 2);
        if (p.length < 2) { sendData(ProtocolConstants.authFail("Формат: REGISTER:имя:пароль")); return; }
        String name = p[0].trim();
        String pass = p[1].trim();

        if (!ValidationUtil.isValidUsername(name)) {
            sendData(ProtocolConstants.authFail("Имя должно начинаться с буквы, 3–20 символов"));
            return;
        }
        if (!ValidationUtil.isValidPassword(pass)) {
            sendData(ProtocolConstants.authFail("Пароль минимум 4 символа"));
            return;
        }
        if (userRepo.existsByUsernameIgnoreCase(name)) {
            sendData(ProtocolConstants.authFail("Имя уже занято"));
            return;
        }
        String hashed = BCrypt.hashpw(pass, BCrypt.gensalt());
        userEntity = userRepo.save(new UserEntity(name.toLowerCase(), hashed));
        username   = userEntity.getUsername();
        finishAuth();
    }

    private void finishAuth() {
        sendData(ProtocolConstants.authOk(username));
        broadcastUserList();
        broadcastInfo(username + " вошёл в чат");
        System.out.println("[Server] " + username + " авторизован.");
    }

    private void handleMsg(String raw) {
        String[] p = ProtocolConstants.parts(raw, 3);
        if (p.length < 3) return;
        String to   = p[1].trim();
        String text = p[2];

        var recipient = findOnline(to);
        if (recipient == null) {
            sendData(ProtocolConstants.authFail("Пользователь " + to + " не в сети"));
            return;
        }

        userRepo.findByUsernameIgnoreCase(to).ifPresent(toEntity ->
                msgRepo.save(new MessageEntity(userEntity, toEntity, text, LocalDateTime.now()))
        );

        String line = ProtocolConstants.msg(username, to, text);
        recipient.sendData(line);
        sendData(line);
    }

    private void handleBroadcast(String raw) {
        String[] p = ProtocolConstants.parts(raw, 2);
        if (p.length < 2) return;
        String text = p[1];

        msgRepo.save(new MessageEntity(userEntity, null, text, LocalDateTime.now()));
        String line = ProtocolConstants.broadcast(username, text);
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.username != null)
                    .forEach(c -> c.sendData(line));
        }
    }

    private void handleHistory(String raw) {
        String peer = ProtocolConstants.payload(raw).trim();
        userRepo.findByUsernameIgnoreCase(peer).ifPresent(peerEntity -> {
            var history = msgRepo.findHistory(
                    userEntity, peerEntity, ProtocolConstants.HISTORY_LIMIT);
            for (int i = history.size() - 1; i >= 0; i--) {
                var m = history.get(i);
                String receiverName = m.getReceiver() != null ? m.getReceiver().getUsername() : "all";
                sendData(ProtocolConstants.historyItem(
                        m.getSender().getUsername(),
                        receiverName,
                        m.getText(),
                        m.getSentAt().toString()));
            }
            sendData(ProtocolConstants.historyEnd());
        });
    }

    private void handleSearch(String raw) {
        String[] p = ProtocolConstants.parts(raw, 2);
        if (p.length < 2) return;
        String peer    = p[0].trim();
        String keyword = p[1].trim();

        userRepo.findByUsernameIgnoreCase(peer).ifPresent(peerEntity -> {
            var results = msgRepo.searchMessages(userEntity, peerEntity, keyword);
            for (var m : results) {
                String receiverName = m.getReceiver() != null ? m.getReceiver().getUsername() : "all";
                sendData(ProtocolConstants.searchItem(
                        m.getSender().getUsername(),
                        receiverName,
                        m.getText(),
                        m.getSentAt().toString()));
            }
            sendData(ProtocolConstants.searchEnd());
        });
    }

    private void broadcastUserList() {
        String csv;
        synchronized (clients) {
            csv = clients.stream()
                    .filter(c -> c.username != null)
                    .map(c -> c.username)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + ProtocolConstants.LIST_SEP + b);
        }
        String line = ProtocolConstants.userList(csv);
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.username != null)
                    .forEach(c -> c.sendData(line));
        }
    }

    private void broadcastInfo(String text) {
        String line = MessageType.INFO + ProtocolConstants.SEP + text;
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.username != null)
                    .forEach(c -> c.sendData(line));
        }
    }

    private boolean isOnline(String name) {
        return findOnline(name) != null;
    }

    private ConnectedClient findOnline(String name) {
        synchronized (clients) {
            return clients.stream()
                    .filter(c -> c.username != null && c.username.equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }
    }

    public void stop() {
        String departedUser = username;
        if (username != null) {
            System.out.println("[Server] " + username + " отключился.");
            username = null;
        }
        synchronized (clients) {
            clients.remove(this);
        }
        if (departedUser != null) {
            broadcastInfo(departedUser + " покинул чат");
            broadcastUserList();
        }
        communicator.stop();
    }
}