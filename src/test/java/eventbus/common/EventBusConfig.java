package eventbus.common;

import eventbus.spring.SpringEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author carl
 */
@Configuration
public class EventBusConfig {

    @Bean
    public SpringEventBus eventBus() {
        SpringEventBus eventBus = new SpringEventBus();
        return eventBus;
    }
}
