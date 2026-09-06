package ch.rasc.eventbus.demo.distributed;

import java.io.Serial;
import java.io.Serializable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.SseEventBus;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class SseController {

	private final SseEventBus eventBus;

	private final ApplicationEventPublisher eventPublisher;

	private final String nodeName;

	public SseController(SseEventBus eventBus, ApplicationEventPublisher eventPublisher,
			@Value("${app.node-name}") String nodeName) {
		this.eventBus = eventBus;
		this.eventPublisher = eventPublisher;
		this.nodeName = nodeName;
	}

	@GetMapping("/register/{clientId}")
	public SseEmitter register(@PathVariable String clientId, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("X-Accel-Buffering", "no");
		return this.eventBus.createSseEmitter(clientId, 0L, "chat");
	}

	@PostMapping("/send")
	@ResponseBody
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void send(@RequestBody String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		this.eventPublisher.publishEvent(SseEvent.of("chat", new ChatMessage(text.strip(), this.nodeName)));
	}

	public record ChatMessage(String text, String node) implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

	}

}
