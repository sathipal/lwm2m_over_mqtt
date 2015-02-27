package com.ibm.mqttv3.binding;

public abstract class AbstractRequestObserver {
    protected Request mqttRequest;

    public AbstractRequestObserver(final Request mqttRequest) {
        this.mqttRequest = mqttRequest;
    }

    public abstract void onResponse(final Response mqttResponse);
    public abstract void onError(final Response mqttResponse);

	public void onCancel() {
		// TODO Auto-generated method stub
		
	}
}
