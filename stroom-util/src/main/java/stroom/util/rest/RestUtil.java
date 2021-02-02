package stroom.util.rest;

import stroom.util.shared.HasIntegerId;

import javax.ws.rs.BadRequestException;
import java.util.function.Supplier;

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

    public static void requireNonNull(final Object object, Supplier<String> messageSupplier) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(messageSupplier != null ? messageSupplier.get() : null);
        }
    }

    public static void requireMatchingIds(final int id, final HasIntegerId object) {
        if (object == null) {
            throw new BadRequestException("Object is null");
        }
        if (object.getId() != id) {
            throw new BadRequestException("Id " + id + " doesn't match id in object " + object.getId());
        }
    }
}
