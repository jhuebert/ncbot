@import org.huebert.ncbot.entity.ChatChannel

@param ChatChannel channel
@param String tab = "messages"
@param boolean active = false

<div class="list-group-item list-group-item-action channel-link ${active ? "active" : ""}"
     data-channel-id="${channel.getId()}"
     data-tab="${tab}"
     hx-get="/admin/messages/${channel.getId()}"
     hx-target="#tab-messages"
     hx-swap="innerHTML">
    <i class="bi bi-chat-left-text"></i> ${channel.getChannelName()}
</div>
