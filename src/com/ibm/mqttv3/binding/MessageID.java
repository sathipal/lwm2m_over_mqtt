package com.ibm.mqttv3.binding;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageID {
	private static AtomicLong value = new AtomicLong();
	private static Lock roundOffLock = new ReentrantLock();
	
	public static long get() {
		long val = value.getAndIncrement();
		
		if(val < 0) {
			try {
				roundOffLock.lock();
				if((val = value.getAndIncrement()) < 0) {
					value.set(0);
					val = value.getAndIncrement();
				}
				
			} finally {
				roundOffLock.unlock();
			}
		}
		return val;
	}
	

}
