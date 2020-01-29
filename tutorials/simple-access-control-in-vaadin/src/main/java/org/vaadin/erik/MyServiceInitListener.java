package org.vaadin.erik;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * @author erik@vaadin.com
 * @since 24/01/2020
 */
public class MyServiceInitListener implements VaadinServiceInitListener, UIInitListener, BeforeEnterListener {

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource().addUIInitListener(this);
    }

    @Override
    public void uiInit(UIInitEvent uiInitEvent) {
        uiInitEvent.getUI().addBeforeEnterListener(this);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        boolean authenticated = SecurityService.getInstance().isAuthenticated();

        if (beforeEnterEvent.getNavigationTarget().equals(LoginView.class)) {
            if (authenticated) {
                beforeEnterEvent.forwardTo(SecurityService.getInstance().getDefaultView());
            }
            return;
        }

        if (!authenticated) {
            beforeEnterEvent.forwardTo(LoginView.class);
        }
    }
}
