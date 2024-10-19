package com.example.chat2;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChatRoomService {

    private Map<String, ChatRoom> chatRooms = new HashMap<>();

    // 채팅방 생성
    public ChatRoom createRoom(String name) {
        ChatRoom room = ChatRoom.create(name);
        chatRooms.put(room.getRoomId(), room);
        return room;
    }

    // 채팅방 조회
    public Collection<ChatRoom> findAllRooms() {
        return chatRooms.values();
    }

    // 채팅방 찾기
    public ChatRoom findRoomById(String roomId) {
        return chatRooms.get(roomId);
    }
}
