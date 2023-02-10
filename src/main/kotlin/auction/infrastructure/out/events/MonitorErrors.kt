package auction.infrastructure.out.events

import auction.domain.model.ReportCrash
import auction.domain.model.ReportError
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

private const val errorCounter = "domain.error"

private const val crashCounter = "app.crash"

class MonitorErrors(
    private val metrics: MeterRegistry,
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass()),
) {

    val reportError : ReportError = { error, originator ->
        logger.warn("domain-error: '${error::class.simpleName}', originator: '${originator.simpleName}'")
        metrics.counter(
            errorCounter,
            listOf(Tag.of("type", error::class.simpleName.toString()), Tag.of("originator", originator.simpleName.toString()))
        ).increment()
    }

    val reportCrash : ReportCrash = { crash, originator ->
        logger.error("originator: '${originator.simpleName}'", crash)
        metrics.counter(
            crashCounter,
            listOf(Tag.of("type", crash::class.simpleName.toString()), Tag.of("originator", originator.simpleName.toString()))
        ).increment()
    }
}
