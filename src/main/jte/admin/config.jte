@import org.huebert.ncbot.config.NcbotProperties

@param NcbotProperties properties

@template.admin._layout(
    title = "Config",
    content = @`
        <h1>Configuration</h1>

        <div class="config-section">
            <h2>AI Settings</h2>
            <table class="data-table">
                <tbody>
                    <tr>
                        <td>Response Delay</td>
                        <td>${properties.responseDelaySeconds()} seconds</td>
                    </tr>
                    <tr>
                        <td>Max Reply Bytes</td>
                        <td>${properties.maxReplyBytes()}</td>
                    </tr>
                    <tr>
                        <td>History Limit</td>
                        <td>${properties.historyLimit()}</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="config-section">
            <h2>Channel Filtering</h2>
            <table class="data-table">
                <tbody>
                    <tr>
                        <td>Allow DMs</td>
                        <td>
                            <span class="badge ${properties.allowDms() ? "badge-yes" : "badge-no"}">
                                ${properties.allowDms() ? "Yes" : "No"}
                            </span>
                        </td>
                    </tr>
                    <tr>
                        <td>Allowed Channels</td>
                        <td>
                            @if(properties.allowedChannels() == null || properties.allowedChannels().isEmpty())
                                <span class="badge badge-info">All channels</span>
                            @else
                                <div class="channel-list">
                                @for(String ch : properties.allowedChannels())
                                    <span class="badge badge-channel mono">${ch}</span>
                                @endfor
                                </div>
                            @endif
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="config-section">
            <h2>System Prompt</h2>
            <pre class="system-prompt">${properties.systemPrompt()}</pre>
        </div>

        <p class="note">
            Configuration is loaded from <code>application.yaml</code> and environment variables.
            Changes require a restart. See the README for full configuration options.
        </p>
    `
)
