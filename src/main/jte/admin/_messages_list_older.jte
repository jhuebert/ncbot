@import org.huebert.ncbot.entity.ChatMessage
@import java.util.List

@param List<ChatMessage> messages
@param String formatRelative

@for(ChatMessage msg : messages)
    <div class="message-row ${msg.getSenderName().equals("ncbot") ? "bot" : "user"}">
        <div class="message-bubble">
            <div class="message-meta">
                <strong class="sender">${msg.getSenderName()}</strong>
                <span class="timestamp" data-timestamp="${msg.getCreatedAt().toString()}">${formatRelative}</span>
            </div>
            <div class="message-text">${msg.getContent()}</div>
        </div>
    </div>
@endfor
