package ru.gr0946x.net;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Client {
    private final Communicator communicator;
    private final List<BiConsumer<String, MessageType>> listeners = new ArrayList<>();

    public Client(String host, int port) throws IOException {
        var socket = new Socket(host, port);
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
    }

    public void addDataListener(BiConsumer<String, MessageType> listener) {
        listeners.add(listener);
    }

    public void removeDataListener(BiConsumer<String, MessageType> listener) {
        listeners.remove(listener);
    }

    public void start() {
        communicator.start();
    }

    private void parseData(String raw) {
        MessageType type = ProtocolConstants.parseType(raw);
        String payload   = ProtocolConstants.payload(raw);
        for (var listener : listeners) {
            listener.accept(payload, type);
        }
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    public void stop() {
        communicator.stop();
    }
}