package io.armory.plugin.smp.execution

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder
import com.netflix.spinnaker.orca.config.ExecutionConfigurationProperties
import com.netflix.spinnaker.orca.events.BeforeInitialExecutionPersist
import com.netflix.spinnaker.orca.pipeline.ExecutionRunner
import com.netflix.spinnaker.orca.pipeline.PipelineValidator
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import javax.annotation.Nonnull
import java.time.Clock

import static com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType.PIPELINE

class MyExecutionLauncherSpec extends Specification {

    @Shared
    def objectMapper = new ObjectMapper()
    def executionRunner = Mock(ExecutionRunner)
    def executionRepository = Mock(ExecutionRepository)
    def pipelineValidator = Stub(PipelineValidator)
    def applicationEventPublisher = Mock(ApplicationEventPublisher)

    MyExecutionLauncher create() {
        return new MyExecutionLauncher(
                objectMapper,
                executionRepository,
                executionRunner,
                Clock.systemDefaultZone(),
                applicationEventPublisher,
                Optional.of(pipelineValidator),
                Optional.<Registry>empty(),
                new ExecutionConfigurationProperties()
        )
    }

    def "can autowire execution launcher with optional dependencies"()

    {
        given:
        def context = new AnnotationConfigApplicationContext()
        context.with {
        beanFactory.with {
            registerSingleton("clock", Clock.systemDefaultZone())
            registerSingleton("objectMapper", objectMapper)
            registerSingleton("executionRepository", executionRepository)
            registerSingleton("executionRunner", executionRunner)
            registerSingleton("whateverStageDefBuilder", new StageDefinitionBuilder() {
                @Nonnull
                @Override
                String getType() {
                    return "whatever"
                }
            })
            registerSingleton("executionConfigurationProperties", new ExecutionConfigurationProperties())
        }
        register(MyExecutionLauncher)
        refresh()
    }

        expect:
        context.getBean(MyExecutionLauncher)
    }

    def "can autowire execution launcher without optional dependencies"()

    {
        given:
        def context = new AnnotationConfigApplicationContext()
        context.with {
        beanFactory.with {
            registerSingleton("clock", Clock.systemDefaultZone())
            registerSingleton("objectMapper", objectMapper)
            registerSingleton("executionRepository", executionRepository)
            registerSingleton("executionRunner", executionRunner)
            registerSingleton("whateverStageDefBuilder", new StageDefinitionBuilder() {
                @Nonnull
                @Override
                String getType() {
                    return "whatever"
                }
            })
            registerSingleton("executionConfigurationProperties", new ExecutionConfigurationProperties())
        }
        register(MyExecutionLauncher)
        refresh()
    }

        expect:
        context.getBean(MyExecutionLauncher)
    }

    def "starts pipeline"()

    {
        given:
        @Subject def launcher = create()

        when:
        launcher.start(PIPELINE, config)

        then:
        1 * applicationEventPublisher.publishEvent(_ as BeforeInitialExecutionPersist)
        1 * executionRunner.start(_)

        where:
        config = [id:
    "whatever", stages: []]
    }
}