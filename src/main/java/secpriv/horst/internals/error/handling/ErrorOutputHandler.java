package secpriv.horst.internals.error.handling;

import secpriv.horst.internals.error.objects.Error;

public class ErrorOutputHandler implements ErrorHandler {
    @Override
    public void handleError(Error error) {
        System.err.println(error.toString());
    }
}
