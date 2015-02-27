package com.ibm.lwm2m.objects;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Resource;


public class LwM2MExampleDeviceObject extends MQTTResource {
	
	public static final String RESOURCE_NAME = "3";
	
	private boolean bInstance = false;
	
	// Create an object model
    private StringResource manufacturerResource;
    private StringResource modelResource;
    private StringResource serialNumberResource;
    private StringResource firmwareResource;
    private ExecResource rebootResource;
    private ExecResource factoryResetResource;
    private IntegerMultipleResource powerAvailablePowerResource;
    private IntegerMultipleResource powerSourceVoltageResource;
    private IntegerMultipleResource powerSourceCurrentResource;
    private IntegerResource batteryLevelResource;
    private MemoryFreeResource memoryFreeResource;
    private IntegerMultipleResource errorCodeResource;
    private ExecResource resetErrorCodeResource;
    private TimeResource currentTimeResource;
    private StringResource utcOffsetResource; 
    private StringResource timezoneResource;
    private StringResource bindingsResource;
    
	public LwM2MExampleDeviceObject(String name, boolean bInstance) {
		super(name);
		this.bInstance = bInstance;
		
		/* Create resources only if its a instance */
		if(bInstance == true) {
			manufacturerResource = new StringResource("0", false, "MQTT Example Device");
		    modelResource = new StringResource("1", false, "Model Test");
		    serialNumberResource = new StringResource("2", false, getMACAddress());
		    firmwareResource = new StringResource("3", false, "1.0.0");
		    rebootResource = new ExecResource("4");
		    factoryResetResource = new ExecResource("5");
		    powerAvailablePowerResource = new IntegerMultipleResource("6", false, new int[] { 0, 4 });
		    powerSourceVoltageResource = new IntegerMultipleResource("7", false, new int[] { 12000,
		    		5000 });
		    powerSourceCurrentResource = new IntegerMultipleResource("8", false, new int[] { 150, 75 });
		    batteryLevelResource = new IntegerResource("9", false, 92);
		    memoryFreeResource = new MemoryFreeResource("10", false);
		    errorCodeResource = new IntegerMultipleResource("11", false, new int[] { 0 });
		    resetErrorCodeResource = new ExecResource("12");
		    currentTimeResource = new TimeResource("13", true);
		    utcOffsetResource = new StringResource("14", true, 
		    		new SimpleDateFormat("X").format(Calendar.getInstance().getTime()));
		    timezoneResource = new StringResource("15", true, TimeZone.getDefault().getID());
		    bindingsResource = new StringResource("16", false, "MQTT");
		    
		}
	}

    
	private static String getMACAddress() {
		InetAddress ip;
	
		try {
			ip = InetAddress.getLocalHost();
			System.out.println("Current IP address : " + ip.getHostAddress());
			
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
		 
			byte[] mac = network.getHardwareAddress();
			
			System.out.print("Current MAC address : ");
		 
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
			}
			
			return sb.toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e){
			e.printStackTrace();
		}
		return null;
		 
	}
    	
	private class TimeResource extends LongResource {
		
		public TimeResource(String name, boolean bWrite) {
			super(name, bWrite, new Date().getTime());
		}
		
	    @Override
	    public String getValue() {
			return Long.toString(new Date().getTime());
		}
	}
	
	private class MemoryFreeResource extends IntegerResource {
		
		public MemoryFreeResource(String name, boolean bWrite) {
			super(name, bWrite);
		}
	    
		@Override
		public String getValue() {
			return Long.toString(Runtime.getRuntime().freeMemory());
		}
	}
	
	public static LwM2MExampleDeviceObject createObject() {
		LwM2MExampleDeviceObject to = new LwM2MExampleDeviceObject(RESOURCE_NAME, false);
		LwM2MClient.getRootResource().add(to);
		return to;
	}
	
	public static LwM2MExampleDeviceObject createObjectInstance() {
		LwM2MExampleDeviceObject to = new LwM2MExampleDeviceObject("0", true);
		
		Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
		resource.add(to);
		to.add(to.manufacturerResource);
		to.add(to.modelResource);
		to.add(to.serialNumberResource);
		to.add(to.firmwareResource);
		to.add(to.rebootResource);
		to.add(to.factoryResetResource);
		to.add(to.powerAvailablePowerResource);
		to.add(to.powerSourceCurrentResource);
		to.add(to.powerSourceVoltageResource);
		to.add(to.batteryLevelResource);
		to.add(to.memoryFreeResource);
		to.add(to.errorCodeResource);
		to.add(to.resetErrorCodeResource);
		to.add(to.currentTimeResource);
		to.add(to.utcOffsetResource);
		to.add(to.timezoneResource);
		to.add(to.bindingsResource);
		return to;
	}

}