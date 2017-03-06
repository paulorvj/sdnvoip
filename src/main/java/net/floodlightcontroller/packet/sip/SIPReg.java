package net.floodlightcontroller.packet.sip;

import java.util.Vector;

import javax.sdp.Media;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

import gov.nist.javax.sdp.fields.AttributeField;

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
public class SIPReg {

	private String user;
	private String callTo;
	private String callFrom;
	private IPv4Address ip;
	private TransportPort sipTransportPort;
	private TransportPort srcRTPTransportPort;
	private TransportPort dstRTPTransportPort;
	private Media media;
	private Vector<AttributeField> audioAttributes;
	private OFPort switchPort;
	private MacAddress macAddress;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public IPv4Address getIp() {
		return ip;
	}

	public void setIp(IPv4Address ip) {
		this.ip = ip;
	}

	public TransportPort getSipTransportPort() {
		return sipTransportPort;
	}

	public void setSipTransportPort(TransportPort sipTransportPort) {
		this.sipTransportPort = sipTransportPort;
	}

	public TransportPort getSrcRTPTransportPort() {
		return srcRTPTransportPort;
	}

	public void setSrcRTPTransportPort(TransportPort srcRTPTransportPort) {
		this.srcRTPTransportPort = srcRTPTransportPort;
	}

	public TransportPort getDstRTPTransportPort() {
		return dstRTPTransportPort;
	}

	public void setDstRTPTransportPort(TransportPort dstRTPTransportPort) {
		this.dstRTPTransportPort = dstRTPTransportPort;
	}

	public Media getMedia() {
		return media;
	}

	public void setMedia(Media media) {
		this.media = media;
	}

	public Vector<AttributeField> getAudioAttributes() {
		return audioAttributes;
	}

	public void setAudioAttributes(Vector<AttributeField> audioAttributes) {
		this.audioAttributes = audioAttributes;
	}

	public OFPort getSwitchPort() {
		return switchPort;
	}

	public void setSwitchPort(OFPort switchPort) {
		this.switchPort = switchPort;
	}

	public String getCallTo() {
		return callTo;
	}

	public void setCallTo(String callTo) {
		this.callTo = callTo;
	}

	public String getCallFrom() {
		return callFrom;
	}

	public void setCallFrom(String callFrom) {
		this.callFrom = callFrom;
	}
	
	public boolean isValid()
	{
		return (this.srcRTPTransportPort != null &&
				this.dstRTPTransportPort != null);
	}

	public MacAddress getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(MacAddress macAddress) {
		this.macAddress = macAddress;
	}

	@Override
	public String toString() {
		String str = "User: " + user+"\n"+
		"IP: " + ip.toString()+"\n"+
		"SIP Port: " + sipTransportPort.toString()+"\n"+
		"SRC RTP Port: " + srcRTPTransportPort+"\n"+
		"DST RTP Port: " + dstRTPTransportPort+"\n"+
		"SW POrt: " + switchPort.toString();
		return str;
	}
	
	

}
