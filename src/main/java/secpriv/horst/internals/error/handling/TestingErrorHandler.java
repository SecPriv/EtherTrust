package secpriv.horst.internals.error.handling;

import secpriv.horst.internals.error.objects.Error;

import java.util.ArrayList;

public class TestingErrorHandler implements ErrorHandler {
    public final ArrayList<Error> errorObjects;

    public TestingErrorHandler() {
        this.errorObjects = new ArrayList<>();
    }

    @Override
    public void handleError(Error error) {
        this.errorObjects.add(error);
    }
}
