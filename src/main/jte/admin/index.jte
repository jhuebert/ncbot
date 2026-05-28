@import org.huebert.ncbot.entity.ChatChannel
@import org.huebert.ncbot.entity.ChatMessage
@import org.huebert.ncbot.config.NcbotProperties
@import java.util.List
@import java.time.Duration
@import java.time.Instant

@param List<ChatChannel> publicChannels
@param List<ChatChannel> dmChannels
@param String messagesHtml
@param Long channelId
@param String channelName

@template.admin._layout(
    title = "Admin",
    publicChannels = publicChannels,
    dmChannels = dmChannels,
    channelId = channelId,
    channelName = channelName
)

<script>
    document.getElementById('tab-messages').innerHTML = @{messagesHtml};
    setActiveChannel(@{channelId != null ? channelId.toString() : "null"});
    const msgContainer = document.querySelector('.message-container');
    if (msgContainer) {
        requestAnimationFrame(() => { msgContainer.scrollTop = msgContainer.scrollHeight; });
    }
</script>
