package com.speechcalendar.service;

import com.speechcalendar.dto.AIResponse;

public interface AIService {

    AIResponse parseIntent(String text);
}
