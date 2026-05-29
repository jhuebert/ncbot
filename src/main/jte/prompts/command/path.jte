@import org.huebert.ncbot.dto.ChatRequest

@param ChatRequest request

@[${request.senderName()}] ${String.join(",", request.getPathItems())}
