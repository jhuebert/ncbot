@import java.util.List

@param List<String> senders
@param String title

<div class="participants-view">
    <h5 class="mb-3">${title}</h5>
    @if(senders.isEmpty())
        <div class="empty-state">No participants yet.</div>
    @else
        <div class="list-group list-group-flush">
            @for(String name : senders)
                <div class="list-group-item d-flex justify-content-between align-items-center px-0">
                    <span class="participant-name">${name}</span>
                    <span class="participant-last-seen text-muted small">—</span>
                </div>
            @endfor
        </div>
    @endif
</div>
