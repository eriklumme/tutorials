package org.vaadin.erik;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
@PWA(name = "Project Base for Vaadin", shortName = "Project Base")
@CssImport("./styles/shared-styles.css")
@CssImport(value = "./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
public class MainView extends VerticalLayout {

    private void initialize() {
        Button logOutButton = new Button("Log out");
        logOutButton.getStyle().set("margin-left", "auto");
        logOutButton.addClickListener(e -> SecurityService.getInstance().logOut());

        HorizontalLayout toolbar = new HorizontalLayout(logOutButton);
        toolbar.setWidthFull();
        add(toolbar);

        add(new Span("Hello, you are logged in"));
    }

    public void onAttach(AttachEvent event) {
        if (event.isInitialAttach()) {
            initialize();
        }
    }
}
