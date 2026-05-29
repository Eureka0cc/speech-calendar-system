package com.speechcalendar.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.speechcalendar.dto.AIResponse;
import com.speechcalendar.dto.EventVO;
import com.speechcalendar.dto.ProcessResult;
import com.speechcalendar.entity.Event;
import com.speechcalendar.mapper.EventMapper;
import com.speechcalendar.service.AIService;
import com.speechcalendar.service.EventService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static com.speechcalendar.entity.table.EventTableDef.EVENT;

@Service
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final AIService aiService;

    public EventServiceImpl(EventMapper eventMapper, AIService aiService) {
        this.eventMapper = eventMapper;
        this.aiService = aiService;
    }

    @Override
    public ProcessResult process(String text, Long userId) {
        AIResponse parsed = aiService.parseIntent(text);

        ProcessResult result = new ProcessResult();
        result.setIntent(parsed.getIntent());
        result.setParsed(parsed);

        switch (parsed.getIntent()) {
            case "create_event" -> handleCreate(parsed, userId, result);
            case "query_event" -> handleQuery(parsed, result);
            case "delete_event" -> handleDelete(parsed, result);
            default -> {
                result.setSuccess(false);
                result.setMessage("无法识别的意图: " + parsed.getIntent());
            }
        }

        return result;
    }

    private void handleCreate(AIResponse parsed, Long userId, ProcessResult result) {
        if (parsed.getStartTime() == null || parsed.getStartTime().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("没有识别到时间信息，请说明具体的日期和时间");
            return;
        }

        Event event = new Event();
        event.setUserId(userId != null ? userId : 1L);
        event.setTitle(parsed.getTitle() != null ? parsed.getTitle() : "未命名事件");
        event.setStartTime(LocalDateTime.parse(parsed.getStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (parsed.getEndTime() != null && !parsed.getEndTime().isEmpty()) {
            event.setEndTime(LocalDateTime.parse(parsed.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        eventMapper.insert(event);

        result.setSuccess(true);
        result.setMessage("日程已创建：「" + event.getTitle() + "」" +
                event.getStartTime().format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm")));
    }

    private void handleQuery(AIResponse parsed, ProcessResult result) {
        String dateStr = parsed.getDate();
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<Event> events = getEventsByDate(date);
        List<EventVO> eventVOs = events.stream().map(e -> {
            EventVO vo = new EventVO();
            vo.setId(e.getId());
            vo.setTitle(e.getTitle());
            vo.setStartTime(e.getStartTime());
            vo.setEndTime(e.getEndTime());
            return vo;
        }).collect(Collectors.toList());

        result.setSuccess(true);
        result.setEvents(eventVOs);
        result.setMessage(eventVOs.isEmpty()
                ? dateStr + " 没有日程安排"
                : dateStr + " 共有 " + eventVOs.size() + " 个日程");
    }

    private void handleDelete(AIResponse parsed, ProcessResult result) {
        String dateStr = parsed.getDate();
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        QueryWrapper qw = QueryWrapper.create()
                .where(EVENT.START_TIME.between(startOfDay, endOfDay));

        if (parsed.getTitle() != null && !parsed.getTitle().isEmpty()) {
            qw.where(EVENT.TITLE.like("%" + parsed.getTitle() + "%"));
        }

        List<Event> events = eventMapper.selectListByQuery(qw);
        if (events.isEmpty()) {
            result.setSuccess(false);
            result.setMessage(dateStr + " 没有找到匹配的日程");
            return;
        }

        for (Event event : events) {
            eventMapper.deleteById(event.getId());
        }

        result.setSuccess(true);
        result.setMessage("已删除 " + events.size() + " 个日程");
    }

    @Override
    public List<Event> getEventsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        QueryWrapper qw = QueryWrapper.create()
                .where(EVENT.START_TIME.between(startOfDay, endOfDay))
                .orderBy(EVENT.START_TIME, true);

        return eventMapper.selectListByQuery(qw);
    }

    @Override
    public Event getEventById(Long id) {
        return eventMapper.selectOneById(id);
    }
}
