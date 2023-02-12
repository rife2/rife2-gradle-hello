package hello;

import rife.engine.Server;
import rife.engine.Site;

public class AppUber extends App {
    public static void main(String[] args) {
        new Server()
            .staticUberJarResourceBase("webapp")
            .start(new AppUber());
    }
}