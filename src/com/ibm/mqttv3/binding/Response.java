package com.ibm.mqttv3.binding;

import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Response {

	private float responseCode;
	private String payload = "";
	
	public Response(ResponseCode responseCode) {
		this.responseCode = responseCode.value;
	}

	public Response(MqttMessage message) {
		String msg = null;
		try {
			msg = new String(message.getPayload(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] data = msg.split(" ", 2);
		this.payload = data[1];
		this.responseCode = Float.valueOf(data[0]);
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public void setPayload(byte[] payload) {
		try {
			this.payload = new String(payload, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String getMessage() {
		return responseCode + " " + payload;
	}

	public float getCode() {
		return responseCode;
	}
	
	public byte[] getPayload() {
		return this.payload.getBytes();
	}
	
	public String getPayloadText() {
		return this.payload;
	}

}
