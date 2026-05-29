package com.speechcalendar.mapper;

import com.mybatisflex.core.BaseMapper;
import com.speechcalendar.entity.Event;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventMapper extends BaseMapper<Event> {
}
