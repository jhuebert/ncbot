@import com.google.common.base.Splitter
@import org.apache.logging.log4j.util.Strings
@import org.huebert.ncbot.dto.ChatRequest
@import java.time.ZonedDateTime
@import java.time.format.DateTimeFormatter

@param ChatRequest request

User: @[${request.senderName()}]
Time: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}
Path: ${Strings.trimToNull(request.path()) == null ? "direct" : String.join(",", Splitter.fixedLength(request.pathBytesPerHop() * 2).splitToList(request.path()))}
