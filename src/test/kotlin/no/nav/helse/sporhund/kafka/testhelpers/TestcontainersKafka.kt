package no.nav.helse.sporhund.kafka.testhelpers

import com.github.navikt.tbd_libs.kafka.findOffsets
import com.github.navikt.tbd_libs.kafka.getPartitions
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.OffsetSpec
import org.testcontainers.kafka.KafkaContainer
import java.util.Properties

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

    fun stop() = kafka.stop()
}
