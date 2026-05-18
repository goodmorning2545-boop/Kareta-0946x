package ru.gr0946x.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import java.util.Arrays;

public class ChatController {

    @FXML private ListView<String> userList;
    @FXML private ListView<String> messageList;
    @FXML private TextField inputField;
    @FXML private TextField searchField;
    @FXML private Label chatWithLabel;

    private Client client;
    private String myUsername;
    private String currentPeer = null; // null = broadcast

    public void init(Client client, String username) {
        this.client     = client;
        this.myUsername = username;

        client.addDataListener(this::onData);

        client.sendData("USER_LIST_REQUEST:");

        // Клик по пользователю в списке
        userList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null) selectPeer(selected);
                });

        // Enter в поле ввода
        inputField.setOnAction(e -> onSend());
    }

    // ── Выбор собеседника ─────────────────────────────────────────────────

    @FXML
    private void onSelectBroadcast() {
        currentPeer = null;
        chatWithLabel.setText("📢 Все пользователи");
        messageList.getItems().clear();
        userList.getSelectionModel().clearSelection();
    }

    private void selectPeer(String peer) {
        if (peer.equals(myUsername)) return;
        currentPeer = peer;
        chatWithLabel.setText("💬 " + peer);
        messageList.getItems().clear();
        // Запрашиваем историю
        client.sendData(ProtocolConstants.historyRequest(peer));
    }

    // ── Отправка сообщения ────────────────────────────────────────────────

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isBlank()) return;
        inputField.clear();

        if (currentPeer == null) {
            client.sendData(ProtocolConstants.broadcast(myUsername, text));
        } else {
            client.sendData(ProtocolConstants.msg(myUsername, currentPeer, text));
        }
    }

    // ── Поиск ─────────────────────────────────────────────────────────────

    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank() || currentPeer == null) return;
        messageList.getItems().clear();
        messageList.getItems().add("🔍 Результаты поиска «" + keyword + "»:");
        client.sendData(ProtocolConstants.searchRequest(currentPeer, keyword));
    }

    // ── Входящие данные с сервера ─────────────────────────────────────────

    private void onData(String payload, MessageType type) {
        Platform.runLater(() -> {
            switch (type) {

                case USER_LIST -> {
                    String[] users = payload.split(ProtocolConstants.LIST_SEP);
                    userList.getItems().setAll(
                            Arrays.stream(users)
                                    .map(String::trim)
                                    .filter(u -> !u.isBlank() && !u.equalsIgnoreCase(myUsername))
                                    .toList()
                    );
                }

                case MSG -> {
                    // payload: от:кому:текст
                    String[] p = payload.split(ProtocolConstants.SEP, 3);
                    if (p.length < 3) return;
                    String from = p[0].trim();
                    String to   = p[1].trim();
                    String text = p[2];
                    if (currentPeer != null &&
                            (from.equalsIgnoreCase(currentPeer) ||
                                    to.equalsIgnoreCase(currentPeer) ||
                                    from.equalsIgnoreCase(myUsername))) {
                        addMessage(from, text);
                    }
                }

                case BROADCAST -> {
                    // payload: от:текст
                    String[] p = payload.split(ProtocolConstants.SEP, 2);
                    if (p.length < 2) return;
                    if (currentPeer == null) {
                        addMessage(p[0].trim(), p[1]);
                    }
                }

                case HISTORY_ITEM, SEARCH_ITEM -> {
                    String[] p = payload.split(ProtocolConstants.SEP, 4);
                    if (p.length < 4) return;
                    String dt   = p[3].length() >= 16 ? p[3].substring(0, 16).replace("T", " ") : p[3];
                    String line = "[" + dt + "] " + p[0].trim() + ": " + p[2];
                    messageList.getItems().add(line);
                    scrollToBottom();
                }

                case HISTORY_END -> {}

                case SEARCH_END -> {
                    if (messageList.getItems().stream()
                            .noneMatch(s -> !s.startsWith("🔍"))) {
                        messageList.getItems().add("Ничего не найдено.");
                    }
                }

                case INFO -> addSystemMessage("ℹ️ " + payload);

                case AUTH_FAIL, ERROR -> addSystemMessage("⚠️ " + payload);
            }
        });
    }

    private void addMessage(String from, String text) {
        String prefix = from.equalsIgnoreCase(myUsername) ? "Вы" : from;
        messageList.getItems().add(prefix + ": " + text);
        scrollToBottom();
    }

    private void addSystemMessage(String text) {
        messageList.getItems().add(text);
        scrollToBottom();
    }

    private void scrollToBottom() {
        int size = messageList.getItems().size();
        if (size > 0) messageList.scrollTo(size - 1);
    }

}