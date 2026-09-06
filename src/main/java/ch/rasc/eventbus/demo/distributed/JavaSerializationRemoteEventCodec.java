package ch.rasc.eventbus.demo.distributed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.springframework.stereotype.Component;

import ch.rasc.sse.eventbus.distributed.RemoteSseEventEnvelope;

@Component
public class JavaSerializationRemoteEventCodec implements RemoteEventCodec {

	private static final ObjectInputFilter INPUT_FILTER = ObjectInputFilter.Config.createFilter(
			"maxdepth=20;maxrefs=1000;maxbytes=1048576;java.base/*;ch.rasc.sse.eventbus.**;ch.rasc.eventbus.demo.distributed.**;!*");

	@Override
	public byte[] encode(RemoteSseEventEnvelope envelope) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(envelope);
			return bos.toByteArray();
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to serialize event for Redis", e);
		}
	}

	@Override
	public RemoteSseEventEnvelope decode(byte[] payload) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(payload);
				ObjectInputStream ois = new ObjectInputStream(bis)) {
			ois.setObjectInputFilter(INPUT_FILTER);
			Object decoded = ois.readObject();
			if (decoded instanceof RemoteSseEventEnvelope envelope) {
				return envelope;
			}
			String decodedType = decoded == null ? "null" : decoded.getClass().getName();
			throw new IOException("Unexpected remote event type: " + decodedType);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to deserialize event from Redis", e);
		}
	}

}
