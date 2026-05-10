@import org.huebert.ncbot.config.NcbotProperties
@import java.util.Map

@param long totalMessages
@param long channelMessages
@param long dmMessages
@param Map<String, Long> channelStats
@param String uptimeStr
@param NcbotProperties properties

@template.admin._layout(
    title = "Dashboard",
    content = @`
        <h1>Dashboard</h1>

        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-value">${totalMessages}</div>
                <div class="stat-label">Total Messages</div>
            </div>
            <div class="stat-card">
                <div class="stat-value">${channelMessages}</div>
                <div class="stat-label">Channel Messages</div>
            </div>
            <div class="stat-card">
                <div class="stat-value">${dmMessages}</div>
                <div class="stat-label">DM Messages</div>
            </div>
            <div class="stat-card">
                <div class="stat-value">${uptimeStr}</div>
                <div class="stat-label">Uptime</div>
            </div>
        </div>

        <h2>Channel Stats</h2>
        @if(channelStats.isEmpty())
            <p class="empty-state">No channels yet.</p>
        @else
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Channel Key</th>
                        <th>Messages</th>
                    </tr>
                </thead>
                <tbody>
                @for(var entry : channelStats.entrySet())
                    <tr>
                        <td class="mono">${entry.getKey()}</td>
                        <td>${entry.getValue()}</td>
                    </tr>
                @endfor
                </tbody>
            </table>
        @endif

        <h2>Configuration</h2>
        <table class="data-table">
            <tbody>
                <tr>
                    <td>Response Delay</td>
                    <td>${properties.minimumResponseMs()} ms</td>
                </tr>
                <tr>
                    <td>Max Reply Bytes</td>
                    <td>${properties.maxReplyBytes()}</td>
                </tr>
                <tr>
                    <td>History Limit</td>
                    <td>${properties.messageHistoryMinutes()} minutes</td>
                </tr>
                <tr>
                    <td>Allow DMs</td>
                    <td>${properties.allowDms()}</td>
                </tr>
                <tr>
                    <td>Allowed Channels</td>
                    <td>${properties.allowedChannels() == null || properties.allowedChannels().isEmpty() ? "All channels" : properties.allowedChannels().size() + " channels"}</td>
                </tr>
            </tbody>
        </table>
    `
)
