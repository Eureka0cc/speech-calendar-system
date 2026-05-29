package com.speechcalendar.controller;

import com.speechcalendar.dto.EventVO;
import com.speechcalendar.dto.ProcessRequest;
import com.speechcalendar.dto.ProcessResult;
import com.speechcalendar.entity.Event;
import com.speechcalendar.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "语音日历", description = "语音日历事件的创建、查询、删除")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "自然语言处理", description = "输入自然语言文本，AI 解析意图后自动执行创建/查询/删除操作")
    @PostMapping("/process")
    public ProcessResult process(@RequestBody ProcessRequest request) {
        return eventService.process(request.getText(), request.getUserId());
    }

    @Operation(summary = "查询指定日期日程", description = "查询某天的所有日程，格式 yyyy-MM-dd")
    @GetMapping
    public List<EventVO> getEvents(
            @Parameter(description = "日期，格式 yyyy-MM-dd，默认当天") @RequestParam(required = false) String date) {
        LocalDate queryDate;
        if (date == null || date.isEmpty()) {
            queryDate = LocalDate.now();
        } else {
            queryDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return eventService.getEventsByDate(queryDate).stream().map(e -> {
            EventVO vo = new EventVO();
            vo.setId(e.getId());
            vo.setTitle(e.getTitle());
            vo.setStartTime(e.getStartTime());
            vo.setEndTime(e.getEndTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Operation(summary = "查询单个日程详情")
    @GetMapping("/{id}")
    public Event getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }
}
