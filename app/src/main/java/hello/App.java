package hello;

import rife.engine.*;

public class App extends Site {
    public void setup() {
        var hello = get("/hello", c -> c.print(c.template("hello")));
        get("/", c -> c.redirect(hello));
        get("/world", c -> c.print(c.template("world")));
    }

    public static void main(String[] args) {
        new Server()
            .staticResourceBase("src/main/webapp")
            .start(new App());
    }
}
