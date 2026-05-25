@import com.google.common.base.Splitter
@import org.apache.logging.log4j.util.Strings
@import org.huebert.ncbot.dto.ChatRequest

@param ChatRequest request

@[${request.senderName()}] ${Strings.trimToNull(request.path()) == null ? "direct" : String.join(",", Splitter.fixedLength(request.pathBytesPerHop() * 2).splitToList(request.path()))}
