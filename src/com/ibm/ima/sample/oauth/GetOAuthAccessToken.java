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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Retrieve an OAuth 2 AccessToken from the MessageSightOAuth OAuth provider that we have configured in WAS. The
 * provider in WAS will have OAuth clients defined. clientId and clientSecret are used to specify one of the OAuth
 * clients. username and password are used to specify a WAS user.
 * 
 * Ex. I have OAuth client "imaclient" and make the token request as WAS user "admin".
 * 
 * @author Mike Robertson
 * 
 */
public class GetOAuthAccessToken {
    private final String charset = "UTF-8";
    private String accessToken;
    private String _uri;
    private String _clientId;
    private String _clientSecret;
    private String _username;
    private String _password;
    private String tokenEndPoint;

    public GetOAuthAccessToken(String clientId, String clientSecret, String uri, String username, String password) {
    	_uri = uri;
        _clientId = clientId;
        _clientSecret = clientSecret;
        _username = username;
        _password = password;
    }
    
    static {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                // Don't verify the oauth providers certificate
                return true;
            }
        });
    }

    public String getOAuthToken() {
        try {
            //tokenEndPoint = "https://" + _uri + "/oauth2/endpoint/MessageSightOAuth/token";
            tokenEndPoint = _uri;
            String grant_type = "password";
            String query = String.format("grant_type=%s&client_id=%s&client_secret=%s&username=%s&password=%s",
                            URLEncoder.encode(grant_type, charset),
                            URLEncoder.encode(_clientId, charset), URLEncoder.encode(_clientSecret, charset),
                            URLEncoder.encode(_username, charset), URLEncoder.encode(_password, charset));
            
            // send Resource Request using (accessToken);
            sendRequestForAccessToken(query);
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
        return accessToken;
    }

    private void sendRequestForAccessToken(String query) throws IOException {
        URL url = new URL(tokenEndPoint);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        OutputStream output = null;
        try {
            output = con.getOutputStream();
            output.write(query.getBytes(charset));
            output.flush();
        } finally {
            if (output != null)
                try {
                    output.close();
                } catch (IOException logOrIgnore) {
                }
        }
        con.connect();
        // read the output from the server
        BufferedReader reader = null;
        StringBuilder stringBuilder;
        reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        stringBuilder = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException logOrIgnore) {
                }
        }
        String tokenResponse = stringBuilder.toString();
        accessToken = tokenResponse;
        System.out.println("Retrieved access token: " + accessToken);
    }
}