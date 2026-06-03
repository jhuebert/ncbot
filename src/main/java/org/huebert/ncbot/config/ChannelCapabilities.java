package org.huebert.ncbot.config;

/**
 * Resolved channel capabilities derived from flat configuration lists.
 */
public record ChannelCapabilities(
        boolean welcome,
        boolean pathUpgrade,
        boolean command,
        AiMode ai
) {

    /**
     * Build capabilities for a channel name from flat property lists.
     *
     * <p>Precedence for AI mode:
     * <ol>
     *   <li>{@code ai-disabled} wins over {@code ai-tagged} and {@code ai-each}</li>
     *   <li>{@code ai-each} wins over {@code ai-tagged}</li>
     * </ol>
     *
     * <p>Welcome, command, and path-upgrade are independent boolean flags —
     * presence in the list means {@code true}, absence means {@code false}.
     */
    public static ChannelCapabilities from(String channelName, NcbotProperties props) {
        boolean welcome = props.channelsWelcome().contains(channelName);
        boolean command = props.channelsCommand().contains(channelName);
        boolean pathUpgrade = props.channelsPathUpgrade().contains(channelName);
        AiMode ai = resolveAiMode(channelName, props);
        return new ChannelCapabilities(welcome, command, pathUpgrade, ai);
    }

    private static AiMode resolveAiMode(String channelName, NcbotProperties props) {
        if (props.channelsAiDisabled().contains(channelName)) {
            return AiMode.DISABLED;
        }
        if (props.channelsAiEach().contains(channelName)) {
            return AiMode.EACH;
        }
        if (props.channelsAiTagged().contains(channelName)) {
            return AiMode.TAGGED;
        }
        return AiMode.DISABLED;
    }

}
