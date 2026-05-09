package org.huebert.ncbot.tool;

import java.util.List;
import java.util.stream.Collectors;
import org.huebert.ncbot.entity.ChatMessage;
import org.huebert.ncbot.repository.ChatMessageRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class MessageHistoryTool {

    private final ChatMessageRepository repository;
    private final ThreadLocal<String> currentChannelKey = new ThreadLocal<>();
    private final ThreadLocal<Boolean> currentIsDm = new ThreadLocal<>();
    private final ThreadLocal<String> currentSenderKey = new ThreadLocal<>();

    public MessageHistoryTool(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public void setContext(String channelKey, Boolean isDm, String senderKey) {
        currentChannelKey.set(channelKey);
        currentIsDm.set(isDm);
        currentSenderKey.set(senderKey);
    }

    public void clearContext() {
        currentChannelKey.remove();
        currentIsDm.remove();
        currentSenderKey.remove();
    }

    @Tool(description = "Search previous messages in the current channel only")
    public String searchMessages(
        @ToolParam(description = "Text to search for in messages") String query,
        @ToolParam(description = "Maximum number of results") Integer limit
    ) {
        String channelKey = currentChannelKey.get();
        Boolean isDm = currentIsDm.get();
        String senderKey = currentSenderKey.get();
        int searchLimit = limit != null ? limit : 10;

        List<ChatMessage> messages;
        if (Boolean.TRUE.equals(isDm) && senderKey != null) {
            messages = repository.findAllByIsDmAndSenderKeyOrderByCreatedAtDesc(
                true, senderKey, PageRequest.of(0, Math.max(searchLimit, 50)));
        } else if (channelKey != null) {
            messages = repository.findAllByChannelKeyOrderByCreatedAtDesc(
                channelKey, PageRequest.of(0, Math.max(searchLimit, 50)));
        } else {
            return "No channel context available.";
        }

        String lowerQuery = query.toLowerCase();
        List<String> results = messages.stream()
            .filter(m -> m.getMessageText() != null && m.getMessageText().toLowerCase().contains(lowerQuery))
            .limit(searchLimit)
            .map(m -> formatMessage(m))
            .collect(Collectors.toList());

        if (results.isEmpty()) {
            return "No messages found matching: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" message(s):\n");
        for (String result : results) {
            sb.append(result).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Return recent message history for context building.
     */
    public String getRecentHistory(int limit) {
        String channelKey = currentChannelKey.get();
        Boolean isDm = currentIsDm.get();
        String senderKey = currentSenderKey.get();

        List<ChatMessage> messages;
        if (Boolean.TRUE.equals(isDm) && senderKey != null) {
            messages = repository.findAllByIsDmAndSenderKeyOrderByCreatedAtDesc(
                true, senderKey, PageRequest.of(0, limit));
        } else if (channelKey != null) {
            messages = repository.findAllByChannelKeyOrderByCreatedAtDesc(
                channelKey, PageRequest.of(0, limit));
        } else {
            return "";
        }

        return messages.stream()
            .map(m -> formatMessage(m))
            .collect(Collectors.joining("\n"));
    }

    private String formatMessage(ChatMessage m) {
        String sender = m.getSenderName() != null ? m.getSenderName() : "(unknown)";
        return String.format("[%s] %s: %s",
            m.getCreatedAt().toString().substring(0, 19),
            sender,
            m.getMessageText());
    }
}
