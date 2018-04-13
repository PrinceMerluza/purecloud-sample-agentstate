package main.java;

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
    public static void main(String[] args) throws ApiException, IOException{
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

        // Get the group members and subscribe to presence,routing status topics..
        Group theGroup = getGroup(groupName, groupsApi);
        List<User> users = getGroupMembers(theGroup, groupsApi);

        NotificationHandler notificationHandler = NotificationHandler.Builder.standard()
                .withWebSocketListener(new MyWebSocketListener())
                .withNotificationListener(new ChannelMetadataListener())
                .withAutoConnect(false)
                .build();

        subscribeToUserGroupPresence(users, notificationHandler);
        System.out.println("Listening...");
    }

    /**
     * Subscribe the handler to the users' presence and routing statuses.
     * @param usersList contains the list of users to subscribe to
     * @param handler 	notificationhandler reference
     */
    private static void subscribeToUserGroupPresence(List<User> usersList, NotificationHandler handler) throws ApiException, IOException{
        // Go through list of users and subscribe to each routing status and presence.
        for(User user : usersList) {
            handler.addSubscription(new UserPresenceListener(user.getId(), user.getName()));
            handler.addSubscription(new UserRoutingStatusListener(user.getId(), user.getName()));
            System.out.println("Subscribed to: " + user.getName());
        }
    }

    /**
     * Get members of a group.
     * @param group	PureCloud group to get all members from
     * @param api	GroupsApi for calling api functions
     * @return		list of Users from the group
     */
    private static List<User> getGroupMembers(Group group, GroupsApi api) throws ApiException, IOException{
        List<User> members = new ArrayList<User>();
        int pageSize = 50; //arbitrary number
        int pageCount = (group.getMemberCount().intValue()/pageSize) + 1;

        try {
            // Synchronous calls to get group members and fill up the List
            for(int i = 1;i <= pageCount;i++) {
                UserEntityListing result = api.getGroupMembers(group.getId(), pageSize, i, "ASC", null);
                members.addAll(result.getEntities());
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        Group result = null;

        // Search criteria is group name with exact value.
        GroupSearchCriteria criteria = new GroupSearchCriteria();
        criteria.setValue(name);
        criteria.setOperator(GroupSearchCriteria.OperatorEnum.AND);
        criteria.setFields(new ArrayList<String>(Arrays.asList(new String[] {"name", "id"})));
        criteria.setType(TypeEnum.EXACT);

        // Build query
        List<GroupSearchCriteria> query = new ArrayList<GroupSearchCriteria>();
        query.add(criteria);

        GroupSearchRequest request = new GroupSearchRequest();
        request.setQuery(query);

        try {
            // Call Groups api for result. Should only be 1.
            GroupsSearchResponse response = api.postGroupsSearch(request);
            if(response.getTotal() > 1) {
                System.out.println("Warning: More than 1 group was found. Getting first result.");
            }
            result = response.getResults().get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     *	Request client credentials token from PureCloud
     * @param clientId 		OAuth clientid
     * @param clientSecret  OAuth client secret
     * @return String		access token
     */
    private static String getToken(String clientId, String clientSecret)throws ApiException, IOException {
        String token = "";

        // Token Request info + encoded client credentials
        String url = "https://login.mypurecloud.com/oauth/token";
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder()
                                    .encodeToString(credentials.getBytes());

        //System.out.println(encodedCredentials);
        try {
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

            // HTTP Response with token
            if(connection.getResponseCode() == 200) {
                InputStream response = connection.getInputStream();
                String responseString =  Helper.convertStreamToString(response);
                // Hacky-way of extracting token from response string
                // so we don't have to add external library for JSON parsing
                token = responseString.substring(responseString.indexOf(':')+2,
                        responseString.indexOf(',')-1).trim();
            }else {
                System.out.println("Token not acquired.");
            }

        } catch (Exception e) {
            System.out.println("Error with HTTP connection");;
            e.printStackTrace();
        }

        return token;
    }
}