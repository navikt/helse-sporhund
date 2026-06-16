package no.nav.helse.sporhund.infrastructure.kafka.testhelpers

import com.github.navikt.tbd_libs.kafka.findOffsets
import com.github.navikt.tbd_libs.kafka.getPartitions
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.OffsetSpec
import org.testcontainers.kafka.KafkaContainer
import java.util.*

class TestcontainersKafka(
    moduleLabel: String,
) {
    private val kafka =
        KafkaContainer("apache/kafka:3.7.1")
            .withLabel("app", "sporhund")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply { start() }

    val config =
        LocalKafkaConfig(
            kafka.bootstrapServers,
        )

    private val adminClient = AdminClient.create(config.adminConfig(Properties()))

    fun isOnLatestOffset(
        consumerGroupId: String,
        topic: String,
    ): Boolean {
        val offsets =
            adminClient.findOffsets(
                adminClient.getPartitions(topic),
                OffsetSpec.latest(),
            )
        val committed =
            adminClient
                .listConsumerGroupOffsets(consumerGroupId)
                .partitionsToOffsetAndMetadata()
                .get()
                .map {
                    it.key to it.value.offset()
                }.toMap()
        return offsets.all { (partition, endOffset) ->
            (committed[partition] ?: 0L) >= endOffset
        }
    }

    fun waitForConsumerGroupAssignment(
        consumerGroupId: String,
        timeoutMs: Long = 10_000,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val members =
                adminClient
                    .describeConsumerGroups(listOf(consumerGroupId))
                    .describedGroups()[consumerGroupId]
                    ?.get()
                    ?.members()
            if (!members.isNullOrEmpty()) return
            Thread.sleep(100)
        }
        error("Consumer group '$consumerGroupId' fikk ikke tildelt partisjoner innen timeout")
    }

    fun stop() = kafka.stop()
}
