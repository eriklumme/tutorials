package org.vaadin.erik;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * @author erik@vaadin.com
 * @since 24/01/2020
 */
public class SecurityService {

    private static final String USER_ATTRIBUTE = "SecurityService.User";

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";

    private static SecurityService instance;

    private SecurityService() {}

    public static SecurityService getInstance() {
        if (instance == null) {
            instance = new SecurityService();
        }
        return instance;
    }

    public boolean isAuthenticated() {
        return VaadinSession.getCurrent() != null  &&
                VaadinSession.getCurrent().getAttribute(USER_ATTRIBUTE) != null;
    }

    public boolean authenticate(String username, String password) {
        if (USERNAME.equals(username) && PASSWORD.equals(password)) {
            VaadinSession.getCurrent().setAttribute(USER_ATTRIBUTE, username);
            return true;
        }
        return false;
    }

    public void logOut() {
        VaadinSession.getCurrent().close();
        VaadinSession.getCurrent().getSession().invalidate();
        UI.getCurrent().navigate(LoginView.class);
    }

    public Class<? extends Component> getDefaultView() {
        return MainView.class;
    }
}
