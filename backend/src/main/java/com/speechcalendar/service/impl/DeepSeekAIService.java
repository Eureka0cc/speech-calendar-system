package com.speechcalendar.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speechcalendar.dto.AIResponse;
import com.speechcalendar.service.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekAIService implements AIService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekAIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AIResponse parseIntent(String text) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String systemPrompt = """
                你是一个日程管理助手。解析用户输入的自然语言，返回 JSON。

                当前时间：%s
                今天日期：%s

                返回格式（必须严格 JSON，不要 markdown 代码块）：
                {
                  "intent": "create_event|query_event|delete_event",
                  "title": "事件标题",
                  "startTime": "2026-05-30 15:00:00",
                  "endTime": "2026-05-30 16:00:00",
                  "date": "2026-05-30"
                }

                规则：
                1. "明天"="%s"，"今天"="%s"，"后天"等需要基于今天正确计算
                2. "上午"默认 08:00，"下午"默认 14:00，"晚上"默认 19:00
                3. 如果没有明确结束时间，endTime 设为 null
                4. query_event 和 delete_event 如果有指定日期用 date 字段，无日期则默认今天
                5. 课程、会议等有明确时长的，如"第一节课8点半到10点"，要解析出 startTime 和 endTime
                6. 只返回 JSON，不要任何其他文字
                """.formatted(now, today, getTomorrow(), today);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", text)
                ),
                "temperature", 0.1,
                "max_tokens", 500
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(apiUrl, request, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            return objectMapper.readValue(content, AIResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 意图解析失败: " + e.getMessage(), e);
        }
    }

    private String getTomorrow() {
        return LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
