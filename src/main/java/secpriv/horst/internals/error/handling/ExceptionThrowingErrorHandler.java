package secpriv.horst.internals.error.handling;

import secpriv.horst.internals.error.objects.Error;

public class ExceptionThrowingErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Error error) {
        throw new RuntimeException(error.toString());
    }
}
