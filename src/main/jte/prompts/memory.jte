@import org.huebert.ncbot.entity.ChatMemory
@import org.huebert.ncbot.entity.ChatMessage
@import java.util.Collection
@import java.util.List

@param Collection<ChatMemory> memories
@param List<ChatMessage> messages

__CHAT_MEMORY__
@for(ChatMemory m : memories)
${m.getKey()}=${m.getValue()}@endfor

__CHAT_MESSAGES__
@for(ChatMessage m : messages)
${m.getSenderName()}: ${m.getContent()}
@if(m.getResponse() != null)ncbot: ${m.getResponse()}@endif@endfor
