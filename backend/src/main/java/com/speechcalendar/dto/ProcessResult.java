package com.speechcalendar.dto;

import java.util.List;

public class ProcessResult {

    private boolean success;
    private String message;
    private String intent;
    private AIResponse parsed;
    private List<EventVO> events;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public AIResponse getParsed() { return parsed; }
    public void setParsed(AIResponse parsed) { this.parsed = parsed; }

    public List<EventVO> getEvents() { return events; }
    public void setEvents(List<EventVO> events) { this.events = events; }
}
