@import org.huebert.ncbot.entity.ChatMemory
@import org.huebert.ncbot.entity.ChatMessage
@import java.util.Collection
@import java.util.List

@param Collection<ChatMemory> memories
@param List<ChatMessage> messages

__CHAT_MEMORY__
@for(ChatMemory memory : memories)
${memory.getKey()}=${memory.getValue()}@endfor

__CHAT_MESSAGES__
@for(ChatMessage msg : messages)
${msg.getSenderName()}: ${msg.getContent()}@endfor
