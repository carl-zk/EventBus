package moc.oreh.eventbus.util;

/**
 * Created by hero on 17-4-5.
 */
public abstract class Assert {
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T> T requireNotNull(T object) {
        if (object != null)
            return object;
        else
            throw new IllegalArgumentException("argument must not be null");
    }
}
