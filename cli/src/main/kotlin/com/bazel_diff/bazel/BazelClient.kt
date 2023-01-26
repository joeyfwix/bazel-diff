package com.bazel_diff.bazel

import com.bazel_diff.log.Logger
import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentMap
import java.util.Calendar

class BazelClient : KoinComponent {
    private val logger: Logger by inject()
    private val queryService: BazelQueryService by inject()

    suspend fun queryAllTargets(excluding: String? = null): List<BazelTarget> {
        var calendar = Calendar.getInstance()
        val queryEpoch = calendar.getTimeInMillis()
		val allTargetsQuery = "//external:all-targets + //...:all-targets"
        val excludingQuery = excluding?.let { " - ($it)" } ?: "" // attr(generator_function, load_2nd_party_repositories, //external:all-targets)
		val targets = queryService.query(allTargetsQuery + excludingQuery)
        val queryDuration = calendar.getTimeInMillis() - queryEpoch
        logger.i { "All targets queried in $queryDuration" }
        return targets.mapNotNull { target: Build.Target ->
            when (target.type) {
                Build.Target.Discriminator.RULE -> BazelTarget.Rule(target)
                Build.Target.Discriminator.SOURCE_FILE -> BazelTarget.SourceFile(
                    target
                )
                Build.Target.Discriminator.GENERATED_FILE -> BazelTarget.GeneratedFile(
                    target
                )
                else -> {
                    logger.w { "Unsupported target type in the build graph: ${target.type.name}" }
                    null
                }
            }
        }
    }

    suspend fun queryAllSourcefileTargets(): List<Build.Target> {
        var calendar = Calendar.getInstance()
        val queryEpoch = calendar.getTimeInMillis()
        val targets = queryService.query("kind('source file', //...:all-targets)")
        val queryDuration = calendar.getTimeInMillis() - queryEpoch
        logger.i { "All source files queried in $queryDuration" }

        return targets
    }
}

