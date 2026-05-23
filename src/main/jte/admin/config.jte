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
                        <td>${properties.minimumResponseMs()} ms</td>
                    </tr>
                    <tr>
                        <td>Max Reply Bytes</td>
                        <td>${properties.maxReplyBytes()}</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="config-section">
            <h2>Channel Filtering</h2>
            <table class="data-table">
                <tbody>
                    <tr>
                        <td>Allowed Channels</td>
                        <td>

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
