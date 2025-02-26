package io.vels.ai.tools;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;

public class DateTimeTools {

    Logger log = LoggerFactory.getLogger(DateTimeTools.class);

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(description = "Set the alarm for the given date and time, provided in ISO-8601 format")
    String setAlarm(String time) {
        return "Alarm set at the time : " + time;
    }
}