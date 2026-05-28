@import org.huebert.ncbot.entity.ChatChannel
@import java.util.List

@param List<ChatChannel> publicChannels
@param List<ChatChannel> dmChannels
@param Long activeChannelId = null

<div class="list-group list-group-flush">
    <h6 class="list-group-item-text text-muted small text-uppercase fw-bold px-3 py-2">Public</h6>
    @for(ChatChannel ch : publicChannels)
        @template.admin._channel_item(channel = ch, tab = "messages", active = (ch.getId() == activeChannelId))
    @endfor

    <h6 class="list-group-item-text text-muted small text-uppercase fw-bold px-3 py-2">DMs</h6>
    @for(ChatChannel ch : dmChannels)
        @template.admin._channel_item(channel = ch, tab = "messages", active = (ch.getId() == activeChannelId))
    @endfor

    <h6 class="list-group-item-text text-muted small text-uppercase fw-bold px-3 py-2">Tools</h6>
    <div class="list-group-item list-group-item-action channel-link"
         data-channel-id="global"
         data-tab="memory"
         hx-get="/admin/memory/global"
         hx-target="#tab-memory"
         hx-swap="innerHTML">
        <i class="bi bi-journal-text"></i> Global Memory
    </div>
    <div class="list-group-item list-group-item-action channel-link"
         data-channel-id="participants"
         data-tab="participants"
         hx-get="/admin/participants/null"
         hx-target="#tab-participants"
         hx-swap="innerHTML">
        <i class="bi bi-people"></i> Participants
    </div>
</div>
