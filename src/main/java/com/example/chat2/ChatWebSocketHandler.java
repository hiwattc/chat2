package com.example.chat2;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 각 채팅방에 대해 세션을 관리
    private Map<String, List<WebSocketSession>> roomSessions = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomId(session);
        roomSessions.computeIfAbsent(roomId, k -> new ArrayList<>()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = getRoomId(session);
        List<WebSocketSession> sessions = roomSessions.get(roomId);

        for (WebSocketSession webSocketSession : sessions) {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = getRoomId(session);
        roomSessions.get(roomId).remove(session);
    }

    // WebSocketSession에서 채팅방 ID 추출
    private String getRoomId(WebSocketSession session) {
        return session.getUri().getPath().split("/chat/")[1];
    }
}


