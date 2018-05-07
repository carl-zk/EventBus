package eventbus.support;

/**
 * Created by hero on 17-4-5.
 */
public class EventBusException extends RuntimeException {
    private static final long serialVersionUID = 2140705946825208509L;

    public EventBusException(String detailMessage) {
        super(detailMessage);
    }

    public EventBusException(Throwable throwable) {
        super(throwable);
    }

    public EventBusException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
