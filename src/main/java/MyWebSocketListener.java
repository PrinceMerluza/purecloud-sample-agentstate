package main.java;

import com.mypurecloud.sdk.v2.extensions.notifications.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketState;

public class MyWebSocketListener implements WebSocketListener{

	public void onStateChanged(WebSocketState state) {
		// TODO Auto-generated method stub
		System.out.println("State Changed");
	}

	public void onConnected() {
		// TODO Auto-generated method stub
		System.out.println("Connected to socket");
	}

	public void onDisconnected(boolean closedByServer) {
		// TODO Auto-generated method stub
		System.out.println("Disconnected from socket");
	}

	public void onError(WebSocketException exception) {
		// TODO Auto-generated method stub
		System.out.println("ERROR");
	}

	public void onConnectError(WebSocketException exception) {
		// TODO Auto-generated method stub
		System.out.println("ERROR");
	}

	public void onCallbackError(Throwable exception) {
		// TODO Auto-generated method stub
		System.out.println("ERROR");
	}

	public void onUnhandledEvent(String event) {
		// TODO Auto-generated method stub
		System.out.println(event);
	}

}
