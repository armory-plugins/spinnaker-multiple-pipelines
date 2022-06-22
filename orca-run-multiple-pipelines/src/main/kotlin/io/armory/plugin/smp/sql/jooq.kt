package io.armory.plugin.smp.sql

import com.netflix.spinnaker.kork.core.RetrySupport
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType
import de.huxhorn.sulky.ulid.ULID
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field

/**
 * Run the provided [fn] in a transaction.
 */
internal fun DSLContext.transactional(
    retrySupport: RetrySupport,
    fn: (DSLContext) -> Unit
) {
    retrySupport.retry(
        {
            transaction { ctx ->
                fn(DSL.using(ctx))
            }
        },
        5, 100, false
    )
}

/**
 * Converts a String id to a jooq where condition, either using the legacy
 * UUID scheme or modern ULID.
 */
internal fun String.toWhereCondition() =
    if (isULID(this)) {
        field("id").eq(this)
    } else {
        field("legacy_id").eq(this)
    }

/**
 * Determines if the given [id] is ULID format or not.
 */
internal fun isULID(id: String): Boolean {
    try {
        if (id.length == 26) {
            ULID.parseULID(id)
            return true
        }
    } catch (ignored: Exception) {}

    return false
}

/**
 * Convert an execution type to its jooq table object.
 */
internal val ExecutionType.tableName: Table<Record>
    get() = when (this) {
        ExecutionType.PIPELINE -> DSL.table("pipelines")
        ExecutionType.ORCHESTRATION -> DSL.table("orchestrations")
    }

/**
 * Convert an execution type to its jooq stages table object.
 */
internal val ExecutionType.stagesTableName: Table<Record>
    get() = when (this) {
        ExecutionType.PIPELINE -> DSL.table("pipeline_stages")
        ExecutionType.ORCHESTRATION -> DSL.table("orchestration_stages")
    }

/**
 * Selects all stages for an [executionType] and List [executionIds].
 */
internal fun DSLContext.selectExecutionStages(executionType: ExecutionType, executionIds: Collection<String>) =
    select(field("execution_id"), field("body"))
        .from(executionType.stagesTableName)
        .where(field("execution_id").`in`(*executionIds.toTypedArray()))
        .fetch()
        .intoResultSet()