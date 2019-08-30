package secpriv.horst.visitors;

import secpriv.horst.data.Rule;

import java.util.Objects;

public class RuleTypeOracle {
    private final VisitorState state;

    public RuleTypeOracle(VisitorState state) {
        this.state = Objects.requireNonNull(state, "State may not be null");
    }

    public boolean isTest(Rule rule) {
        return state.isTest(rule);
    }

    public boolean isQueryOrTest(Rule rule) {
        return state.isQueryOrTest(rule);
    }

    public boolean isExpectedTestResult(Rule test, VisitorState.TestResult result) {
        return state.isExpectedTestResult(test, result);
    }
}
