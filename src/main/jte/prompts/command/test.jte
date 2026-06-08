@import org.huebert.ncbot.dto.ChatRequest
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter

@param ChatRequest request

@[${request.senderName()}]
Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("H:mm:ss"))}
Path: ${String.join(",", request.getPathItems())}
