@import org.huebert.ncbot.dto.ChatRequest
@import java.nio.charset.StandardCharsets

@param ChatRequest request
@param String response

REQUEST: ${request.messageText()}
RESPONSE: ${response}
LENGTH: ${response.getBytes(StandardCharsets.UTF_8).length} UTF-8 bytes
