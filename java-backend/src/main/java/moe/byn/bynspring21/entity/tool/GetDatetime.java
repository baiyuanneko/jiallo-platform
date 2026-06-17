package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class GetDatetime {

    @Tool(description = "获取当前日期和时间。当用户询问'现在几点'、'今天几号'、'当前时间'等与当前日期时间相关的问题时使用此工具。")
    public String getDatetime(
            @ToolParam(description = "时区偏移量，例如'+08:00'表示东八区，默认为'+08:00'", required = false) String timezone) {

        ZoneId zoneId = (timezone != null && !timezone.isBlank())
                ? ZoneId.of(timezone)
                : ZoneId.of("+08:00");
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        long epochMillis = now.toInstant().toEpochMilli();

        log.debug("getDatetime invoked, timezone={}, result={}", timezone, formatted);

        return String.format(
                "当前时间：%s\n时间戳（毫秒）：%d",
                formatted, epochMillis
        );
    }
}
