MessageSight OAuth Sample
=========================
The purpose of this project is to provide an example client application that uses an OAuth 2.0 access token to authenticate with an IBM MessageSight server.

Prerequisites
-------------
+ A MessageSight server
+ An OAuth 2.0 Provider

How it works
------------
OAuth is used to delegate authorization across web applications. An OAuth provider grants access tokens that allow other applications to access some protected resource
without that application needing to provide a username and password. Refer to [OAuth](http://oauth.net/) for more information.

In this application, an HttpsURLRequest is used to retrieve an access token from the OAuth provider. An example of what the request to the OAuth provider
looks like can be seen using the cURL command:
curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" 
-d "grant_type=password&client_id=${your_oauth_client}&client_secret=${your_client_secret}&username=${your_username}&password=${your_password}" 
https://${oauth_provider_url}:${oauth_provider_port}/oauth2/endpoint/${your_oauth_provider}/token

This token can then be used as the password in an MQTT client to
connect to the MessageSight server. This is done by specifying the following on the MQTT Connect Options:
1. Username: "IMA_OAUTH_ACCESS_TOKEN"
2. Password: "{"access_token":"some token", "token_type":"bearer", "expires_in":360, "scope":"", "refresh_token":"some token"}"

Configuring MessageSight to use OAuth
-------------------------------------
In order to authenticate users with OAuth access tokens, some configuration must be done on the MessageSight server.
1. Create a Certificate Profile. In order to create a Certificate Profile, you must specify an SSL certificate and key in PEM format.
2. Create an OAuth Profile. In order to create an OAuth Profile, you must specify at least a resource URL. This is the URL of a protected resource that your OAuth Provider grants access to.
3. Create a Security Profile. In order to create a Security Profile, you must specify a Certificate Profile to use. For this sample, you must also specify the OAuth Profile to use.
4. Create an Endpoint. For this sample, you must create or update an existing endpoint to use the Security Profile created in the previous step.

Configuring the client application
----------------------------------
In order to connect your client application to MessageSight using OAuth, the client must be using a secure connection. At the very least, your client
must specify a truststore that contains a valid CA Path to verify the MessageSight server certificate.
You must also set the properties specific to your OAuth provider that are required to generate an access token.
1. URL for requesting access token.
2. OAuth Client ID and Client Secret.
3. Username and Password used to access the protected resource.
4. MessageSight server endpoint URI.

Running the application
-----------------------
To run the application, use the following command:
java -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
	-Djavax.net.ssl.trustStorePassword=password \
	-cp bin/sample.jar:lib/org.eclipse.paho.client.mqttv3-1.0.1.jar \
	com.ibm.ima.sample.oauth.MessageSightOAuthSample -client_id imaclient -client_secret password -username admin \
	-password password -broker ssl://${broker_ip}:${broker_port} -oauthURI https://${oauth_provider_ip}:${oauth_provider_port}/oauth2/endpoint/MessageSightOAuth/token

Resources
---------
+ [IBM MessageSight](https://developer.ibm.com/messaging/messagesight/)
+ [IBM MessageSight V1.2 Knowledge Center](http://www-01.ibm.com/support/knowledgecenter/SSCGGQ_1.2.0/com.ibm.ism.doc/welcome_page/ic-homepage.html)
+ [OAuth](http://oauth.net/)
+ [IBM WebSphere Application Server OAuth](http://www.ibm.com/developerworks/websphere/techjournal/1305_odonnell2/1305_odonnell2.html)
+ [MQTT](http://mqtt.org/)