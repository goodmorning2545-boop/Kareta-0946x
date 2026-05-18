package ru.gr0946x.net;

import ru.gr0946x.net.db.MessageRepository;
import ru.gr0946x.net.db.UserRepository;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectedClient {

    private final Communicator communicator;
    private static final List<ConnectedClient> clients = new ArrayList<>();

    private String username = null;  // null = ещё не авторизован
    private int userId = -1;

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
        // Просим клиента авторизоваться
        sendData(ProtocolConstants.authFail("Требуется вход. Отправьте LOGIN:user:pass или REGISTER:user:pass"));
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    // ── Разбор входящих строк ────────────────────────────────────────────

    private void parseData(String raw) {
        System.out.println("[Server] Получено от " + username + ": " + raw);
        MessageType type = ProtocolConstants.parseType(raw);


        if (username == null) {
            // До авторизации принимаем только LOGIN и REGISTER
            switch (type) {
                case LOGIN    -> handleLogin(raw);
                case REGISTER -> handleRegister(raw);
                default       -> sendData(ProtocolConstants.authFail(
                        "Сначала выполните вход (LOGIN) или регистрацию (REGISTER)"));
            }
        } else {
            switch (type) {
                case MSG             -> handleMsg(raw);
                case BROADCAST       -> handleBroadcast(raw);
                case HISTORY_REQUEST -> handleHistory(raw);
                case SEARCH_REQUEST  -> handleSearch(raw);
                case DISCONNECT      -> stop();
                case USER_LIST_REQUEST -> broadcastUserList();
                default              -> sendData(ProtocolConstants.authFail("Неизвестная команда"));
            }
        }
    }

    // ── Авторизация ──────────────────────────────────────────────────────

    private void handleLogin(String raw) {
        // LOGIN:username:password
        String[] p = ProtocolConstants.parts(raw, 2); // ["username", "password"]
        if (p.length < 2) { sendData(ProtocolConstants.authFail("Формат: LOGIN:имя:пароль")); return; }
        String name = p[0].trim();
        String pass = p[1].trim();

        var opt = userRepo.findByUsername(name);
        if (opt.isEmpty() || !opt.get().password().equals(pass)) {
            sendData(ProtocolConstants.authFail("Неверный логин или пароль"));
            return;
        }
        if (isOnline(name)) {
            sendData(ProtocolConstants.authFail("Пользователь уже в сети"));
            return;
        }
        userId   = opt.get().id();
        username = opt.get().username();
        finishAuth();
    }

    private void handleRegister(String raw) {
        // REGISTER:username:password
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
        if (!userRepo.register(name, pass)) {
            sendData(ProtocolConstants.authFail("Имя уже занято"));
            return;
        }
        userId   = userRepo.findByUsername(name).get().id();
        username = name.toLowerCase();
        finishAuth();
    }

    private void finishAuth() {
        sendData(ProtocolConstants.authOk(username));
        broadcastUserList();
        broadcastInfo(username + " вошёл в чат");
        System.out.println("[Server] " + username + " авторизован.");
    }

    // ── Сообщения ────────────────────────────────────────────────────────

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

        var peerId = userRepo.findIdByUsername(to);
        peerId.ifPresent(rid -> msgRepo.save(userId, rid, text));

        String line = ProtocolConstants.msg(username, to, text);
        recipient.sendData(line);
        sendData(line);
    }

    private void handleBroadcast(String raw) {
        // BROADCAST:от:текст
        String[] p = ProtocolConstants.parts(raw, 2); // ["от", "текст"]
        if (p.length < 2) return;
        String text = p[1];

        msgRepo.save(userId, null, text);
        String line = ProtocolConstants.broadcast(username, text);
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.username != null)
                    .forEach(c -> c.sendData(line));
        }
    }

    // ── История ──────────────────────────────────────────────────────────

    private void handleHistory(String raw) {
        // HISTORY_REQUEST:собеседник
        String peer = ProtocolConstants.payload(raw).trim();
        var peerId  = userRepo.findIdByUsername(peer);
        if (peerId.isEmpty()) return;

        var history = msgRepo.getHistory(userId, peerId.get(), ProtocolConstants.HISTORY_LIMIT);
        // История в БД хранится от новых к старым — разворачиваем
        for (int i = history.size() - 1; i >= 0; i--) {
            var m = history.get(i);
            sendData(ProtocolConstants.historyItem(m.senderName(), m.receiverName(), m.text(), m.sentAt()));
        }
        sendData(ProtocolConstants.historyEnd());
    }

    // ── Поиск ────────────────────────────────────────────────────────────

    private void handleSearch(String raw) {
        // SEARCH_REQUEST:собеседник:ключевое_слово
        String[] p = ProtocolConstants.parts(raw, 2);
        if (p.length < 2) return;
        String peer    = p[0].trim();
        String keyword = p[1].trim();

        var peerId = userRepo.findIdByUsername(peer);
        if (peerId.isEmpty()) return;

        var results = msgRepo.search(userId, peerId.get(), keyword);
        for (var m : results) {
            sendData(ProtocolConstants.searchItem(m.senderName(), m.receiverName(), m.text(), m.sentAt()));
        }
        sendData(ProtocolConstants.searchEnd());
    }

    // ── Утилиты ──────────────────────────────────────────────────────────

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