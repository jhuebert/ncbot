@import org.huebert.ncbot.entity.ChatMemory
@import java.util.List

@param List<ChatMemory> memories
@param String title
@param String listId
@param Long channelId = null

<div class="memory-view">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="mb-0">${title}</h5>
        @if(channelId != null)
            <button class="btn btn-sm btn-primary"
                    hx-post="/admin/memory"
                    hx-vals='{"channelId": ${channelId}}'
                    hx-swap="afterbegin"
                    hx-target="#${listId}"
                    title="Add memory entry">
                <i class="bi bi-plus-lg"></i> Add
            </button>
        @endif
    </div>

    <div id="${listId}">
        @if(memories.isEmpty())
            <div class="empty-state">No ${title.toLowerCase()} entries.</div>
        @else
            @for(ChatMemory mem : memories)
                @template.admin._memory_item(memory = mem, channelId = channelId)
            @endfor
        @endif
    </div>
</div>
