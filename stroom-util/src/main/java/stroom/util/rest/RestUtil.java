package stroom.util.rest;

import java.util.function.Supplier;
import javax.ws.rs.BadRequestException;

public class RestUtil {

    private RestUtil() {
    }

    public static void requireNonNull(final Object object) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException();
        }
    }

    public static void requireNonNull(final Object object, String message) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(message);
        }
    }

    public static void requireNonNull(final Object object, Supplier<String> messageSupplier)
            throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(messageSupplier != null
                    ? messageSupplier.get()
                    : null);
        }
    }
}
