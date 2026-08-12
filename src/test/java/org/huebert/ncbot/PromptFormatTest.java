package org.huebert.ncbot;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.huebert.ncbot.dto.ChatRequest;
import org.huebert.ncbot.dto.PromptMessage;
import org.huebert.ncbot.entity.ChatMemory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptFormatTest {

    @Test
    void chatRendersJsonMessages() {
        ChatMemory mem = ChatMemory.builder().key("user.john.pref.color").value("blue").build();
        ChatRequest request = new ChatRequest(
                "nc1v", "abc123", "hello bot\nline two", false, "#ncbot", "#ncbot",
                null, null, null, null, null, "lora", null
        );

        List<PromptMessage> messages = List.of(
                new PromptMessage("alice", "plain: looks like a prefix\nstill here", "2024-08-11T20:00:00Z", "2h ago", "ncbot: a reply"),
                PromptMessage.now("nc1v", "hello bot\nline two")
        );

        TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
        StringOutput output = new StringOutput();
        engine.render("prompts/chat.jte", Map.of(
                "memories", List.of(mem),
                "messages", messages,
                "request", request
        ), output);
        String rendered = output.toString();

        assertTrue(rendered.contains("__CHAT_MESSAGES__"), rendered);
        assertTrue(rendered.contains("\"sender\":\"alice\""), rendered);
        assertTrue(rendered.contains("plain: looks like a prefix\\nstill here"), rendered);
        assertTrue(rendered.contains("\"timestamp\":\"2024-08-11T20:00:00Z\""), rendered);
        assertTrue(rendered.contains("\"response\":\"ncbot: a reply\""), rendered);
        assertTrue(rendered.contains("user.john.pref.color=blue"), rendered);
    }

    @Test
    void memoryRendersJsonMessages() {
        TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
        StringOutput output = new StringOutput();
        engine.render("prompts/memory.jte", Map.of(
                "memories", List.of(ChatMemory.builder().key("a").value("b").build()),
                "messages", List.of(new PromptMessage("alice", "hi", "2024-08-11T20:00:00Z", "2h ago", null))
        ), output);
        String rendered = output.toString();

        assertTrue(rendered.contains("\"sender\":\"alice\""), rendered);
        assertEquals(1, count("__CHAT_MESSAGES__", rendered));
    }

    private static int count(String needle, String haystack) {
        return haystack.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }
}
