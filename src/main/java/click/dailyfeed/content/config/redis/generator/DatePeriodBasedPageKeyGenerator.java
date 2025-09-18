package click.dailyfeed.content.config.redis.generator;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DatePeriodBasedPageKeyGenerator implements KeyGenerator {
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    @Override
    public Object generate(Object target, Method method, Object... params) {
        LocalDateTime startDateTime = (LocalDateTime) params[0];
        LocalDateTime endDateTime = (LocalDateTime) params[1];
        return String.format("period__from_%s_to_%s__page_%s_size_%s", startDateTime.format(dateTimeFormatter), endDateTime.format(dateTimeFormatter), params[2], params[3]);
    }
}
