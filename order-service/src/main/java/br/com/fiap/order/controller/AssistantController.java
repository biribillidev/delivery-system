package br.com.fiap.order.controller;

import br.com.fiap.order.dto.AssistantRequest;
import br.com.fiap.order.dto.AssistantResponse;
import br.com.fiap.order.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssistantController {

    private final ChatService chatService;

    public AssistantController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/assistant")
    public AssistantResponse ask(@Valid @RequestBody AssistantRequest request) {
        return new AssistantResponse(chatService.ask(request.question()));
    }
}
