package org.huebert.ncbot.service;

import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateEngine templateEngine;

    public String render(String template, Map<String, Object> model) {
        log.debug("render: template={}, model={}", template, model);
        TemplateOutput output = new StringOutput();
        templateEngine.render("prompts/" + template + ".jte", model, output);
        log.debug("render result: {}", output);
        return output.toString().trim();
    }
}
