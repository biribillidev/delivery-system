package br.com.fiap.order.exception;

/** Business exceptions mapped to HTTP statuses by {@link ApiExceptionHandler}. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class DishNotFoundException extends RuntimeException {
        public DishNotFoundException(Long id) {
            super("Dish not found: " + id);
        }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(Long id) {
            super("Order not found: " + id);
        }
    }

    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException() {
            super("Dish out of stock");
        }
    }

    public static class PaymentUnavailableException extends RuntimeException {
        public PaymentUnavailableException(Throwable cause) {
            super("Payment unavailable", cause);
        }
    }

    public static class AssistantUnavailableException extends RuntimeException {
        public AssistantUnavailableException(Throwable cause) {
            super("Assistant unavailable: " + summarize(cause), cause);
        }
    }

    private static String summarize(Throwable cause) {
        String message = cause.getMessage() == null ? "" : cause.getMessage().replaceAll("\\s+", " ");
        String text = cause.getClass().getSimpleName() + (message.isEmpty() ? "" : ": " + message);
        return text.length() > 220 ? text.substring(0, 220) + "..." : text;
    }

    public static class TooManyRequestsException extends RuntimeException {
        public TooManyRequestsException() {
            super("Too many requests, try again in a moment");
        }
    }
}
