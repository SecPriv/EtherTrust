package secpriv.horst.execution;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public abstract class ExecutionResultHandler {
    public abstract void handle(List<ExecutionResult> results);

    public static class JsonOutputExecutionResultHandler extends ExecutionResultHandler {
        private final String outputFileName;

        public JsonOutputExecutionResultHandler(String outputFileName) {
            this.outputFileName = outputFileName;
        }

        @Override
        public void handle(List<ExecutionResult> results) {
            try (PrintWriter writer = new PrintWriter(outputFileName)) {
                Gson gson = new Gson();

                JsonObject o = new JsonObject();
                gson.toJson(results, writer);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
