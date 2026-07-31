@import org.huebert.ncbot.dto.ChatRequest
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter

@param ChatRequest request

@[${request.senderName()}]
Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("H:mm:ss"))}
Transit: ${request.getTransitDescription()}
Path: ${String.join(", ", request.getPathItems())}
Region: ${(request.scoped() != null && request.scoped()) ? (request.region() != null ? request.region() : "unknown") : "none"}
