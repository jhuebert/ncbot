@import com.google.common.base.Splitter
@import org.apache.logging.log4j.util.Strings
@import org.huebert.ncbot.dto.ChatRequest
@import org.huebert.ncbot.entity.ChatMessage
@import org.huebert.ncbot.entity.ConversationMemory
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter
@import java.util.List

@param ConversationMemory memory
@param List<ChatMessage> messages
@param ChatRequest request

## Conversation Memory
@if(memory != null && Strings.trimToNull(memory.getContent()) != null)
${memory.getContent()}
@else
No conversation memory
@endif

## Latest Messages
@for(ChatMessage msg : messages)
${msg.getSenderName()}: ${msg.getMessageText()}@endfor
${request.senderName()}: ${request.messageText()}

## Additional Context

Current Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))}
Message Routing Path: ${Strings.trimToNull(request.path()) == null ? "direct route" : String.join(",", Splitter.fixedLength(request.pathBytesPerHop() * 2).splitToList(request.path()))}
