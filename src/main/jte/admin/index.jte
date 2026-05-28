@import org.huebert.ncbot.entity.ChatChannel
@import org.huebert.ncbot.entity.ChatMessage
@import java.util.List

@param List<ChatChannel> publicChannels
@param List<ChatChannel> dmChannels
@param Long channelId
@param String channelName
@param List<ChatMessage> messages
@param String formatRelative

@template.admin._layout(
    title = "Admin",
    publicChannels = publicChannels,
    dmChannels = dmChannels,
    channelId = channelId,
    channelName = channelName
)

<script>
    document.getElementById('tab-messages').innerHTML = "";
    setActiveChannel(${channelId != null ? channelId.toString() : "null"});
    const msgContainer = document.querySelector('.message-container');
    if (msgContainer) {
        requestAnimationFrame(() => { msgContainer.scrollTop = msgContainer.scrollHeight; });
    }
</script>
