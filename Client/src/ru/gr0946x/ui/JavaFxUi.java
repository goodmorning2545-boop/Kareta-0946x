package ru.gr0946x.ui;

import ru.gr0946x.net.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class JavaFxUi implements Ui {

    private final List<Consumer<String>> listeners = new ArrayList<>();

    @Override
    public void showInfo(String data, MessageType type) {
        // JavaFX обновление UI происходит через ChatController
        // этот метод вызывается если кто-то использует JavaFxUi напрямую
        System.out.println("[UI] " + type + ": " + data);
    }

    @Override
    public void addUserDataListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeUserDataListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(String data) {
        for (var listener : listeners) {
            listener.accept(data);
        }
    }
}