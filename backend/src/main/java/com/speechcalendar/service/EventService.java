package com.speechcalendar.service;

import com.speechcalendar.dto.AIResponse;
import com.speechcalendar.dto.ProcessResult;
import com.speechcalendar.entity.Event;

import java.time.LocalDate;
import java.util.List;

public interface EventService {

    ProcessResult process(String text, Long userId);

    List<Event> getEventsByDate(LocalDate date);

    Event getEventById(Long id);
}
