package eventbus.common;

import eventbus.annotation.Subscribe;
import eventbus.annotation.SubscribeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author carl
 */
@Configuration
@Import(EventBusConfig.class)
public class TestConfiguration {
    private static Logger log = LoggerFactory.getLogger(TestConfiguration.class);

    @Bean
    public LoginSubscriber loginSubscriber() {
        return new LoginSubscriber();
    }

    public static class LoginSubscriber {

        @Subscribe(mode = SubscribeMode.SYNC)
        public void sync(LoginEvent event) {
            log.info("login sync: {}", event.getUsername());
            //throw new RuntimeException("runtime ex");
        }

        @Subscribe(mode = SubscribeMode.ASYNC)
        public void async(LoginEvent event) {
            log.info("login async: {}", event.getUsername());
            //  throw new RuntimeException("runtime ex");
        }

        @Subscribe(mode = SubscribeMode.BACKGROUND)
        public void background(LoginEvent event) {
            log.info("login background: {}", event.getUsername());
        }
    }

    public static class LoginEvent {
        private String username;

        public LoginEvent(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
