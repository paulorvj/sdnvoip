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

import java.util.Arrays;

import javax.sip.SipFactory;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;

public class SIP extends BasePacket {

	
	private static final int SIP_HEADER_INIT_BYTE = 42;
	private static final String SIP_2_0 = "SIP/2.0";
	
	private SIPType sipType;
	private Message sipMessage;
	
	public SIP() {
	}

	@Override
	public byte[] serialize() {
		return this.sipMessage.getRawContent();
	}

	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException {
		byte[] b2 = Arrays.copyOfRange(data, SIP_HEADER_INIT_BYTE, data.length);
		String txt = new String(b2);
		SipFactory sipFactory = null;
        MessageFactory messageFactory;

        sipFactory = SipFactory.getInstance();
        //sipFactory.setPathName("gov.nist");

        try {
			messageFactory = sipFactory.createMessageFactory();
	        
			if ( txt.trim().equalsIgnoreCase("jak") )
			{
				this.sipType = SIPType.JAK;
				return this;
			}
	        String firstWord = txt.substring(0, txt.indexOf(" "));
	        if ( firstWord.equalsIgnoreCase(SIP_2_0) )
	        {
	        	sipMessage = messageFactory.createResponse(txt);
	        	this.sipType = SIPType.RESPONSE;
	        }
	        else
	        {
	        	sipMessage = messageFactory.createRequest(txt);
	        	this.sipType = SIPType.REQUEST;
	        }	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new PacketParsingException(e.getMessage());
		}
		return this;
	}

	public SIPType getSipType() {
		return sipType;
	}

	public Message getSipMessage() {
		return sipMessage;
	}

}
