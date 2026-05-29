@import org.huebert.ncbot.dto.ChatRequest
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter

@param ChatRequest request
@param String welcomeContent

Welcome @[${request.senderName()}]! ${welcomeContent}
Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}
Path: ${String.join(",", request.getPathItems())}
