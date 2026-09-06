package ch.rasc.eventbus.demo.distributed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.rasc.eventbus.demo.distributed.SseController.ChatMessage;
import ch.rasc.sse.eventbus.DefaultSubscriptionRegistry;
import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.SseEventBus;
import ch.rasc.sse.eventbus.config.SseEventBusConfigurer;

class SseControllerTest {

	@Test
	void publishesTypedMessagesWithControlCharacters() {
		List<Object> publishedEvents = new ArrayList<>();
		RecordingSseEventBus eventBus = new RecordingSseEventBus();
		SseController controller = new SseController(eventBus, publishedEvents::add, "Node A");

		controller.send("line 1\nline 2\t");
		controller.send("  ");

		assertThat(publishedEvents).hasSize(1);
		SseEvent event = (SseEvent) publishedEvents.getFirst();
		assertThat(event.event()).isEqualTo("chat");
		assertThat(event.data()).isEqualTo(new ChatMessage("line 1\nline 2", "Node A"));
	}

	@Test
	void registersProxySafeSseConnections() {
		RecordingSseEventBus eventBus = new RecordingSseEventBus();
		SseController controller = new SseController(eventBus, event -> {
		}, "Node A");
		MockHttpServletResponse response = new MockHttpServletResponse();

		SseEmitter emitter = controller.register("client-1", response);

		assertThat(emitter).isSameAs(eventBus.emitter);
		assertThat(eventBus.clientId).isEqualTo("client-1");
		assertThat(eventBus.timeout).isZero();
		assertThat(eventBus.events).containsExactly("chat");
		assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(response.getHeader("X-Accel-Buffering")).isEqualTo("no");
		assertThat(new SseEventBusConfiguration().heartbeatInterval()).isEqualTo(Duration.ofSeconds(15));
	}

	private static final class RecordingSseEventBus extends SseEventBus {

		private final SseEmitter emitter = new SseEmitter();

		private String clientId;

		private Long timeout;

		private List<String> events;

		private RecordingSseEventBus() {
			super(new SseEventBusConfigurer() {
				@Override
				public ScheduledExecutorService taskScheduler() {
					return null;
				}
			}, new DefaultSubscriptionRegistry(), List.of(), null);
		}

		@Override
		public SseEmitter createSseEmitter(String clientId, Long timeout, String... events) {
			this.clientId = clientId;
			this.timeout = timeout;
			this.events = Arrays.asList(events);
			return this.emitter;
		}

	}

}
