@import org.huebert.ncbot.dto.ChatRequest
@import org.huebert.ncbot.entity.ChatMemory
@import org.huebert.ncbot.dto.PromptMessage
@import org.huebert.ncbot.util.PromptMessages
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter
@import java.util.List

@param Long channelId
@param List<ChatMemory> memories
@param List<PromptMessage> messages
@param ChatRequest request

CHANNEL_ID: ${channelId}

__CHAT_MEMORY__
@for(ChatMemory memory : memories)
${memory.getKey()}=${memory.getValue()}@endfor

__CHAT_MESSAGES__
${PromptMessages.toJson(messages)}

__ADDITIONAL_CONTEXT__

Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))}
Path: ${String.join(",", request.getPathItems())}
Region: ${(request.scoped() != null && request.scoped()) ? (request.region() != null ? request.region() : "unknown") : "none"}
