package ru.volodin.jasperai.pipeline;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public class Pipeline {

    @Singular
    private final List<PipelineStep> steps;

    public void execute(PipelineContext context) throws Exception {
        for (PipelineStep step : steps) {
            step.execute(context);
        }
    }
}
