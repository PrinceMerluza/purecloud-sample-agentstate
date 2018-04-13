package main.java;

import com.neovisionaries.ws.client.WebSocketException;
import main.utils.*;

import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.Configuration;
import com.mypurecloud.sdk.v2.api.GroupsApi;
import com.mypurecloud.sdk.v2.extensions.notifications.NotificationHandler;
import com.mypurecloud.sdk.v2.model.*;
import com.mypurecloud.sdk.v2.model.GroupSearchCriteria.TypeEnum;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws ApiException, IOException, WebSocketException {
        //OAuth Input
        String clientId = "unknown";
        String clientSecret = "unknown";

        //Group name to get members from
        String groupName = "";

        // Configure SDK settings
        String accessToken = getToken(clientId, clientSecret);
        Configuration.setDefaultApiClient(ApiClient.Builder.standard()
                .withAccessToken(accessToken)
                .withBasePath("https://api.mypurecloud.com")
                .build());

        // Instantiate APIs
        GroupsApi groupsApi = new GroupsApi();

        // Get the group using the group name
        Group theGroup = getGroup(groupName, groupsApi);

        // Get the members of the group
        List<User> users = getGroupMembers(theGroup, groupsApi);

        // Build the notification handler
        NotificationHandler notificationHandler = NotificationHandler.Builder.standard()
                .withWebSocketListener(new MyWebSocketListener())
                .withNotificationListener(new ChannelMetadataListener())
                .withAutoConnect(false)
                .build();

        // Subscribe the notification handler to the presence of group members
        subscribeToUserGroupPresence(users, notificationHandler);

        // Display to confirm websocket client is listening
        System.out.println("Listening...");
    }

    /**
     * Subscribe the notification handler to the users' presence and routing statuses.
     * @param usersList contains the list of users to subscribe to
     * @param handler 	notificationhandler reference
     */
    private static void subscribeToUserGroupPresence(List<User> usersList, NotificationHandler handler) throws ApiException, IOException{
        // Go through list of users and subscribe to each routing status and presence.
        for(User user : usersList) {
            handler.addSubscription(new UserPresenceListener(user.getId(), user.getName()));
            handler.addSubscription(new UserRoutingStatusListener(user.getId(), user.getName()));
        }
    }

    /**
     * Get members of a group.
     * @param group	PureCloud group to get all members from
     * @param api	GroupsApi for calling api functions
     * @return		List of Users from the group
     */
    private static List<User> getGroupMembers(Group group, GroupsApi api) throws ApiException, IOException{
        // The list that will contain the group members
        List<User> members = new ArrayList<User>();

        // Get the number of pages of the group and loop through all them to get all members
        for(int i = 1;i <= (group.getMemberCount().intValue()/25) + 1;i++) {
            // API Call to get current page and members
            UserEntityListing result = api.getGroupMembers(group.getId(),25, i, "ASC", null);

            // Add the members of the page to the List
            members.addAll(result.getEntities());
        }

        return members;
    }

    /**
     *	Search and Get a PureCloud group using its name or id.
     * @param name	search query value. Could be a group name or group id.
     * @param api	GroupsApi
     * @return		First PureCloud Group that is found.
     */
    private static Group getGroup(String name, GroupsApi api) throws ApiException, IOException{
        // Search criteria is group name with exact value.
        GroupSearchCriteria criteria = new GroupSearchCriteria()
                                           .value(name)
                                           .operator(GroupSearchCriteria.OperatorEnum.AND)
                                           .fields(new ArrayList<>(Arrays.asList("name")))
                                           .type(TypeEnum.EXACT);

        // Build list of Group Search Criteria
        GroupSearchRequest request = new GroupSearchRequest().query(new ArrayList<>(Arrays.asList(criteria)));

        // Return the group that was found. Should only be 1, if not, get the first one.
        return api.postGroupsSearch(request).getResults().get(0);
    }

    /**
     *	Request client credentials token from PureCloud
     * @param clientId 		OAuth clientid
     * @param clientSecret  OAuth client secret
     * @return String		access token
     */
    private static String getToken(String clientId, String clientSecret)throws IOException {
        String token = "";

        // Token Request info + encoded client credentials
        String url = "https://login.mypurecloud.com/oauth/token";
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        // Build HTTP Request Information
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
        connection.setDoOutput(true);

        // HTTP Request Body
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes("grant_type=client_credentials");
        wr.close();

        // Check if HTTP Response is successful
        if(connection.getResponseCode() == 200) {
            InputStream response = connection.getInputStream();
            String responseString =  Helper.convertStreamToString(response);

            // Hacky-way of extracting token from response string
            // so we don't have to add external library for JSON parsing
            token = responseString.substring(responseString.indexOf(':')+2,
                    responseString.indexOf(',')-1).trim();
        }

        return token;
    }
}