package com.speechcalendar.dto;

public class ProcessRequest {

    private String text;
    private Long userId;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
