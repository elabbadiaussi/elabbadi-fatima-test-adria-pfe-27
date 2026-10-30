package ma.adria.bankagent.config;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class AgentConfig {

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant bancaire de la plateforme. Tu reponds en francais, de facon claire et concise.
            Tu disposes d'outils (tools MCP) connectes aux microservices account-service et transaction-service.
            Regles :
            - Utilise TOUJOURS les outils pour lire ou modifier les donnees, n'invente jamais de compte, de solde ou d'identifiant.
            - Si l'utilisateur donne une reference client (ex: CUST-001), recupere d'abord ses comptes pour obtenir l'UUID.
            - Pour un paiement ou un virement, verifie la devise du compte avant d'appeler l'outil.
            - Si un outil renvoie une erreur (plafond, compte bloque, devise, solde insuffisant), explique-la simplement.
            - Presente les montants avec leur devise.
            """;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, List<McpSyncClient> mcpClients, ChatMemory chatMemory) {
        SyncMcpToolCallbackProvider toolProvider = new SyncMcpToolCallbackProvider(mcpClients);
        log.info("Tools MCP disponibles pour l'agent : {}",
                Arrays.stream(toolProvider.getToolCallbacks()).map(t -> t.getToolDefinition().name()).toList());
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(toolProvider)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}