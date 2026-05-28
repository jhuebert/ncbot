@import org.huebert.ncbot.entity.ChatMessage

@param ChatMessage message
@param String formatRelative

<div class="message-row ${message.getSenderName().equals("ncbot") ? "bot" : "user"}">
    <div class="message-bubble">
        <div class="message-meta">
            <strong class="sender">${message.getSenderName()}</strong>
            <span class="timestamp" data-timestamp="${message.getCreatedAt().toString()}">${formatRelative}</span>
        </div>
        <div class="message-text">${message.getContent()}</div>
    </div>
</div>
