package com.sample.legacy.jpa;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.*;

@Stateful
@Local(EjbUserStorageProvider.class)
public class EjbUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache {

    private static final Logger logger = Logger.getLogger(EjbUserStorageProvider.class);
    public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

    @PersistenceContext
    protected EntityManager em;

    protected ComponentModel model;
    protected KeycloakSession session;

    public void setModel(ComponentModel model) {
        this.model = model;
    }

    public void setSession(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }


    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        UserCredentialModel cred = (UserCredentialModel) input;
        String password = getPassword(user);
        return password != null && password.equals(cred.getValue());
    }

    public String getPassword(UserModel user) {
        String password = null;
        if (user instanceof CachedUserModel) {
            password = (String) ((CachedUserModel) user).getCachedWith().get(PASSWORD_CACHE_KEY);
        } else if (user instanceof UserAdapter) {
            password = ((UserAdapter) user).getPassword();
        }
        return password;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel userModel, CredentialInput credentialInput) {
        if (!supportsCredentialType(credentialInput.getType()) || !(credentialInput instanceof UserCredentialModel))
            return false;
        UserCredentialModel cred = (UserCredentialModel) credentialInput;
        UserAdapter adapter = getUserAdapter(userModel);
        adapter.setPassword(cred.getValue());

        return true;
    }


    public UserAdapter getUserAdapter(UserModel user) {
        UserAdapter adapter = null;
        if (user instanceof CachedUserModel) {
            adapter = (UserAdapter) ((CachedUserModel) user).getDelegateForUpdate();
        } else {
            adapter = (UserAdapter) user;
        }
        return adapter;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel userModel, String credentialType) {
        if (!supportsCredentialType(credentialType)) return;

        getUserAdapter(userModel).setPassword(null);
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel userModel) {
        if (getUserAdapter(userModel).getPassword() != null) {
            Set<String> set = new HashSet<>();
            set.add(PasswordCredentialModel.TYPE);
            return set;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel cachedUser, UserModel delegate) {
        String password = ((UserAdapter) delegate).getPassword();
        if (password != null) {
            cachedUser.getCachedWith().put(PASSWORD_CACHE_KEY, password);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserById(String s, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        logger.info("getUserByUsername: " + username);
        TypedQuery<User> query = em.createNamedQuery("getUserByUsername", User.class);
        query.setParameter("username", username);
        List<User> result = query.getResultList();
        if (result.isEmpty()) {
            logger.info("could not find username: " + username);
            return null;
        }

        return new UserAdapter(session, realm, model, result.get(0));
    }

    @Override
    public UserModel getUserByEmail(String s, RealmModel realm) {
        return null;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        Object count = em.createNamedQuery("getUserCount")
                .getSingleResult();
        return ((Number) count).intValue();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, -1, -1);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {

        TypedQuery<User> query = em.createNamedQuery("getAllUsers", User.class);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<User> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (User entity : results) {
            users.add(new UserAdapter(session, realm, model, entity));
        }
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String s, RealmModel realmModel) {
        return null;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        TypedQuery<User> query = em.createNamedQuery("searchForUser", User.class);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<User> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (User entity : results) {
            users.add(new UserAdapter(session, realm, model, entity));
        }
        return users;
    }



    @Override
    public List<UserModel> searchForUser(Map<String, String> map, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> map, RealmModel realm, int i, int i1) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel groupModel, int i, int i1) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel groupModel) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String s, String s1, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        User entity = new User();
        entity.setUsername(username);
        em.persist(entity);
        logger.info("added user: " + username);
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel userModel) {
        String persistenceId = StorageId.externalId(userModel.getId());
        User entity = em.find(User.class, persistenceId);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }
}
