@import org.huebert.ncbot.entity.ChatParticipant
@import java.util.List

@param List<ChatParticipant> participants
@param String title

<div class="participants-view">
    <h5 class="mb-3">${title}</h5>
    @if(participants.isEmpty())
        <div class="empty-state">No participants yet.</div>
    @else
        <div class="list-group list-group-flush">
            @for(ChatParticipant p : participants)
                <div class="list-group-item d-flex justify-content-between align-items-center px-0">
                    <span class="participant-name">${p.getName()}</span>
                    <span class="participant-last-seen text-muted small">${p.getLastSeen().toString()}</span>
                </div>
            @endfor
        </div>
    @endif
</div>
