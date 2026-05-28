@import org.huebert.ncbot.entity.ChatMemory

@param ChatMemory memory
@param Long channelId = null

<div class="memory-card" id="memory-${memory.getId()}">
    <!-- View row -->
    <div class="memory-view-row">
        <span class="memory-key" onclick="startEdit(${memory.getId()})" title="Click to edit">${memory.getKey()}</span>
        <span class="memory-value">${memory.getValue()}</span>
        <div class="memory-actions">
            <button class="btn btn-sm btn-outline-danger"
                    hx-delete="/admin/memory/${memory.getId()}"
                    hx-swap="outerHTML"
                    hx-confirm="Delete this memory entry?"
                    title="Delete">
                <i class="bi bi-trash"></i>
            </button>
            @if(channelId != null)
                <button class="btn btn-sm btn-outline-primary"
                        hx-post="/admin/memory/promote"
                        hx-vals='{"memoryId": ${memory.getId()}}'
                        hx-swap="afterbegin"
                        hx-target="#global-memory-list"
                        title="Promote to global">
                    <i class="bi bi-arrow-up-circle"></i>
                </button>
            @endif
        </div>
    </div>

    <!-- Edit form -->
    <div class="memory-edit" id="edit-${memory.getId()}" style="display:none;">
        <input type="text" class="form-control form-control-sm key-input"
               id="key-${memory.getId()}" value="${memory.getKey()}">
        <textarea class="form-control form-control-sm value-input"
                  id="value-${memory.getId()}" rows="2">${memory.getValue()}</textarea>
        <div class="memory-edit-actions mt-2">
            <button class="btn btn-sm btn-primary"
                    hx-put="/admin/memory/${memory.getId()}"
                    hx-vals='js:{
                        "key": document.getElementById("key-${memory.getId()}").value,
                        "value": document.getElementById("value-${memory.getId()}").value
                    }'
                    hx-swap="outerHTML"
                    hx-target="#memory-${memory.getId()}"
                    title="Save">
                <i class="bi bi-check"></i> Save
            </button>
            <button class="btn btn-sm btn-secondary"
                    onclick="cancelEdit(${memory.getId()})"
                    title="Cancel">
                <i class="bi bi-x"></i>
            </button>
        </div>
    </div>
</div>
