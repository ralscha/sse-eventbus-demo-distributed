package ch.rasc.eventbus.demo.distributed;

import ch.rasc.sse.eventbus.distributed.RemoteSseEventEnvelope;

public interface RemoteEventCodec {

	byte[] encode(RemoteSseEventEnvelope envelope);

	RemoteSseEventEnvelope decode(byte[] payload);

}