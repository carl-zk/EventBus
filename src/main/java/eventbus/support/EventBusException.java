package eventbus.support;

/**
 * @author carl
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
