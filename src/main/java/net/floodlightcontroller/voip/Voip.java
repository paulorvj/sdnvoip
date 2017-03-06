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
package net.floodlightcontroller.voip;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nist.javax.sip.address.Authority;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.forwarding.Forwarding;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.SIP;
import net.floodlightcontroller.packet.SIPType;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.packet.UDPType;
import net.floodlightcontroller.packet.sip.SIPReg;
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.topology.ITopologyService;

public class Voip extends ForwardingBase implements IFloodlightModule {

	public static final String DEFAULT_SERVERS_FILE = "src/main/resources/voipservers.txt";
	private final int hardTimeout = 0;
	private static ConcurrentMap<String, SIPReg> sips = new ConcurrentHashMap<>();
	protected static Logger log = LoggerFactory.getLogger(Forwarding.class);
	private IPv4Address VOIP_SERVER_IP;
	private List<IPv4Address> voipServers = new ArrayList<>();
	
	private void readVoipServers() {
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(DEFAULT_SERVERS_FILE));
		    String line;
		    while ((line = reader.readLine()) != null)
		    {
		    	voipServers.add(IPv4Address.of(line));
		    }
		    reader.close();
		}catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IDeviceService.class);
		l.add(IRoutingService.class);
		l.add(ITopologyService.class);
		l.add(IDebugCounterService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		super.init();
		this.floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		this.deviceManagerService = context.getServiceImpl(IDeviceService.class);
		this.routingEngineService = context.getServiceImpl(IRoutingService.class);
		this.topologyService = context.getServiceImpl(ITopologyService.class);
		this.debugCounterService = context.getServiceImpl(IDebugCounterService.class);
		this.switchService = context.getServiceImpl(IOFSwitchService.class);

		FLOWMOD_DEFAULT_HARD_TIMEOUT = 0;
		FLOWMOD_DEFAULT_IDLE_TIMEOUT = 0;
		readVoipServers();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		super.startUp();
	}
	
	@Override
	public net.floodlightcontroller.core.IListener.Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi,
			IRoutingDecision decision, FloodlightContext cntx) {

		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		IPv4Address dstIp = null;

		if (eth.getEtherType() == EthType.ARP) {
			doFlood(sw, pi, cntx);
			return Command.CONTINUE;
		} else if (!eth.isBroadcast() && !eth.isMulticast()) {
			
			if (eth.getEtherType() == EthType.IPv4) {
				IPv4 ipv4 = null;
				try
				{
					ipv4 = (IPv4) eth.getPayload();
				}
				catch (ClassCastException c)
				{
					return Command.CONTINUE;
				}
				

				IPv4Address sourceIp = ipv4.getSourceAddress();
				if ( voipServers.contains(sourceIp) )
				{
					VOIP_SERVER_IP = sourceIp;
				}
				dstIp = ipv4.getDestinationAddress();
				
				if (ipv4.getProtocol() == IpProtocol.UDP) {
					UDP udp = (UDP) ipv4.getPayload();

					TransportPort srcPort = udp.getSourcePort();
					TransportPort dstPort = udp.getDestinationPort();
					
					if ( udp.getUdpType() == UDPType.RTP || udp.getUdpType() == UDPType.RTCP )
					{
						if ( sourceIp.compareTo(VOIP_SERVER_IP) != 0 )
						{
							for (SIPReg r : sips.values()) {
								if ( r.isValid() &&
										r.getSrcRTPTransportPort().compareTo(srcPort)==0 )
								{
									if ( r.getCallTo() != null )
									{
										if ( sips.get(r.getCallTo()) != null )
											installFlows(r, sw, cntx, inPort);
									}
									else if ( sips.get(r.getCallFrom()).isValid() )
									{
										installFlows(r, sw, cntx, inPort);
									}
								}
							}
						}
						return Command.CONTINUE;
					}
					else if ( udp.getUdpType() == UDPType.SIP )
					{
						SIP sip = (SIP) udp.getPayload();
						
						if ( sip.getSipType() == SIPType.JAK )
						{
							return Command.CONTINUE;
						}
						else if ( sip.getSipType() == SIPType.REQUEST )
						{
							SIPRequest req = (SIPRequest) sip.getSipMessage();
							if ( req.getMethod().equalsIgnoreCase(SIPRequest.INVITE) )
							{
								if ( req.getContentTypeHeader().getContentSubType().equalsIgnoreCase("sdp") )
								{
									if ( req.getAuthorization() != null )
									{
										processSDPInviteFromClientToVOIPServer(req, sourceIp, srcPort, inPort, eth.getSourceMACAddress());
									}
									else if ( sourceIp.compareTo(VOIP_SERVER_IP) == 0 )
									{
										processSDPInviteFromVOIPServerToClient(req, dstIp, dstPort);
									}
								}
							}
						}
						else if ( sip.getSipType() == SIPType.RESPONSE )
						{
							SIPResponse res = (SIPResponse) sip.getSipMessage();
							if ( res.getStatusCode() == SIPResponse.OK &&
									res.getCSeqHeader().getMethod().equalsIgnoreCase(SIPRequest.REGISTER) )
							{
								try
								{
									System.out.println("Register: " + res.getContactHeader().getAddress());
								}
								catch(NullPointerException npe)
								{
									System.out.println("Unregister: " + res.getFrom());
								}
							}
							else if ( res.getStatusCode() == SIPResponse.OK &&
										res.getCSeqHeader().getMethod().equalsIgnoreCase(SIPRequest.INVITE))
							{
								if ( res.getContentTypeHeader().getContentSubType().equalsIgnoreCase("sdp") )
								{
									if ( sourceIp.compareTo(VOIP_SERVER_IP) == 0 )
									{
										processSDPInviteOkResponseFromServer(res, sourceIp, srcPort, inPort);
									}
									else
									{
										SIPReg reg = processSDPInviteOkResponseFromClient(res, sourceIp, srcPort, inPort, eth.getSourceMACAddress());
										installFlows(reg, sw, cntx, inPort);
									}
								}
								 
							}
						}
						IDevice dstDevice = null;
						for (IDevice dev : deviceManagerService.getAllDevices()) {
							if ( dev == null || dev.getIPv4Addresses().length == 0 )
							{
								return Command.CONTINUE;
							}
							IPv4Address addr = dev.getIPv4Addresses()[0];
							if ( dstIp == null )
							{
								break;
							}
							if ( addr.compareTo(dstIp) == 0 )
							{
								dstDevice = dev;
								break;
							}
						}
						
						if ( dstDevice != null)
						{
							SwitchPort[] pts = dstDevice.getAttachmentPoints();
							if ( pts.length > 0 )
							{
								OFPort outPort = pts[0].getPort();
								pushPacket(sw, pi, false, outPort, cntx);
							}
						}
					}
				}
			}
		}
		return Command.CONTINUE;
	}

	private void installFlows(SIPReg reg, IOFSwitch sw, FloodlightContext cntx, OFPort inPort) {
		
		SIPReg to = reg;
		SIPReg from = sips.get(reg.getCallFrom());
		
		List<OFAction> actionsTo = new ArrayList<OFAction>();
		List<OFAction> actionsFrom = new ArrayList<OFAction>();
		
		List<OFAction> actionsToRTCP = new ArrayList<OFAction>();
		List<OFAction> actionsFromRTCP = new ArrayList<OFAction>();
		
		OFFactory my13Factory = OFFactories.getFactory(OFVersion.OF_13);
		OFActions actions = my13Factory.actions();
		OFOxms oxms = my13Factory.oxms();
		
		IDevice voipServer = findDevice(VOIP_SERVER_IP);
		
		OFActionSetField setSrcIPToVoipServer = actions.buildSetField()
			    .setField(
			        oxms.buildIpv4Src()
			        .setValue(VOIP_SERVER_IP)
			        .build()
			    )
			    .build();
		
		OFActionSetField setSrcMacToVoipServer = actions.buildSetField()
			    .setField(
			        oxms.buildEthSrc()
			        .setValue(voipServer.getMACAddress())
			        .build()
			    )
			    .build();
		
		/*
		 * ACTIONS TO
		 */
		OFActionSetField setDstIPTo = actions.buildSetField()
			    .setField(
			        oxms.buildIpv4Dst()
			        .setValue(from.getIp())
			        .build()
			    )
			    .build();
		
		OFActionSetField setSrcUdpTo = actions.buildSetField()
				.setField(
				    oxms.buildUdpSrc()
				    .setValue(from.getDstRTPTransportPort())
				    .build()
			    )
			    .build();
		
		OFActionSetField setDstUdpTo = actions.buildSetField()
				.setField(
				    oxms.buildUdpDst()
				    .setValue(from.getSrcRTPTransportPort())
				    .build()
			    )
			    .build();
		
		OFActionSetField setMacTo = actions.buildSetField()
			    .setField(
			        oxms.buildEthDst()
			        .setValue(from.getMacAddress())
			        .build()
			    )
			    .build();
		
		OFActionOutput outputPortTo = actions.buildOutput()
			    .setMaxLen(0xFFffFFff)
			    .setPort(from.getSwitchPort())
			    .build();
		
		/*
		 * ACTIONS FROM
		 */
		OFActionSetField setDstIPFrom = actions.buildSetField()
			    .setField(
			        oxms.buildIpv4Dst()
			        .setValue(to.getIp())
			        .build()
			    )
			    .build();
		
		OFActionSetField setUdpSrcFrom = actions.buildSetField()
				.setField(
				    oxms.buildUdpSrc()
				    .setValue(to.getDstRTPTransportPort())
				    .build()
			    )
			    .build();
		
		OFActionSetField setUdpDstFrom = actions.buildSetField()
				.setField(
				    oxms.buildUdpDst()
				    .setValue(to.getSrcRTPTransportPort())
				    .build()
			    )
			    .build();
		
		OFActionSetField setMacFrom = actions.buildSetField()
			    .setField(
			        oxms.buildEthDst()
			        .setValue(to.getMacAddress())
			        .build()
			    )
			    .build();
		
		OFActionOutput outputPortFrom = actions.buildOutput()
			    .setMaxLen(0xFFffFFff)
			    .setPort(to.getSwitchPort())
			    .build();
		
		/*
		 * ACTIONS RTCP
		 */
		int rtcpSrcTo = Integer.parseInt(to.getSrcRTPTransportPort().toString());
		int rtcpSrcFrom = Integer.parseInt(from.getSrcRTPTransportPort().toString());
		int rtcpDstTo = Integer.parseInt(to.getDstRTPTransportPort().toString());
		int rtcpDstFrom = Integer.parseInt(from.getDstRTPTransportPort().toString());
		
		OFActionSetField setDstIPToRTCP = actions.buildSetField()
			    .setField(
			        oxms.buildIpv4Dst()
			        .setValue(from.getIp())
			        .build()
			    )
			    .build();
		
		OFActionSetField setSrcUdpToRTCP = actions.buildSetField()
				.setField(
				    oxms.buildUdpSrc()
				    .setValue(TransportPort.of(rtcpDstFrom+1))
				    .build()
			    )
			    .build();
		
		OFActionSetField setUdpDstToRTCP = actions.buildSetField()
				.setField(
				    oxms.buildUdpDst()
				    .setValue(TransportPort.of(rtcpSrcFrom+1))
				    .build()
			    )
			    .build();
		
		OFActionSetField setMacToRTCP = actions.buildSetField()
			    .setField(
			        oxms.buildEthDst()
			        .setValue(from.getMacAddress())
			        .build()
			    )
			    .build();
		
		OFActionOutput outputPortToRTCP = actions.buildOutput()
			    .setMaxLen(0xFFffFFff)
			    .setPort(from.getSwitchPort())
			    .build();
		
		OFActionSetField setSrcUdpFromRTCP = actions.buildSetField()
				.setField(
				    oxms.buildUdpSrc()
				    .setValue(TransportPort.of(rtcpDstTo+1))
				    .build()
			    )
			    .build();
		
		OFActionSetField setUdpDstFromRTCP = actions.buildSetField()
				.setField(
				    oxms.buildUdpDst()
				    .setValue(TransportPort.of(rtcpSrcTo+1))
				    .build()
			    )
			    .build();
		
		OFActionSetField setDstIPFromRTCP = actions.buildSetField()
			    .setField(
			        oxms.buildIpv4Dst()
			        .setValue(to.getIp())
			        .build()
			    )
			    .build();
		
		OFActionOutput outputPortFromRTCP = actions.buildOutput()
			    .setMaxLen(0xFFffFFff)
			    .setPort(to.getSwitchPort())
			    .build();
		
		OFActionSetField setMacFromRTCP = actions.buildSetField()
			    .setField(
			        oxms.buildEthDst()
			        .setValue(to.getMacAddress())
			        .build()
			    )
			    .build();
		
		actionsTo.add(setDstIPTo);
		actionsTo.add(setDstUdpTo);
		actionsTo.add(setMacTo);
		actionsTo.add(setSrcIPToVoipServer);
		actionsTo.add(setSrcUdpTo);
		actionsTo.add(setSrcMacToVoipServer);
		actionsTo.add(outputPortTo);
		
		actionsFrom.add(setDstIPFrom);
		actionsFrom.add(setUdpDstFrom);
		actionsFrom.add(setMacFrom);
		actionsFrom.add(setSrcIPToVoipServer);
		actionsFrom.add(setUdpSrcFrom);
		actionsFrom.add(setSrcMacToVoipServer);
		actionsFrom.add(outputPortFrom);
		
		actionsToRTCP.add(setSrcIPToVoipServer);
		actionsToRTCP.add(setDstIPToRTCP);
		actionsToRTCP.add(setSrcUdpToRTCP);
		actionsToRTCP.add(setUdpDstToRTCP);
		actionsToRTCP.add(setMacToRTCP);
		actionsToRTCP.add(setSrcMacToVoipServer);
		actionsToRTCP.add(outputPortToRTCP);
		
		actionsFromRTCP.add(setSrcIPToVoipServer);
		actionsFromRTCP.add(setDstIPFromRTCP);
		actionsFromRTCP.add(setSrcUdpFromRTCP);
		actionsFromRTCP.add(setUdpDstFromRTCP);
		actionsFromRTCP.add(setMacFromRTCP);
		actionsFromRTCP.add(setSrcMacToVoipServer);
		actionsFromRTCP.add(outputPortFromRTCP);
		
		OFFactory myFactory = sw.getOFFactory();
		
		Match.Builder mbTo = sw.getOFFactory().buildMatch();
		Match.Builder mbFrom = sw.getOFFactory().buildMatch();
		
		Match.Builder mbToRTCP = sw.getOFFactory().buildMatch();
		Match.Builder mbFromRTCP = sw.getOFFactory().buildMatch();
		
		
		mbTo.setExact(MatchField.ETH_TYPE, EthType.IPv4)
			.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
			.setExact(MatchField.IPV4_SRC, to.getIp())
			.setExact(MatchField.IPV4_DST, VOIP_SERVER_IP)
			.setExact(MatchField.UDP_SRC, to.getSrcRTPTransportPort())
			.setExact(MatchField.UDP_DST, to.getDstRTPTransportPort());
		
		mbFrom.setExact(MatchField.ETH_TYPE, EthType.IPv4)
			.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
			.setExact(MatchField.IPV4_SRC, from.getIp())
			.setExact(MatchField.IPV4_DST, VOIP_SERVER_IP)
			.setExact(MatchField.UDP_SRC, from.getSrcRTPTransportPort())
			.setExact(MatchField.UDP_DST, from.getDstRTPTransportPort());
		
		/*
		 * 
		 */
		
		mbToRTCP.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
		.setExact(MatchField.IPV4_SRC, to.getIp())
		.setExact(MatchField.IPV4_DST, VOIP_SERVER_IP)
		.setExact(MatchField.UDP_SRC, TransportPort.of((rtcpSrcTo+1)))
		.setExact(MatchField.UDP_DST, to.getDstRTPTransportPort());
		
		
		mbFromRTCP.setExact(MatchField.ETH_TYPE, EthType.IPv4)
			.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
			.setExact(MatchField.IPV4_SRC, from.getIp())
			.setExact(MatchField.IPV4_DST, VOIP_SERVER_IP)
			.setExact(MatchField.UDP_SRC, TransportPort.of((rtcpSrcFrom+1)))
			.setExact(MatchField.UDP_DST, from.getDstRTPTransportPort());
		
		/*
		 * 
		 */
		OFFlowAdd flowToUDP = fluxoUDP(mbTo.build(), myFactory, actionsTo);
		OFFlowAdd flowFromUDP = fluxoUDP(mbFrom.build(), myFactory, actionsFrom);
		
		OFFlowAdd flowToRTCP = fluxoUDP(mbToRTCP.build(), myFactory, actionsToRTCP);
		OFFlowAdd flowFromRTCP = fluxoUDP(mbFromRTCP.build(), myFactory, actionsFromRTCP);
		
		try {
			messageDamper.write(sw, flowToUDP);
			messageDamper.write(sw, flowFromUDP);
			
			messageDamper.write(sw, flowToRTCP);
			messageDamper.write(sw, flowFromRTCP);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processSDPInviteFromClientToVOIPServer(SIPMessage msg, IPv4Address srcIP, TransportPort srcPort, OFPort inPort, MacAddress mac)
	{
		Authority sipUser = ((SipUri) msg.getContactHeader().getAddress().getURI()).getAuthority();
		
		SIPReg reg = new SIPReg();
		if ( sips.containsKey(sipUser.getUser()) )
		{
			reg = sips.get(sipUser.getUser());
		}
		else
		{
			reg.setUser(sipUser.getUser());
			reg.setIp(srcIP);
			reg.setSwitchPort(inPort);
			reg.setSipTransportPort(srcPort);
			reg.setCallTo(msg.getTo().getName());
			reg.setMacAddress(mac);
		}
		
		try
		{
			String sdpContent = new String(msg.getRawContent());
			SessionDescription requestSDP = SdpFactory.getInstance().createSessionDescription(sdpContent);
			MediaDescription md = (MediaDescription) requestSDP.getMediaDescriptions(true).get(0);
			Media media = md.getMedia();
			reg.setSrcRTPTransportPort(TransportPort.of(media.getMediaPort()));
			
			sips.put(sipUser.getUser(), reg);
		} catch (SdpException e) {
			e.printStackTrace();
		}
	}
	
	private void processSDPInviteFromVOIPServerToClient(SIPMessage msg, IPv4Address srcIP, TransportPort srcPort)
	{
		Authority sipUser = ((SipUri) msg.getTo().getAddress().getURI()).getAuthority();
		IDevice dstDevice = findDevice(srcIP);
		
		SIPReg reg = new SIPReg();
		reg.setUser(sipUser.getUser());
		reg.setIp(srcIP);
		
		reg.setSwitchPort(dstDevice.getAttachmentPoints()[0].getPort());
		
		reg.setSipTransportPort(srcPort);
		
		try
		{
			String sdpContent = new String(msg.getRawContent());
			SessionDescription requestSDP = SdpFactory.getInstance().createSessionDescription(sdpContent);
			MediaDescription md = (MediaDescription) requestSDP.getMediaDescriptions(true).get(0);
			Media media = md.getMedia();
			reg.setDstRTPTransportPort(TransportPort.of(media.getMediaPort()));
			
			sips.put(sipUser.getUser(), reg);
		} catch (SdpException e) {
			e.printStackTrace();
		}
	}
	
	private void processSDPInviteOkResponseFromServer(SIPMessage msg, IPv4Address srcIP, TransportPort srcPort, OFPort inPort)
	{
		Authority sipUser = ((SipUri) msg.getFrom().getAddress().getURI()).getAuthority();
		
		SIPReg reg = new SIPReg();
		reg = sips.get(sipUser.getUser());
		try
		{
			String sdpContent = new String(msg.getRawContent());
			SessionDescription requestSDP = SdpFactory.getInstance().createSessionDescription(sdpContent);
			MediaDescription md = (MediaDescription) requestSDP.getMediaDescriptions(true).get(0);
			Media media = md.getMedia();
			reg.setDstRTPTransportPort(TransportPort.of(media.getMediaPort()));
			
			sips.put(sipUser.getUser(), reg);
		} catch (SdpException e) {
			e.printStackTrace();
		}
	}
	
	private SIPReg processSDPInviteOkResponseFromClient(SIPMessage msg, IPv4Address srcIP, TransportPort srcPort, OFPort inPort, MacAddress mac)
	{
		Authority sipUser = ((SipUri) msg.getContactHeader().getAddress().getURI()).getAuthority();
		
		SIPReg reg = new SIPReg();
		reg = sips.get(sipUser.getUser());
		try
		{
			String sdpContent = new String(msg.getRawContent());
			SessionDescription requestSDP = SdpFactory.getInstance().createSessionDescription(sdpContent);
			MediaDescription md = (MediaDescription) requestSDP.getMediaDescriptions(true).get(0);
			Media media = md.getMedia();
			reg.setSrcRTPTransportPort(TransportPort.of(media.getMediaPort()));
			reg.setCallFrom( ((SipUri)msg.getFrom().getAddress().getURI()).getUser() );
			reg.setMacAddress(mac);
			sips.put(sipUser.getUser(), reg);
		} catch (SdpException e) {
			e.printStackTrace();
		}
		return reg;
	}
	
	protected Match createMatchFromPacket(IOFSwitch sw, OFPort inPort,
			IPv4Address src, IPv4Address dst, TransportPort srcP, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());

		Match.Builder mb = sw.getOFFactory().buildMatch();

		if (FLOWMOD_DEFAULT_MATCH_VLAN) {
			if (!vlan.equals(VlanVid.ZERO)) {
				mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
			}
		}

		if (eth.getEtherType() == EthType.IPv4) { /* shallow check for equality is okay for EthType */
			IPv4 ip = (IPv4) eth.getPayload();
			
			if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setExact(MatchField.IPV4_SRC, src)
				.setExact(MatchField.IPV4_DST, dst);
			}

			if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
				if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
					mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				}
				
				if (ip.getProtocol().equals(IpProtocol.UDP)) {
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_SRC, srcP);
				}
			}
		} else if (eth.getEtherType() == EthType.ARP) { /* shallow check for equality is okay for EthType */
			mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
		}
		return mb.build();
	}
	
	protected Match createMatchReverseFromPacket(IOFSwitch sw, OFPort inPort,
			IPv4Address src, IPv4Address dst, TransportPort srcP, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());

		Match.Builder mb = sw.getOFFactory().buildMatch();
		if (FLOWMOD_DEFAULT_MATCH_VLAN) {
			if (!vlan.equals(VlanVid.ZERO)) {
				mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
			}
		}

		if (eth.getEtherType() == EthType.IPv4) { /* shallow check for equality is okay for EthType */
			IPv4 ip = (IPv4) eth.getPayload();
			
			if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setExact(MatchField.IPV4_SRC, src)
				.setExact(MatchField.IPV4_DST, dst);
			}

			if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
				if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
					mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				}
				
				if (ip.getProtocol().equals(IpProtocol.UDP)) {
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_SRC, srcP);
				}
			}
		} else if (eth.getEtherType() == EthType.ARP) { /* shallow check for equality is okay for EthType */
			mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
		}
		return mb.build();
	}

	private OFFlowAdd fluxoUDP(Match match, OFFactory myFactory, List<OFAction> actions) {

		Set<OFFlowModFlags> flags = new HashSet<>();
		flags.add(OFFlowModFlags.SEND_FLOW_REM);

		OFFlowAdd flowToUDP = myFactory.buildFlowAdd().setFlags(flags).setActions(actions)
				.setIdleTimeout(60)
				.setBufferId(OFBufferId.NO_BUFFER).setHardTimeout(hardTimeout)
				.setMatch(match).setCookie(U64.of(1L << 59)).setPriority(1).build();
		return flowToUDP;
	}
	
	private IDevice findDevice(IPv4Address ip)
	{
		IDevice ast = null;
		for (IDevice dev : deviceManagerService.getAllDevices()) {
			if ( dev.getIPv4Addresses().length > 0 && dev.getIPv4Addresses()[0].compareTo(ip) == 0 )
			{
				ast = dev;
				break;
			}
		}
		return ast;
	}

	protected void doFlood(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort()
				: pi.getMatch().get(MatchField.IN_PORT));
		if (topologyService.isIncomingBroadcastAllowed(sw.getId(), inPort) == false) {
			if (log.isTraceEnabled()) {
				log.trace(
						"doFlood, drop broadcast packet, pi={}, "
								+ "from a blocked port, srcSwitch=[{},{}], linkInfo={}",
						new Object[] { pi, sw.getId(), inPort });
			}
			return;
		}

		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		List<OFAction> actions = new ArrayList<OFAction>();
		if (sw.hasAttribute(IOFSwitch.PROP_SUPPORTS_OFPP_FLOOD)) {
			actions.add(sw.getOFFactory().actions().output(OFPort.FLOOD, Integer.MAX_VALUE)); // FLOOD
		} else {
			actions.add(sw.getOFFactory().actions().output(OFPort.ALL, Integer.MAX_VALUE));
		}
		pob.setActions(actions);

		pob.setBufferId(OFBufferId.NO_BUFFER);
		pob.setInPort(inPort);
		pob.setData(pi.getData());

		if (log.isTraceEnabled()) {
			log.trace("Writing flood PacketOut switch={} packet-in={} packet-out={}",
					new Object[] { sw, pi, pob.build() });
		}
		try {
			messageDamper.write(sw, pob.build());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return;
	}

}
