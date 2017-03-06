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
package net.floodlightcontroller.packet.sip;

public enum SIPResponseCode {

	/*
	 * RFC3261 SEC 21
	 */
	// CLASS 1xx PROVISIONAL
	TRYING(100),
	RINGING(180),
	FORWARDED(181),
	QUEUED(182),
	SESSION_PROGRESS(183),
	
	// CLASS 2xx SUCCESS
	OK(200),
	
	// CLASS 3xx REDIRECTION
	MULTIPLE_CHOICES(300),
	MOVED_PERMANENTLY(301),
	MOVED_TEMPORARILY(302),
	USE_PROXY(305),
	ALTERNATIVE_SERVICE(380),
	
	// CLASS 4xx REQUEST FAILURE
	BAD_REQUEST(400),
	UNAUTHORIZED(401),
	PAYMENT_REQUIRED(402),
	FORBIDDEN(403),
	NOT_FOUND(404),
	METHOD_NOT_ALLOWED(405),
	NOT_ACCEPTABLE_406(406),
	PROXY_AUTHENTICATION_REQUIRED(407),
	REQUEST_TIMEOUT(408),
	GONE(410),
	REQUEST_ENTITY_TOO_LARGE(413),
	REQUEST_URI_TOO_LONG(414),
	UNSUPPORTED_MEDIA_TYPE(415),
	UNSUPPORTED_URI_SCHEME(416),
	BAD_EXTENSION(420),
	
	EXTENSION_REQUIRED(421),
	INTERVAL_TOO_BRIEF(423),
	TEMPORARILY_UNAVAILABLE(480),
	CALL_OR_TRANSACTION_DOES_NOT_EXIST(481),
	LOOP_DETECTED(482),
	TOO_MANY_HOPS(483),
	ADDRESS_INCOMPLETE(484),
	AMBIGUOUS(485),
	BUSY_HERE(486),
	REQUEST_TERMINATED(487),
	NOT_ACCEPTABLE_HERE(488),
	REQUEST_PENDING(491),
	UNDECIPHERABLE(493),

	//CLASS 5XX SERVER FAILURE
	SERVER_INTERNAL_ERROR(500),
	NOT_IMPLEMENTED(501),
	BAD_GATEWAY(502),
	SERVICE_UNAVAILABLE(503),
	SERVER_TIME_OUT(504),
	VERSION_NOT_SUPPORTED(505),
	MESSAGE_TOO_LARGE(513),

	//CLASS 6XX GLOBAL FAILURES
	BUSY_EVERYWHERE(600),
	DECLINE(603),
	DOES_NOT_EXIST_ANYWHERE(604),
	NOT_ACCEPTABLE_606(606);
	
	private int value;
	
	private SIPResponseCode(int value) {
		this.value = value;
	}
	
	public static SIPResponseCode getResponse(int value) {
        switch (value) {
	        case 100:
	        	return TRYING;
	        case 180:
	        	return RINGING;
	        case 181:
	        	return FORWARDED;
	        case 182:
	        	return QUEUED;
	        case 183:
	        	return SESSION_PROGRESS;
	        case 200:
	        	return OK;
	        case 300:
	        	return MULTIPLE_CHOICES;
	        case 301:
	        	return MOVED_PERMANENTLY;
	        case 302:
	        	return MOVED_TEMPORARILY;
	        case 305:
	        	return USE_PROXY;
	        case 380:
	        	return ALTERNATIVE_SERVICE;
	        case 400:
	        	return BAD_REQUEST;
	        case 401:
	        	return UNAUTHORIZED;
	        case 402:
	        	return PAYMENT_REQUIRED;
	        case 403:
	        	return FORBIDDEN;
	        case 404:
	        	return NOT_FOUND;
	        case 405:
	        	return METHOD_NOT_ALLOWED;
	        case 406:
	        	return NOT_ACCEPTABLE_406;
	        case 407:
	        	return PROXY_AUTHENTICATION_REQUIRED;
	        case 408:
	        	return REQUEST_TIMEOUT;
	        case 410:
	        	return GONE;
	        case 413:
	        	return REQUEST_ENTITY_TOO_LARGE;
	        case 414:
	        	return REQUEST_URI_TOO_LONG;
	        case 415:
	        	return UNSUPPORTED_MEDIA_TYPE;
	        case 416:
	        	return UNSUPPORTED_URI_SCHEME;
	        case 420:
	        	return BAD_EXTENSION;
	        case 421:
	        	return EXTENSION_REQUIRED;
	        case 423:
	        	return INTERVAL_TOO_BRIEF;
	        case 480:
	        	return TEMPORARILY_UNAVAILABLE;
	        case 481:
	        	return CALL_OR_TRANSACTION_DOES_NOT_EXIST;
	        case 482:
	        	return LOOP_DETECTED;
	        case 483:
	        	return TOO_MANY_HOPS;
	        case 484:
	        	return ADDRESS_INCOMPLETE;
	        case 485:
	        	return AMBIGUOUS;
	        case 486:
	        	return BUSY_HERE;
	        case 487:
	        	return REQUEST_TERMINATED;
	        case 488:
	        	return NOT_ACCEPTABLE_HERE;
	        case 491:
	        	return REQUEST_PENDING;
	        case 493:
	        	return UNDECIPHERABLE;
	        case 500:
	        	return SERVER_INTERNAL_ERROR;
	        case 501:
	        	return NOT_IMPLEMENTED;
	        case 502:
	        	return BAD_GATEWAY;
	        case 503:
	        	return SERVICE_UNAVAILABLE;
	        case 504:
	        	return SERVER_TIME_OUT;
	        case 505:
	        	return VERSION_NOT_SUPPORTED;
	        case 513:
	        	return MESSAGE_TOO_LARGE;
	        case 600:
	        	return BUSY_EVERYWHERE;
	        case 603:
	        	return DECLINE;
	        case 604:
	        	return DOES_NOT_EXIST_ANYWHERE;
	        case 606:
	        	return NOT_ACCEPTABLE_606;
        }
        return null;
	}
	
	public int getValue()
	{
		return this.value;
	}
}
