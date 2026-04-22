package ch.rasc.eventbus.demo.distributed;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
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
		SseEmitter emitter = this.eventBus.createSseEmitter(clientId, 0L, "chat");
		try {
			emitter.send(SseEmitter.event().comment("connected"));
		}
		catch (IOException ex) {
			emitter.completeWithError(ex);
		}
		return emitter;
	}

	@PostMapping("/send")
	@ResponseBody
	public void send(@RequestBody String text) {
		String payload = "{\"text\":" + jsonString(text) + ",\"node\":" + jsonString(this.nodeName) + "}";
		this.eventPublisher.publishEvent(SseEvent.of("chat", payload));
	}

	private static String jsonString(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

}
