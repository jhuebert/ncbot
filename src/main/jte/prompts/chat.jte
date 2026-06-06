@import org.huebert.ncbot.dto.ChatRequest
@import org.huebert.ncbot.entity.ChatMemory
@import org.huebert.ncbot.entity.ChatMessage
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter
@import java.util.List

@param List<ChatMemory> memories
@param List<ChatMessage> messages
@param ChatRequest request

__CHAT_MEMORY__
@for(ChatMemory memory : memories)
${memory.getKey()}=${memory.getValue()}@endfor

__CHAT_MESSAGES__
@for(ChatMessage msg : messages)
${msg.getSenderName()}: ${msg.getContent()}@endfor
${request.senderName()}: ${request.messageText()}

__ADDITIONAL_CONTEXT__

Current Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))}
Message Routing Path: ${String.join(",", request.getPathItems())}
