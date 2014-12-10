/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *******************************************************************************/
package com.ibm.ima.sample.oauth;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * IBM WebSphere Application Server setup for OAuth:
 * For an overview of configuring WebSphere Application Server to be an OAuth Provider, refer to:
 *  http://www.ibm.com/developerworks/websphere/techjournal/1305_odonnell1/1305_odonnell1.html
 *  
 * In this example, we created an OAuth provider named MessageSightOAuth with 1 client. 
 * OAuth Client: 
 * 	id="imaclient" 
 * 	client_secret="password" 
 * 	displayname="imaclient" 
 * 	redirect="https://[oauth_provider_url]:[oauth_provider_port]/snoop"
 * 
 * The following is the contents of base.clients.xml for the OAuth provider configured.
 * ---------------------------------------
 *	<?xml version="1.0" encoding="UTF-8"?>
 *	<OAuthClientConfiguration>
 *		<client id="imaclient" component="MessageSightOAuth" secret="password" displayname="imaclient" redirect="https://[oauth_provider_url]:[oauth_provider_port]/snoop" enabled="true">
 *		</client>
 *	<OAuthClientConfiguration>
 * ---------------------------------------
 * 
 * To verify that OAuth is configured correctly, the WAS default application is used.
 * 	When navigating to https://[oauth_provider_url]:[oauth_provider_port]/snoop, the page will require login.
 * Or, attempt to generate an access token with the following cURL command:
 * 	curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" 
 * 	-d "grant_type=password&client_id=imaclient&client_secret=password&username=admin&password=admin" 
 * 	https://[oauth_provider_url]:[oauth_provider_port]/oauth2/endpoint/MessageSightOAuth/token
 * 
 * -----------------------------
 * MessageSight setup for OAuth:
 * 1. Set up a certificate profile on MessageSight so that the server has a certificate to present for SSL Connections.
 * file get scp://[host]:/path/to/server_certificate server-crt.pem
 * file get scp://[host]:/path/to/server_key server-key.pem
 * imaserver apply Certificate CertFileName=server-crt.pem KeyFileName=server-key.pem
 * imaserver create CertificateProfile Name=certprof Certificate=server-crt.pem Key=server-key.pem 
 * 
 * 2. Set up an OAuth profile to point to your OAuth Provider for authenticating the users access token.
 * imaserver create OAuthProfile Name=OAuthProfile ResourceURL=https://[oauth_provider_url]:[oauth_provider_port]/snoop AuthKey=access_token
 * 
 * 3. Create a security profile using the certificate and OAuth profiles created in the previous steps.
 * imaserver create SecurityProfile Name=OAuthSecProf MinimumProtocolMethod=TLSv1 UsePasswordAuthentication=True CertificateProfile=certprof OAuthProfile=OAuthProfile
 * 
 * 4. Finally, update DemoMqttEndpoint to use the security profile that was created.
 * imaserver update Endpoint Name=DemoMqttEndpoint Enabled=True SecurityProfile=OAuthSecProf
 * 
 * -----------------------
 * Client setup for OAuth:
 * This example uses the Eclipse Paho Java MQTT Client available at: http://repo.eclipse.org/content/repositories/paho-releases/org/eclipse/paho/org.eclipse.paho.client.mqttv3/1.0.1/org.eclipse.paho.client.mqttv3-1.0.1.jar
 * To connect to the secure endpoint created above, you must specify your keystore and truststore that match your server certificate used on MessageSight.
 *  
 * java -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
 *  -Djavax.net.ssl.trustStorePassword=password \
 *  -cp bin/sample.jar:lib/org.eclipse.paho.client.mqttv3-1.0.1.jar \
 *  com.ibm.ima.sample.oauth.MessageSightOAuthSample -client_id imaclient -client_secret password -username admin \
 *  -password password -broker ssl://${broker_ip}:${broker_port} -oauthURI https://${oauth_provider_ip}:${oauth_provider_port}/oauth2/endpoint/MessageSightOAuth/token
 * 
 * org.eclipse.paho.client.mqttv3-1.0.1.jar is the Eclipse Paho Java MQTT Client.
 * 
 * If successful, the client should connect successfully and then disconnect.
 * Example output:
 * 	Access token: {"access_token":"2XoX3lcUyc7jBSTCaROKGfOE8g7JqNYpAo5dEsFo","token_type":"bearer","expires_in":3599,"scope":"","refresh_token":"uG1SuudvoPmTcbe7kJq3a6Htox9CZ4864UvujdrlGfU1TmKn4q"}

 *	Connecting to broker: ssl://[messagesight_endpoint_url]
 *	Connected
 *	Disconnected
 * 
 * @author Mike Robertson
 *
 */
public class MessageSightOAuthSample {
	public static String broker;
	public static String uri;
	public static String clientId;
	public static String clientSecret;
	public static String username;
	public static String password;
	public static String mqttClientID;
    
    public static void main(String[] args) {
        /*String broker       = "ssl://[messagesight_endpoint_url]:[messagesight_endpoint_port]"; 
        String uri          = "[oauth_provider_url]:[oauth_provider_port]";
        String clientId     = "imaclient";
        String clientSecret = "password";
        String username     = "admin";
        String password     = "admin";*/
    	mqttClientID = "OAuthMqttSample";
        MemoryPersistence persistence = new MemoryPersistence();

        parseArgs(args);
        
        try {
		    // Retrieve the access token
		    GetOAuthAccessToken token = new GetOAuthAccessToken(clientId, clientSecret, uri, username, password);
		    String oauthToken = token.getOAuthToken();
        	
        	// Create the Mqtt Client
            MqttClient sampleClient = new MqttClient(broker, mqttClientID, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName("IMA_OAUTH_ACCESS_TOKEN");
            connOpts.setPassword(oauthToken.toCharArray());
            
            // Connect to MessageSight with username IMA_OAUTH_ACCESS_TOKEN and password is the full JSON access token.
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            
            // Disconnect from the server
            sampleClient.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch(MqttException me) {
            me.printStackTrace();
        }
    }
    
    public static void parseArgs(String[] args) {
    	if (args.length < 12) {
    		showUsage();
    	}
    	for (int i = 0; i < args.length; i++) {
    		if ("-client_id".equals(args[i]) && (i + 1 < args.length)) {
    			i++;
    			clientId = args[i];
    		} else if ("-client_secret".equals(args[i]) && (i + 1 < args.length)) {
    			i++;
    			clientSecret = args[i];
    		} else if ("-username".equals(args[i]) && (i + 1 < args.length)) {
    			i++;
    			username = args[i];
    		} else if ("-password".equals(args[i]) && (i + 1 < args.length)) {
    			i++;
    			password = args[i];
    		} else if ("-broker".equals(args[i]) && (i + 1 < args.length)) {
    			i++;
    			broker = args[i];
    		} else if ("-oauthURI".equals(args[i]) && (i + 1 < args.length)) {
    			i++;
    			uri = args[i];
    		} else {
    			showUsage();
    		}
    	}
    }
    
    public static void showUsage() {
    	System.err.println("usage:  "
                + "-client_id <oauth client> -client_secret <client secret> -username <username> -password <password> -broker <broker uri> -oauthURI <oauth uri>");
		System.err.println();
		System.err.println(" -client_id The OAuth clientId to use when requesting an access token.");
		System.err.println(" -client_secret The OAuth client secret to use when requesting an access token.");
		System.err.println(" -username The username to use when requesting an access token.");
		System.err.println(" -password The password to use when requesting an access token.");
		System.err.println(" -broker The URI of the MessageSight Endpoint to connect to.");
		System.err.println(" -oauthURI The URI for the OAuth Provider access token request.");
		System.exit(0);
    }
}