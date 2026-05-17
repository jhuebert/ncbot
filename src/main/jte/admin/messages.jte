@import java.util.List
@import org.huebert.ncbot.entity.ChatMessage

@param List<ChatMessage> messages
@param int currentPage
@param long totalPages
@param int size

@template.admin._layout(
    title = "Messages",
    content = @`
        <h1>Messages</h1>

        <div class="pagination">
            @if(currentPage > 0)
                <a href="/admin/messages?page=${currentPage - 1}&amp;size=${size}" class="btn btn-sm">&laquo; Previous</a>
            @endif
            <span class="page-info">Page ${currentPage + 1} of ${totalPages}</span>
            @if(currentPage + 1 < totalPages)
                <a href="/admin/messages?page=${currentPage + 1}&amp;size=${size}" class="btn btn-sm">Next &raquo;</a>
            @endif
        </div>

        @if(messages.isEmpty())
            <p class="empty-state">No messages found.</p>
        @else
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Time</th>
                        <th>Sender</th>
                        <th>Type</th>
                        <th>Channel / DM</th>
                        <th>Message</th>
                    </tr>
                </thead>
                <tbody>
                @for(ChatMessage msg : messages)
                    <tr>
                        <td class="mono">${msg.getCreatedAt().toString()}</td>
                        <td>${msg.getSenderName() != null ? msg.getSenderName() : "(none)"}</td>

                    </tr>
                @endfor
                </tbody>
            </table>
        @endif
    `
)
