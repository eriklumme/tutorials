# Simple Access Control in Vaadin

_Some things are best kept secret._ This goes for data too. In this tutorial, you will learn how to implement a simple access-control system using plain Vaadin and Java.

We do this in four steps: create the login view, authorize the user, authenticate the user, and create the logout button.

> This tutorial does not cover how the user credentials are stored, nor how to protect static resources, such as CSS and images.

The methods used here can be applied to most Vaadin projects, but they are written for the _Plain Java Servlet_ starter found at [vaadin.com/start](https://vaadin.com/start), which at the time of writing uses Vaadin version `14.1.5`.

After downloading and unzipping the project, it can be run using `mvn jetty:run` from the command line. The application and the `MainView` are then accessible at `localhost:8080`.

## Creating the login screen

Logging in requires a login screen. For simplicity's sake, we use Vaadin's [**LoginForm**](https://vaadin.com/components/vaadin-login/java-examples).

Let's create a class, `LoginView`, that extends `VerticalLayout`, and has a private field containing the `LoginForm`.

```java
public class LoginView extends VerticalLayout {

    private LoginForm loginForm;
}
```

It's good practice to defer component initialization until it is attached. This avoids potentially running costly code for a component that is never displayed to the user. Due to [Flow bug #4595](https://github.com/vaadin/flow/issues/4595), the constructor is run even when access to the component is restricted through the `BeforeEnterEvent` event. 

We can do the initialization by overriding the `onAttach` method. To only run it once, we check if this is the first attach event, using the `AttachEvent#isInitialAttach` method.

#### **LoginView.java**
```java
@Override
public void onAttach(AttachEvent event) {
    if (event.isInitialAttach()) {
        initialize();
    }
}
```

We can then create the initialization method, where we instantiate the login form. We align the form in the center, both horizontally and vertically, and then hide the `Forgot password?` option from the login form before adding it to the layout.

#### **LoginView.java**
```java
private void initialize() {
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
    setSizeFull();

    loginForm = new LoginForm();
    loginForm.setForgotPasswordButtonVisible(false);
    add(loginForm);
}
```

To enable navigating to the view, we annotate it with `@Route`. When no route is specified, the route will be the class name without "view", in this case `login`.

The login event can be handled by this view by implementing `ComponentEventListener<AbstractLogin.LoginEvent>`, and in the initialization method by adding the view as a listener using `loginForm.addLoginListener(this);`.

For now, the component event listener method can just set an error on the form, which displays a message that the username or password is incorrect.

#### **LoginView.java**
```java
@Override
public void onComponentEvent(AbstractLogin.LoginEvent loginEvent) {
    loginForm.setError(true);
}
```

After rebuilding or restarting the application, we can now navigate to `http://localhost:8080/login`, and see that no matter the username and password we enter, we always get an error message.

## Authorizing the user

Authorization is the process of determining if a user can perform a particular action. In our case, a user is authorized to access any view if the user is authenticated. Authorization is done before entering any view.

Let's start by creating a class `SecurityService`, that is responsible for checking if the user is authenticated. We make this a singleton class by creating a private constructor, and returning a static instance in a `getInstance()` method.

> The `getInstance()` method may be `synchronized` to protect against creating multiple instances due to concurrent access. In this case, as no state is stored in the class, we omit it to improve performance.

```java
public class SecurityService {

    private static SecurityService instance;

    private SecurityService() {}

    public static SecurityService getInstance() {
        if (instance == null) {
            instance = new SecurityService();
        }
        return instance;
    }
}
```

When the user logs in, we store the username as a session attribute under an arbitrary key. As such, we can check if the user is logged in by checking if that attribute is set.

#### **SecurityService.java**
```java
// This can be anything, but we want to avoid accidentally using the same name for different purposes
private static final String USER_ATTRIBUTE = "SecurityService.User";

...

public boolean isAuthenticated() {
    return VaadinSession.getCurrent() != null  &&
            VaadinSession.getCurrent().getAttribute(USER_ATTRIBUTE) != null;
}
```

Next, we need to call this method before entering any view. For this, we utilize a `BeforeEnterListener` added to every `UI`. We can use a [VaadinServiceInitListener](https://vaadin.com/docs/v14/flow/advanced/tutorial-service-init-listener.html) to add it whenever a `UI` is created.

We create a class implementing the `VaadinServiceInitListener` interface, and implement the `serviceInit` method.

```java
public class MyServiceInitListener implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
    }
}
```

For this to be registered on startup, we must create the `src/main/resources/META-INF/services/` directory and add a file named `com.vaadin.flow.server.VaadinServiceInitListener`. The only content in the file should be the fully qualified name of our service init listener, in our case, `org.vaadin.erik.MyServiceInitListener`.

Now that we have hooked onto the service initialization, we can use the initialization event to listen for `UI` initializations. Next, we change our class to also implement `UIInitListener`, and register it through the `ServiceInitEvent`.

#### **MyServiceInitListener.java**
```java
@Override
public void serviceInit(ServiceInitEvent serviceInitEvent) {
    serviceInitEvent.getSource().addUIInitListener(this);
}
```

We now go further down the rabbit hole by implementing the `uiInit` method to add the class as a `BeforeEnterListener` to the newly created `UI`. For this, we also need to implement `BeforeEnterListener`.

#### **MyServiceInitListener.java**
```java
@Override
public void uiInit(UIInitEvent uiInitEvent) {
    uiInitEvent.getUI().addBeforeEnterListener(this);
}
```

Finally, we implement the `beforeEnter` method to check if the user is authenticated, and if not, forward the user to the login view.

#### **MyServiceInitListener.java**
```java
@Override
public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    boolean authenticated = SecurityService.getInstance().isAuthenticated();

    if (beforeEnterEvent.getNavigationTarget().equals(LoginView.class)) {
        if (authenticated) {
            beforeEnterEvent.forwardTo(MainView.class);
        }
        return;
    }

    if (!authenticated) {
        beforeEnterEvent.forwardTo(LoginView.class);
    }
}
```

When navigating to the `LoginView`, the code includes a special case to avoid getting stuck in the loop of repeatedly forwarding the user to `LoginView`, by triggering the `beforeEnter` method again and again. Instead, if the user is logged in, they are forwarded to the `MainView`. No action is taken otherwise.

Running the application now, you can see that the `MainView` is no longer accessible, and navigating to `http://localhost:8080` forwards you to the login view.

## Authenticating the user

Now we can move on to authenticating the user, by determining if the provided username and password is correct or not.

We add a method to the `SecurityService` called `authenticate` that takes a username and password as arguments, and returns `true` if the authentication is successful. If this is the case, it also stores the username in the session.

This tutorial does not cover the authentication process in detail, instead we store the username and password directly in the class file. This means that anyone with access to the source code can see the credentials. As a minimum, the password should be hashed using a hashing function like `Bcrypt`.

#### **SecurityService.java**
```java
private static final String USERNAME = "admin";
private static final String PASSWORD = "password";

public boolean authenticate(String username, String password) {
    if (USERNAME.equals(username) && PASSWORD.equals(password)) {
        VaadinSession.getCurrent().setAttribute(USER_ATTRIBUTE, username);
        return true;
    }
    return false;
}
```

In the login view, we call this method, and only set an error if the method returns false. Otherwise, we redirect the user to the `MainView`. As we have now hardcoded the value `MainView.class` twice in the project, we should add a method in the security service that returns the default view, and change the prior uses to use this new method.

#### **LoginView.java**
```java
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
```

#### **SecurityService.java**
```java
public Class<? extends Component> getDefaultView() {
    return MainView.class;
}
```

At this point, we can log in to the application with the credentials `admin`/`password`. We can not yet log out, however.

## Logging out

To be able to log out, we need a method in the `SecurityService` for logging out. This method does two things:


* It invalidates the wrapped `HttpSession`, closing all `VaadinSession` instances that it contains. This causes a new `VaadinSession` to be created upon the next navigation, effectively clearing the stored user.
* It redirects the user to the login view.

#### **SecurityService.java**
```java
public void logOut() {
    VaadinSession.getCurrent().getSession().invalidate();
    UI.getCurrent().navigate(LoginView.class);
}
```

> _All servlets in a WAR-file share the same `HttpSession`, but have their own `VaadinSession` instances. If you only want to close a particular Vaadin session, call `VaadinSession.getCurrent().close()`._

Now we need to call this method from somewhere. We add a button for logging out to the `MainView`, and at the same time move all initialization to `onAttach`, as we did in the login view. We also remove the default components from the main view.

#### **MainView.java**
```java
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
```

The button simply calls the `logOut` method. We set the left margin to `auto`, in order to float the button to the right.

When we now test the application, we cannot access the `MainView` at first. After logging in, but only with the correct credentials, we are forwarded to the main view. Navigating to the login view now brings us right back to the main view.

After clicking the logout button, we can no longer access the main view, unless we log in again. 

Congratulations, access control is now set up in your application!

The complete code can be found [on GitHub](https://github.com/eriklumme/tutorials/tree/master/simple-access-control-in-vaadin).