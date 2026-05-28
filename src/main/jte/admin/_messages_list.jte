@import org.huebert.ncbot.entity.ChatMessage
@import java.util.List

@param List<ChatMessage> messages
@param Long channelId
@param String formatRelative

<div class="message-container" data-channel-id="${channelId}">
    @if(messages.isEmpty())
        <div class="empty-state">No messages yet.</div>
    @else
        @for(ChatMessage msg : messages)
            @template.admin._message_item(message = msg, formatRelative = formatRelative)
        @endfor
    @endif
</div>
