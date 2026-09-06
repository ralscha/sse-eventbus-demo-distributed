package ch.rasc.eventbus.demo.distributed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import ch.rasc.eventbus.demo.distributed.SseController.ChatMessage;
import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.distributed.RemoteSseEventEnvelope;

class JavaSerializationRemoteEventCodecTest {

	@Test
	void roundTripsRemoteEventEnvelope() {
		JavaSerializationRemoteEventCodec codec = new JavaSerializationRemoteEventCodec();
		SseEvent event = SseEvent.builder()
			.event("chat")
			.id("event-1")
			.data(new ChatMessage("line 1\nline 2", "Node A"))
			.build();
		RemoteSseEventEnvelope original = new RemoteSseEventEnvelope("node-1", event);

		RemoteSseEventEnvelope decoded = codec.decode(codec.encode(original));

		assertThat(decoded.originNodeId()).isEqualTo("node-1");
		assertThat(decoded.event().event()).isEqualTo("chat");
		assertThat(decoded.event().id()).contains("event-1");
		assertThat(decoded.event().data()).isEqualTo(event.data());
	}

	@Test
	void rejectsNullPayloadWithoutLeakingANullPointerException() {
		JavaSerializationRemoteEventCodec codec = new JavaSerializationRemoteEventCodec();

		assertThatThrownBy(() -> codec.decode(codec.encode(null)))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Failed to deserialize event from Redis")
			.hasCauseInstanceOf(java.io.IOException.class);
	}

}
