package com.sample.springkeyclock.controller;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class KeyclockController {


    Keycloak keycloak;


    @GetMapping("/connect")
    public String connectKeyClock() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8180/auth")
                .realm("demo")
                .username("testuser")
                .password("welcome1")
                .clientId("login-app")
                .resteasyClient(
                        new ResteasyClientBuilder()
                                .connectionPoolSize(10).build()
                ).build();

        AccessTokenResponse accessTokenResponse = keycloak.tokenManager().getAccessToken();


        //keycloak.tokenManager().invalidate();

        List<UserRepresentation> list=keycloak.realm("demo").users().list();
        list.forEach( e-> System.out.println(e.getFirstName()));

        System.out.println(keycloak.tokenManager().getAccessTokenString());

        return "success";
    }
}
