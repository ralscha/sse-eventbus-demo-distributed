package ch.rasc.eventbus.demo.distributed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.springframework.stereotype.Component;

import ch.rasc.sse.eventbus.distributed.RemoteSseEventEnvelope;

@Component
public class JavaSerializationRemoteEventCodec implements RemoteEventCodec {

	@Override
	public byte[] encode(RemoteSseEventEnvelope envelope) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(envelope);
			return bos.toByteArray();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to serialize event for Redis", e);
		}
	}

	@Override
	public RemoteSseEventEnvelope decode(byte[] payload) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(payload);
				ObjectInputStream ois = new ObjectInputStream(bis)) {
			return (RemoteSseEventEnvelope) ois.readObject();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to deserialize event from Redis", e);
		}
	}

}