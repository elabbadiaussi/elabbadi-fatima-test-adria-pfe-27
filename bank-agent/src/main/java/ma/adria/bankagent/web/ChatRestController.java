package ma.adria.bankagent.web;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatClient chatClient;

    public record ChatRequest(String message, String conversationId) {}

    @GetMapping
    public Map<String, String> chatGet(@RequestParam String message,
                                       @RequestParam(defaultValue = "default") String conversationId) {
        return Map.of("conversationId", conversationId, "answer", ask(message, conversationId));
    }

    @PostMapping
    public Map<String, String> chatPost(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() == null ? "default" : request.conversationId();
        return Map.of("conversationId", conversationId, "answer", ask(request.message(), conversationId));
    }

    private String ask(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}