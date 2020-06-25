package com.sample.legacy;


import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

import java.io.*;
import java.util.Properties;

public class PropertyFileUserStorageProviderFactory implements UserStorageProviderFactory<PropertyFileUserStorageProvider> {
    private static final Logger logger = Logger.getLogger(PropertyFileUserStorageProviderFactory.class);

    public static final String PROVIDER_NAME = "readonly-property-file";
    protected Properties properties = new Properties();

    @Override
    public void init(Config.Scope config) {
        //InputStream is = getClass().getClassLoader().getResourceAsStream("/users.properties");
        InputStream is = null;
        try {
            is = new FileInputStream(new File("d://user.properties"));
            logger.info("loading user .........................");
            properties.load(is);
            logger.info(properties.keySet());
        } catch (IOException e) {
            e.printStackTrace();
        }
    /*    if (is == null) {
            logger.warn(">>>>>>>>>>>>>>>>>> Could not find users.properties in classpath");
        } else {
            try {
                logger.info("loading user .........................");
                properties.load(is);
                logger.info(properties.keySet());
            } catch (IOException ex) {
                logger.error("Failed to load users.properties file", ex);
            }
        }*/
    }

    @Override
    public PropertyFileUserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new PropertyFileUserStorageProvider(keycloakSession, componentModel, properties);
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
