package com.example.chat2;

import java.util.UUID;

public class ChatRoom {
    private String roomId;
    private String roomName;

    // 고유한 채팅방 ID 생성
    public static ChatRoom create(String name) {
        ChatRoom room = new ChatRoom();
        room.roomId = UUID.randomUUID().toString();  // 랜덤한 고유 ID 생성
        room.roomName = name;
        return room;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }
}
