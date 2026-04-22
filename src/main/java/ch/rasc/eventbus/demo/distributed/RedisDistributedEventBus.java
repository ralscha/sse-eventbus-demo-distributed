package ch.rasc.eventbus.demo.distributed;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import ch.rasc.sse.eventbus.DistributedEventBus;
import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.distributed.RemoteSseEventEnvelope;

@Component
public class RedisDistributedEventBus implements DistributedEventBus, MessageListener {

	static final String CHANNEL = "sse-eventbus";

	private final String nodeId = UUID.randomUUID().toString();

	private final AtomicReference<Consumer<SseEvent>> remoteEventConsumer = new AtomicReference<>();

	private final RedisTemplate<String, byte[]> redisTemplate;

	private final RemoteEventCodec codec;

	public RedisDistributedEventBus(RedisTemplate<String, byte[]> redisTemplate,
			RemoteEventCodec codec) {
		this.redisTemplate = redisTemplate;
		this.codec = codec;
	}

	@Override
	public void publishRemote(SseEvent event) {
		RemoteSseEventEnvelope envelope = new RemoteSseEventEnvelope(this.nodeId, event);
		try {
			this.redisTemplate.convertAndSend(CHANNEL, this.codec.encode(envelope));
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to publish event to Redis", e);
		}
	}

	@Override
	public void setRemoteEventConsumer(Consumer<SseEvent> consumer) {
		if (!this.remoteEventConsumer.compareAndSet(null, consumer)) {
			throw new IllegalStateException("setRemoteEventConsumer has already been called");
		}
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		byte[] body = message.getBody();
		try {
			RemoteSseEventEnvelope envelope = this.codec.decode(body);
			if (this.nodeId.equals(envelope.originNodeId())) {
				return;
			}
			Consumer<SseEvent> consumer = this.remoteEventConsumer.get();
			if (consumer != null) {
				consumer.accept(envelope.event());
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to deserialize event from Redis", e);
		}
	}

}
