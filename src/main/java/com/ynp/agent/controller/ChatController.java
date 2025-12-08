package com.ynp.agent.controller;


import com.ynp.agent.model.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate template;

    // 收到客户端发到 /app/echo 的消息，就广播到 /topic/echo
    @MessageMapping("/echo")
    public void echo(@Payload ChatMessage msg) {
        msg.setTs(System.currentTimeMillis());
        template.convertAndSend("/topic/echo", msg);
    }
}