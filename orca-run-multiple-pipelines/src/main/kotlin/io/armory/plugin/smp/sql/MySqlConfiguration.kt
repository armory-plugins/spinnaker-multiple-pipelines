package io.armory.plugin.smp.sql

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.config.OrcaSqlProperties
import com.netflix.spinnaker.kork.plugins.api.spring.ExposeToApp
import com.netflix.spinnaker.kork.sql.config.SqlProperties
import com.netflix.spinnaker.kork.telemetry.InstrumentedProxy
import com.netflix.spinnaker.orca.api.pipeline.persistence.ExecutionRepositoryListener
import com.netflix.spinnaker.orca.interlink.Interlink
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
open class MySqlConfiguration {

    @ConditionalOnProperty("execution-repository.sql.enabled")
    @Bean
    @ExposeToApp
    open fun sqlExecutionRepository(
        dsl: DSLContext,
        mapper: ObjectMapper,
        registry: Registry,
        properties: SqlProperties,
        orcaSqlProperties: OrcaSqlProperties,
        interlink: Optional<Interlink>,
        executionRepositoryListeners: Collection<ExecutionRepositoryListener>
    ) = ConcurrentSqlExecutionRepository(
        orcaSqlProperties.partitionName,
        dsl,
        mapper,
        properties.retries.transactions,
        orcaSqlProperties.batchReadSize,
        orcaSqlProperties.stageReadSize,
        interlink = interlink.orElse(null),
        executionRepositoryListeners = executionRepositoryListeners
    ).let {
        InstrumentedProxy.proxy(registry, it, "sql.executions", mapOf(Pair("repository", "primary"))) as ExecutionRepository
    }

}