package org.vaadin.erik;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * @author erik@vaadin.com
 * @since 24/01/2020
 */
@Route
public class LoginView extends VerticalLayout
        implements ComponentEventListener<AbstractLogin.LoginEvent> {

    private LoginForm loginForm;

    private void initialize() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        loginForm = new LoginForm();
        loginForm.setForgotPasswordButtonVisible(false);
        add(loginForm);

        loginForm.addLoginListener(this);
    }

    @Override
    public void onAttach(AttachEvent event) {
        if (event.isInitialAttach()) {
            initialize();
        }
    }

    @Override
    public void onComponentEvent(AbstractLogin.LoginEvent loginEvent) {
        boolean success = SecurityService.getInstance()
                .authenticate(loginEvent.getUsername(), loginEvent.getPassword());
        if (success) {
            UI.getCurrent().navigate(SecurityService.getInstance().getDefaultView());
        } else {
            loginForm.setError(true);
        }
    }
}
