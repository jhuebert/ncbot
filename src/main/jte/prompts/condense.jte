@import org.huebert.ncbot.dto.ChatRequest
@import java.nio.charset.StandardCharsets

@param ChatRequest request
@param String response

## Request

${request.senderKey()}: ${request.messageText()}

## Response (length = ${response.getBytes(StandardCharsets.UTF_8).length} bytes)

${response}
