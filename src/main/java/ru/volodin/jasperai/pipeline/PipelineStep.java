package ru.volodin.jasperai.pipeline;

public interface PipelineStep {

    void execute(PipelineContext context) throws Exception;
}
