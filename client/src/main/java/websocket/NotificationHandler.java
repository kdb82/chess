package websocket;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import webSocketMessages.Notification;

public interface NotificationHandler {

    void notify(Notification notification);
}