/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/
package net.floodlightcontroller.packet;

import java.nio.ByteBuffer;

public class RTP extends BasePacket {

	private static final byte V2 = (byte) 0x80;
	/*
	 * RFC3550 - Section 5.1 https://tools.ietf.org/html/rfc3550#section-5.1
	 * 
	 * 0 1 2 3 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * |V=2|P|X| CC |M| PT | sequence number |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * timestamp |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * synchronization source (SSRC) identifier |
	 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+ |
	 * contributing source (CSRC) identifiers | | .... |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */
	// 2bits
	private int version;
	// 1bit
	private boolean padding;
	// 1bit
	private boolean extension;
	// 4bits
	private int csrcCount;
	// 1bit
	private boolean marker;
	// 7bits
	private RTPPayloadType payloadType;
	// 16bits
	private int sequenceNumber;
	// 32bits
	private long timestamp;
	// 32bits
	private long ssrc;

	private static final int HEADER_SIZE = 12;

	// 32bits - NOT IMPLEMENTED - Feel free to help and implement
	// private int CSRC;
	// 32bits - NOT IMPLEMENTED - Feel free to help and implement
	// private int headerExtension;

	
	@Override
	public byte[] serialize() {
		// not guaranteed to retain length/exact format
		resetChecksum();

		byte[] data = new byte[12];
		ByteBuffer bf = ByteBuffer.wrap(data);
		bf.putInt(this.version);
		bf.putInt(this.padding ? 0:1);
		bf.putInt(this.extension ? 0:1);
		// TODO: Corrigir isso
		bf.putInt(this.csrcCount);
		bf.putInt(this.marker ? 0:1);
		bf.putInt(this.payloadType.type);
		bf.putInt(this.sequenceNumber);
		bf.putLong(this.timestamp);
		bf.putLong(this.ssrc);
		//bf.put(((Data) payload).getData());
		return data;
	}

	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException {

		// RTP header inits on byte 42
		if (length < 28) {
			throw new PacketParsingException("Not an RTP or RTCP packet");
		}
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length - HEADER_SIZE);
		byte b = bb.get();
		
		version = (byte) (b & 0xC0);
		if (version != V2) {
			throw new PacketParsingException("Not an RTP or RTCP packet or other Version than V2");
		}
		
		// 1bit 00100000
		padding = (b & 0x20) > 0;
		// 1bit 00010000
		extension = (b & 0x10) > 0;
		// 4bits 00001111
		csrcCount = b & 0x0F;

		b = bb.get();

		// 1bit 00000001
		marker = (b & 0x80) > 0;

		// 7bits 01111111
		payloadType = RTPPayloadType.getType((b & 0x7F));

		short sh = bb.getShort();
		// 16bits
		sequenceNumber = sh >= 0 ? sh : 0x10000 + sh;
		
		// 32bits
		timestamp = bb.getInt();

		// 32bits
		ssrc = bb.getInt();

		// 32bits - NOT IMPLEMENTED - Feel free to help and complete the impl
		// CSRC;
		
		
		return this;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public boolean isPadding() {
		return padding;
	}

	public void setPadding(boolean padding) {
		this.padding = padding;
	}

	public boolean isExtension() {
		return extension;
	}

	public void setExtension(boolean extension) {
		this.extension = extension;
	}

	public int getCsrcCount() {
		return csrcCount;
	}

	public void setCsrcCount(int csrcCount) {
		this.csrcCount = csrcCount;
	}

	public boolean isMarker() {
		return marker;
	}

	public void setMarker(boolean marker) {
		this.marker = marker;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getSSRC() {
		return ssrc;
	}

	public void setSSRC(long sSRC) {
		ssrc = sSRC;
	}

	public RTPPayloadType getPayloadType() {
		return payloadType;
	}

	public void setPayloadType(RTPPayloadType payloadType) {
		this.payloadType = payloadType;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void setSSRC(int sSRC) {
		ssrc = sSRC;
	}

	public boolean isRTCP() {
		return this.payloadType.equals(RTPPayloadType.RTCP);
	}

}
