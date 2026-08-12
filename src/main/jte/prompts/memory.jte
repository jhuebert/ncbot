@import org.huebert.ncbot.entity.ChatMemory
@import org.huebert.ncbot.dto.PromptMessage
@import org.huebert.ncbot.util.PromptMessages
@import java.util.Collection
@import java.util.List

@param Collection<ChatMemory> memories
@param List<PromptMessage> messages

__CHAT_MEMORY__
@for(ChatMemory m : memories)
${m.getKey()}=${m.getValue()}@endfor

__CHAT_MESSAGES__
${PromptMessages.toJson(messages)}
