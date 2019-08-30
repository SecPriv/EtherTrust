package secpriv.horst.translation;

import secpriv.horst.data.Rule;
import secpriv.horst.visitors.SExpressionRuleVisitor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TranslationPipeline implements Function<List<Rule>, List<Rule>> {
    private interface Step {
        List<Rule> apply(List<Rule> workingRules);
    }

    private static class MapStep implements Step {
        final Function<Rule, Rule> translation;

        private MapStep(Function<Rule, Rule> translation) {
            this.translation = translation;
        }

        @Override
        public List<Rule> apply(List<Rule> workingRules) {
            return workingRules.stream().map(translation).collect(Collectors.toList());
        }
    }

    private static class FlatMapStep implements Step {
        final Function<Rule, List<Rule>> translation;

        private FlatMapStep(Function<Rule, List<Rule>> translation) {
            this.translation = translation;
        }


        @Override
        public List<Rule> apply(List<Rule> workingRules) {
            return workingRules.stream().flatMap(r -> translation.apply(r).stream()).collect(Collectors.toList());
        }
    }

    public static class TranslationPipelineBuilder {
        private final List<Step> steps = new ArrayList<>();
        private final Map<Step, String> stepNames = new HashMap<>();
        private boolean debug = false;

        private TranslationPipelineBuilder() {
        }

        public TranslationPipelineBuilder debugStep(String stepName) {
            Step lastStep = steps.get(steps.size() - 1);
            stepNames.put(lastStep, stepName);

            return this;
        }

        public TranslationPipelineBuilder enableDebug() {
            debug = true;
            return this;
        }

        public TranslationPipelineBuilder addStep(Function<Rule, Rule> translation) {
            steps.add(new MapStep(translation));
            return this;
        }

        public TranslationPipelineBuilder addStep(Rule.Visitor<Rule> visitor) {
            return addStep((Function<Rule, Rule>) visitor::visit);
        }

        public TranslationPipelineBuilder addFlatMappingStep(Function<Rule, List<Rule>> translation) {
            steps.add(new FlatMapStep(translation));
            return this;
        }

        public TranslationPipelineBuilder addFlatMappingStep(Rule.Visitor<List<Rule>> visitor) {
            return addFlatMappingStep((Function<Rule, List<Rule>>) visitor::visit);
        }

        public TranslationPipeline build() {
            return new TranslationPipeline(steps, stepNames, debug);
        }
    }

    private final List<Step> steps;
    private final Map<Step, StringBuilder> debugOutput;
    private final Map<Step, String> stepNames;
    private final StringBuilder debugForRuleOutput;

    private TranslationPipeline(List<Step> steps, Map<Step, String> stepNames, boolean debug) {
        this.steps = Collections.unmodifiableList(steps);
        this.stepNames = stepNames;
        this.debugForRuleOutput = debug ? new StringBuilder() : null;
        this.debugOutput = new HashMap<>();

        for (Step step : stepNames.keySet()) {
            debugOutput.put(step, new StringBuilder());
        }
    }

    public static TranslationPipelineBuilder builder() {
        return new TranslationPipelineBuilder();
    }

    public List<Rule> apply(List<Rule> initialRules) {
        ArrayList<Rule> ret = new ArrayList<>();

        generateStepDebugHeaders();
        int ruleId = 1;

        for (Rule rule : initialRules) {
            List<Rule> workingRules = Collections.singletonList(rule);
            int stepId = 1;

            generateDebugOutputForRule(ruleId, rule);

            for (Step step : steps) {
                workingRules = step.apply(workingRules);
                generateDebugOutputForStep(step, workingRules);
                generateDebugOutputForRule(stepId, workingRules);
                ++stepId;
            }
            ret.addAll(workingRules);
            ++ruleId;
        }

        return ret;
    }

    public String getDebugOutput() {
        StringBuilder sb = new StringBuilder();
        for (Step step : steps) {
            sb.append(debugOutput.getOrDefault(step, new StringBuilder()));
        }
        if (debugForRuleOutput != null) {
            sb.append("\n");
            sb.append(debugForRuleOutput);
        }
        return sb.toString();
    }

    private void generateDebugOutputForRule(int stepId, List<Rule> rules) {
        if (debugForRuleOutput == null) {
            return;
        }

        debugForRuleOutput.append(String.format("---- Step %2d ----\n", stepId));
        for (Rule rule : rules) {
            debugForRuleOutput.append(rule.accept(new SExpressionRuleVisitor()));
            debugForRuleOutput.append("\n");
        }
    }

    private void generateDebugOutputForRule(int ruleId, Rule rule) {
        if (debugForRuleOutput == null) {
            return;
        }

        debugForRuleOutput.append(String.format("==== Rule %2d ====\n", ruleId));
        debugForRuleOutput.append(rule.accept(new SExpressionRuleVisitor()));
        debugForRuleOutput.append("\n");
    }

    private void generateDebugOutputForStep(Step step, List<Rule> rules) {
        if (stepNames.containsKey(step)) {
            StringBuilder sb = debugOutput.get(step);
            for (Rule rule : rules) {
                SExpressionRuleVisitor sExpressionRuleVisitor = new SExpressionRuleVisitor();
                sb.append(sExpressionRuleVisitor.visit(rule));
                sb.append("\n");
            }
        }
    }

    private void generateStepDebugHeaders() {
        for (Step step : steps) {
            if (stepNames.containsKey(step)) {
                StringBuilder sb = debugOutput.get(step);
                sb.append(generateStepDebugHeader(stepNames.get(step)));
            }
        }
    }

    private String generateStepDebugHeader(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() + 4; ++i) {
            sb.append("#");
        }
        sb.append("\n");
        sb.append("# ");
        sb.append(s);
        sb.append(" #\n");
        for (int i = 0; i < s.length() + 4; ++i) {
            sb.append("#");
        }
        sb.append("\n");
        return sb.toString();
    }
}

