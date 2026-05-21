package ru.gr0946x.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.gr0946x.net.Client;
import ru.gr0946x.net.ProtocolConstants;

public class JavaFxApp extends Application {

    private JavaFxUi ui;
    private Client client;

    @Override
    public void start(Stage stage) throws Exception {
        ui     = new JavaFxUi();
        client = new Client("localhost", 9460);

        // Связываем UI и клиент
        ui.addUserDataListener(client::sendData);
        client.addDataListener((data, type) -> ui.showInfo(data, type));
        client.start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Scene scene = new Scene(loader.load());

        LoginController ctrl = loader.getController();
        ctrl.setClient(client);

        stage.setTitle("Карета");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> {
            client.sendData(ProtocolConstants.disconnect());
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            client.stop();
            Platform.exit();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}