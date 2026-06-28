package org.huebert.ncbot.service;

import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.huebert.ncbot.util.DebugLog;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateEngine templateEngine;

    @DebugLog
    public String render(String template, Map<String, Object> model) {
        TemplateOutput output = new StringOutput();
        templateEngine.render("prompts/" + template + ".jte", model, output);
        return output.toString().trim();
    }
}
