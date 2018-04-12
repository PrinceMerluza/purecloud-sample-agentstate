package main.java;

import com.mypurecloud.sdk.v2.extensions.notifications.ChannelMetadataNotification;
import com.mypurecloud.sdk.v2.extensions.notifications.NotificationEvent;
import com.mypurecloud.sdk.v2.extensions.notifications.NotificationListener;

public class ChannelMetadataListener implements NotificationListener<ChannelMetadataNotification> {
    public String getTopic() {
        return "channel.metadata";
    }

    public Class<?> getEventBodyClass() {
        return ChannelMetadataNotification.class;
    }
    
    public void onEvent(NotificationEvent<?> notificationEvent) {
        System.out.println("[channel.metadata] " + ((ChannelMetadataNotification)notificationEvent.getEventBody()).getMessage());
    }
}