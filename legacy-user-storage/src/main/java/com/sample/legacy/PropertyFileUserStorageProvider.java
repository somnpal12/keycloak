package com.sample.legacy;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.*;

public class PropertyFileUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater {
    private static final Logger logger = Logger.getLogger(PropertyFileUserStorageProvider.class);
    protected KeycloakSession session;
    protected Properties properties;
    protected ComponentModel model;
    // map of loaded users in this transaction
    protected Map<String, UserModel> loadedUsers = new HashMap<>();

    public PropertyFileUserStorageProvider(KeycloakSession session, ComponentModel model, Properties properties) {
        this.session = session;
        this.properties = properties;
        this.model = model;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            throw new ReadOnlyException("user is read only for this update");
        }

        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realmModel, UserModel userModel, String s) {

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realmModel, UserModel userModel) {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        //logger.info("supportsCredentialType :  " + credentialType.equals(PasswordCredentialModel.TYPE));
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        String password = properties.getProperty(user.getUsername());
        return credentialType.equals(PasswordCredentialModel.TYPE) && password != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        //logger.info(" password validation : " + input.getType());
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }
        //logger.info("log 1");
        UserCredentialModel cred = (UserCredentialModel) input;
        String password = properties.getProperty(user.getUsername());
        if (password == null) {
            //logger.info("log 2");
            return false;
        }
        //logger.info("log 3 : "+ password.equals(cred.getValue()));
        return password.equals(cred.getValue());
    }

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        //logger.info("get user by id : "  + id);
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        //logger.info("get user by name : "  + username);
        logger.info(">>"+ realm.getRole("app-user").getName());
        UserModel adapter = loadedUsers.get(username);
        if (adapter == null) {
            String password = properties.getProperty(username);
            if (password != null) {

                adapter = createAdapter(realm, username);

                loadedUsers.put(username, adapter);
            }
        }
        return adapter;
    }



    @Override
    public UserModel getUserByEmail(String s, RealmModel realmModel) {
        return null;
    }

    protected UserModel createAdapter(RealmModel realm, String username) {

        return new AbstractUserAdapter(session, realm, model) {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                Set<RoleModel> roleModels = super.getRoleMappings();
                roleModels.add(realm.getRole("app-user"));
                return roleModels;
            }


            /* @Override
            public Set<RoleModel> getClientRoleMappings(ClientModel app) {
                Set<RoleModel> roleModels = super.getClientRoleMappings(app);

                logger.info(app.getRealm().getRole("user"));
                logger.info(app.getRealm().getRole("app-user"));

                roleModels.add(app.getRealm().getRole("user"));
                return roleModels;
            }*/

            /*
            @Override
            public boolean isEmailVerified() {
                return true;
            }*/

        };
    }

}
