package ru.gr0946x.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import java.util.function.BiConsumer;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;

    private Client client;
    private BiConsumer<String, MessageType> listener;

    public void setClient(Client client) {
        this.client = client;
        listener = this::onServerResponse;
        client.addDataListener(listener);
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        if (user.isBlank() || pass.isBlank()) {
            errorLabel.setText("Заполните все поля");
            return;
        }
        setButtonsDisabled(true);
        client.sendData(ProtocolConstants.login(user, pass));
    }

    @FXML
    private void onRegister() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        if (user.isBlank() || pass.isBlank()) {
            errorLabel.setText("Заполните все поля");
            return;
        }
        setButtonsDisabled(true);
        client.sendData(ProtocolConstants.register(user, pass));
    }

    private void onServerResponse(String payload, MessageType type) {
        Platform.runLater(() -> {
            switch (type) {
                case AUTH_OK -> openChat(payload);
                case AUTH_FAIL -> {
                    errorLabel.setText(payload);
                    setButtonsDisabled(false);
                }
                default -> {}
            }
        });
    }

    private void openChat(String username) {
        try {
            client.removeDataListener(listener); // теперь правильно удаляем

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/chat.fxml"));
            Scene scene = new Scene(loader.load());

            ChatController chatCtrl = loader.getController();
            chatCtrl.init(client, username);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Карета — " + username);
            stage.setScene(scene);
            stage.setResizable(true);
        } catch (Exception e) {
            errorLabel.setText("Ошибка: " + e.getMessage());
            setButtonsDisabled(false);
        }
    }

    private void setButtonsDisabled(boolean disabled) {
        loginBtn.setDisable(disabled);
        registerBtn.setDisable(disabled);
    }
}