package com.example.chat2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 각 채팅방에 대해 세션을 관리
    private Map<String, List<WebSocketSession>> roomSessions = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Value("${file.recv.allow.ip}")
    private String allowedIps;

    // 유틸리티 메서드: IP 주소를 확인하는 함수
    public boolean isIpAllowed(String ip) throws UnknownHostException {
        String[] allowedIpList = allowedIps.split(",");
        for (String allowedIp : allowedIpList) {
            allowedIp = allowedIp.trim();

            if (allowedIp.contains("/")) { // CIDR 처리
                if (isInRange(ip, allowedIp)) {
                    return true;
                }
            } else if (allowedIp.contains("*")) { // 와일드카드 처리
                if (isWildcardMatch(ip, allowedIp)) {
                    return true;
                }
            } else { // 단일 IP 처리
                if (allowedIp.equals(ip)) {
                    return true;
                }
            }
        }
        return false;
    }

    // CIDR 범위를 확인하는 메서드
    private boolean isInRange(String ip, String cidr) throws UnknownHostException {
        String[] parts = cidr.split("/");
        InetAddress inetIp = InetAddress.getByName(ip);
        InetAddress inetNetwork = InetAddress.getByName(parts[0]);
        int prefixLength = Integer.parseInt(parts[1]);

        byte[] ipBytes = inetIp.getAddress();
        byte[] networkBytes = inetNetwork.getAddress();

        int mask = (int) Math.pow(2, 32 - prefixLength) - 1;

        for (int i = 0; i < ipBytes.length; i++) {
            int ipSegment = ipBytes[i] & 0xFF;
            int networkSegment = networkBytes[i] & 0xFF;

            if ((ipSegment & mask) != (networkSegment & mask)) {
                return false;
            }
        }
        return true;
    }

    // 와일드카드 IP 매칭을 확인하는 메서드
    private boolean isWildcardMatch(String ip, String pattern) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return Pattern.matches(regex, ip);
    }


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
        try {
            String roomId = getRoomId(session);  // 현재 세션의 방 ID를 가져옴
            List<WebSocketSession> sessions = roomSessions.get(roomId);  // 해당 방에 있는 모든 세션 가져오기


            // WebSocketSession에서 IP 주소 추출
            InetSocketAddress remoteAddress = session.getRemoteAddress();
            if (remoteAddress == null) {
                logger.info("Could not get remote address");
                return;
            }

            String ipAddress = remoteAddress.getAddress().getHostAddress();
            // IPv6의 로컬호스트 주소를 IPv4로 변환
            if (ipAddress.equals("0:0:0:0:0:0:0:1")) {
                ipAddress = "127.0.0.1"; // IPv4 로컬호스트로 변환
            }
            // 허용된 IP인지 확인
            if (!isIpAllowed(ipAddress)) {
                logger.info("Unauthorized IP: " + ipAddress);
                return; // 파일 전송 차단
            }

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
        } catch (UnknownHostException e) {
            e.printStackTrace();
            logger.info(e.getMessage());

        } catch (Exception e){
            logger.info(e.getMessage());
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


