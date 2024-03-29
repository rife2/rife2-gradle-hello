/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package hello;

import org.junit.jupiter.api.Test;
import rife.test.MockConversation;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {
    @Test
    void verifyRoot() {
        var m = new MockConversation(new AppSite());
        assertEquals(m.doRequest("/").getStatus(), 302);
    }

    @Test
    void verifyHello() {
        var m = new MockConversation(new AppSite());
        assertEquals("Hello", m.doRequest("/hello")
            .getTemplate().getValue("title"));
    }
}
