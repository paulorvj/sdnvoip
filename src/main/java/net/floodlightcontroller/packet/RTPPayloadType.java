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

public enum RTPPayloadType {

	/*
	 * RFC3551
	 * https://tools.ietf.org/html/rfc3551#page-32
	 */
	PMCU(0, "PMCU"),
	RESERVED_1(1, "RESERVED_1"),
	RESERVED_2(2, "RESERVED_2"),
	GSM(3, "GSM"),
	G723(4, "G723"),
    DVI4_5(5, "DVI4_5"),
    DVI4_6(6, "DVI4_6"),
    LPC(7, "LPC"),
    PCMA(8, "PCMA"),
    G722(9, "G722"),
    L16_10(10, "L16_10"),
    L16_11(11, "L16_11"),
    QCELP(12, "QCELP"),
    CN(13, "CN"),
    MPA(14, "MPA"),
    G728(15, "G728"),
    DVI4_16(16, "DVI4_16"),
    DVI4_17(17, "DVI4_17"),
    G729(18, "G729"),
    RESERVED_19(19, "RESERVED_19"),
    UNASSIGNED_20(20, "UNASSIGNED_20"),
    UNASSIGNED_21(21, "UNASSIGNED_21"),
    UNASSIGNED_22(22, "UNASSIGNED_22"),
    UNASSIGNED_23(23, "UNASSIGNED_23"),
	RTCP(99999, "RTCP");
	
	protected int type;
	protected String label;
	
	private RTPPayloadType(int type, String label) {
        this.type = type;
        this.label = label;
    }
	
	public static RTPPayloadType getType(int value) {
        switch (value) {
        	case 0:
        		return PMCU;
            case 1:
                return RESERVED_1;
            case 2:
                return RESERVED_2;
            case 3:
                return GSM;
            case 4:
                return G723;
            case 5:
                return DVI4_5;
            case 6:
                return DVI4_6;
            case 7:
                return LPC;
            case 8:
                return PCMA;
            case 9:
                return G722;
            case 10:
                return L16_10;
            case 11:
                return L16_11;
            case 12:
                return QCELP;
            case 13:
                return CN;
            case 14:
                return MPA;
            case 15:
                return G728;
            case 16:
                return DVI4_16;
            case 17:
                return DVI4_17;
            case 18:
                return G729;
            case 19:
                return RESERVED_19;
            case 20:
                return UNASSIGNED_20;
            case 21:
                return UNASSIGNED_21;
            case 22:
                return UNASSIGNED_22;
            case 23:
                return UNASSIGNED_23;
            default:
            	return RTCP;
        }
    }
	
	public String getLabel()
	{
		return this.label;
	}
	
	public int getIntType()
	{
		return this.type;
	}
}
