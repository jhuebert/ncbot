@import org.huebert.ncbot.dto.ChatRequest
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter

@param ChatRequest request

@[${request.senderName()}]
Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}
Path: ${String.join(",", request.getPathItems())}
