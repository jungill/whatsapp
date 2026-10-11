package com.junior.chat.web;

import com.junior.chat.model.Envelope;
import com.junior.chat.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MessageController {

    private final MessageService messages;

    public MessageController(MessageService messages) {
        this.messages = messages;
    }

    @GetMapping("/conversations/{peer}/messages")
    public List<Envelope.Message> history(@PathVariable String peer, Authentication auth) {
        return messages.conversation(auth.getName(), peer)
                .stream()
                .map(Envelope.Message::of)
                .toList();
    }
}