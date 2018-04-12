package main.java;

import com.mypurecloud.sdk.v2.extensions.notifications.NotificationEvent;
import com.mypurecloud.sdk.v2.extensions.notifications.NotificationListener;
import com.mypurecloud.sdk.v2.model.UserRoutingStatusNotification;

public class UserRoutingStatusListener implements NotificationListener<UserRoutingStatusNotification>{
    private String topic;
    private String userName;

    public Class<UserRoutingStatusNotification> getEventBodyClass() {
        return UserRoutingStatusNotification.class;
    }

    public String getTopic() {
        return topic;
    }

    public void onEvent(NotificationEvent<?> event) {
        String routingStatus = ((UserRoutingStatusNotification) event.getEventBody()).getRoutingStatus().getStatus().name();
        System.out.println("User: " + userName + "\t Routing Status: " + routingStatus);
    }

    public UserRoutingStatusListener(String userId, String userName) {
        this.userName = userName;
        this.topic = "v2.users." + userId + ".routingStatus";
    }

}
