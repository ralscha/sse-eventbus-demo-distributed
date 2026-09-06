package ch.rasc.eventbus.demo.distributed;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;

import ch.rasc.sse.eventbus.config.SseEventBusConfigurer;

@Configuration
public class SseEventBusConfiguration implements SseEventBusConfigurer {

	@Override
	public Duration heartbeatInterval() {
		return Duration.ofSeconds(15);
	}

}
