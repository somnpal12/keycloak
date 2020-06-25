package com.sample.legacy.jpa;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

import javax.naming.InitialContext;

public class EjbUserStorageProviderFactory  implements UserStorageProviderFactory<EjbUserStorageProvider> {
    private static final Logger logger = Logger.getLogger(EjbUserStorageProviderFactory.class);

    @Override
    public EjbUserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        try {
            InitialContext ctx = new InitialContext();
            EjbUserStorageProvider provider = (EjbUserStorageProvider)ctx.lookup("java:global/user-storage-jpa-example/" + EjbUserStorageProvider.class.getSimpleName());
            provider.setModel(componentModel);
            provider.setSession(keycloakSession);
            return provider;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return "example-user-storage-jpa";
    }

    @Override
    public String getHelpText() {
        return "JPA Example User Storage Provider";
    }

    @Override
    public void close() {
        logger.info("<<<<<< Closing factory");

    }
}
