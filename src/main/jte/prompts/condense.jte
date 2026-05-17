@import org.huebert.ncbot.dto.ChatRequest
@import java.nio.charset.StandardCharsets

@param ChatRequest request
@param String response

REQUEST:
${request.senderKey()}: ${request.messageText()}
RESPONSE: ${response}
LENGTH: ${response.getBytes(StandardCharsets.UTF_8).length} bytes
