package ch.rasc.eventbus.demo.distributed;

import ch.rasc.sse.eventbus.distributed.RemoteSseEventEnvelope;

interface RemoteEventCodec {

	byte[] encode(RemoteSseEventEnvelope envelope);

	RemoteSseEventEnvelope decode(byte[] payload);

}
