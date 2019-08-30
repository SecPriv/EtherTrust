package secpriv.horst.execution;

import com.microsoft.z3.Status;

import java.util.Optional;

public abstract class ExecutionResult {
    public final long executionTime;
    public final Status status;
    public final String queryId;
    public final Optional<String> info;

    public interface Visitor<T> {
        T accept(QueryResult queryResult);
        T accept(TestResult testResult);
    }

    private ExecutionResult(String queryId, Status status, long executionTime, Optional<String> info) {
        this.queryId = queryId;
        this.status = status;
        this.executionTime = executionTime;
        this.info = info;
    }

    public static class QueryResult extends ExecutionResult {
        public QueryResult(String queryId, Status status, long executionTime, Optional<String> info) {
            super(queryId, status, executionTime, info);
        }

        public <T> T accept(Visitor<T> visitor) {
            return visitor.accept(this);
        }
    }

    public static class TestResult extends ExecutionResult {
        public final boolean success;

        public TestResult(String queryId, Status status, long executionTime, boolean success, Optional<String> info) {
            super(queryId, status, executionTime, info);
            this.success = success;
        }

        public <T> T accept(Visitor<T> visitor) {
            return visitor.accept(this);
        }
    }
}
