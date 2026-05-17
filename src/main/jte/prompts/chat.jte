@import com.google.common.base.Splitter
@import org.apache.logging.log4j.util.Strings
@import org.huebert.ncbot.dto.ChatRequest
@import org.huebert.ncbot.entity.ChatMemory
@import org.huebert.ncbot.entity.ChatMessage
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter
@import java.util.List

@param List<ChatMemory> memories
@param List<ChatMessage> messages
@param ChatRequest request

CHAT_MEMORY:
@for(ChatMemory memory : memories)
${memory.getKey()}=${memory.getValue()}@endfor

CHAT_MESSAGES:
@for(ChatMessage msg : messages)
${msg.getSenderName()}: ${msg.getContent()}@endfor
${request.senderName()}: ${request.messageText()}

ADDITIONAL_CONTEXT:

Current Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))}
Message Routing Path: ${Strings.trimToNull(request.path()) == null ? "direct route" : String.join(",", Splitter.fixedLength(request.pathBytesPerHop() * 2).splitToList(request.path()))}
