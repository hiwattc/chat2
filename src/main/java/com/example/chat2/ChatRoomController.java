package com.example.chat2;

import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.Collection;

@Controller
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    // 채팅방 목록 조회
    @GetMapping("/rooms")
    public String getRooms(Model model) {
        Collection<ChatRoom> rooms = chatRoomService.findAllRooms();
        model.addAttribute("rooms", rooms);
        return "rooms";  // 채팅방 목록을 표시할 뷰
    }

    // 채팅방 생성
    @PostMapping("/rooms")
    public String createRoom(@RequestParam String name) {
        ChatRoom room = chatRoomService.createRoom(name);
        return "redirect:/talk/" + room.getRoomId();  // 생성된 채팅방으로 리다이렉트
    }

    // 특정 채팅방에 입장
    @GetMapping("/talk/{roomId}")
    public String joinRoom(@PathVariable String roomId, Model model) {
        ChatRoom room = chatRoomService.findRoomById(roomId);
        model.addAttribute("roomId", roomId);
        model.addAttribute("roomName", room.getRoomName());
        return "talk";  // 해당 채팅방을 위한 채팅 뷰
    }
}
