package moc.oreh.eventbus;

import moc.oreh.eventbus.annotation.Subscribe;
import moc.oreh.eventbus.annotation.SubscribeMode;
import org.junit.Assert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Created by hero on 14/04/2018.
 */
@Configuration
@ComponentScan(basePackages = "moc.oreh.eventbus")
public class AppConfig {

    @Bean
    public SubUserLoginEvent sub() {
        return new SubUserLoginEvent();
    }

    static class SubUserLoginEvent {

        @Subscribe(mode = SubscribeMode.SYNC)
        public void sync(LoginEvent event) {
            System.out.println("sync" + event.getUsername());
            //throw new RuntimeException("runtime ex");
        }

        @Subscribe(mode = SubscribeMode.ASYNC)
        public void async(LoginEvent event) {
            System.out.println("async");
            throw new RuntimeException("runtime ex");
        }

        @Subscribe(mode = SubscribeMode.BACKGROUND)
        public void background(LoginEvent event) {
            System.out.println("background");
        }
    }

    static class LoginEvent {
        private String username;

        LoginEvent(String username) {
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
