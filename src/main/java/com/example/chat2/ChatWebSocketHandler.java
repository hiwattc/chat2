package com.example.chat2;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
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

        // 메시지 크기 제한 설정
        session.setTextMessageSizeLimit(64 * 1024); // 64KB
        session.setBinaryMessageSizeLimit(64 * 1024); // 64KB
        super.afterConnectionEstablished(session);

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
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String roomId = getRoomId(session);  // 현재 세션의 방 ID를 가져옴
        List<WebSocketSession> sessions = roomSessions.get(roomId);  // 해당 방에 있는 모든 세션 가져오기

        // 수신한 바이너리 데이터 추출
        ByteBuffer byteBuffer = message.getPayload();
        byte[] fileBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(fileBytes);  // ByteBuffer를 byte 배열로 변환

        // 전송할 바이너리 메시지 생성
        BinaryMessage binaryMessage = new BinaryMessage(fileBytes);

        // 같은 방의 모든 세션에 바이너리 메시지 전송
        for (WebSocketSession webSocketSession : sessions) {
            // 자기 자신에게 전송하지 않도록 조건 추가
            if (webSocketSession.isOpen() && !webSocketSession.getId().equals(session.getId())) {
                try {
                    webSocketSession.sendMessage(new BinaryMessage(fileBytes));  // BinaryMessage 전송
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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


