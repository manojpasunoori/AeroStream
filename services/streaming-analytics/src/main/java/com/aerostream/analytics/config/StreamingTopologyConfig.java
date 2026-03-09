package com.aerostream.analytics.config;

import com.aerostream.analytics.model.RouteDelayAggregation;
import com.aerostream.analytics.service.RouteReliabilityService;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.KafkaStreamsDefaultConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class StreamingTopologyConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    KafkaStreamsConfiguration kStreamsConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming-analytics-v1");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public org.apache.kafka.streams.Topology routeDelayTopology(StreamsBuilder streamsBuilder,
                                                                 RouteReliabilityService reliabilityService) {
        Serde<String> stringSerde = Serdes.String();

        Serde<GenericRecord> avroSerde = new GenericAvroSerde();
        Map<String, String> serdeConfig = Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        avroSerde.configure(serdeConfig, false);

        streamsBuilder
                .stream("flight.events.v1", Consumed.with(stringSerde, avroSerde))
                .selectKey((k, v) -> v.get("origin").toString() + "->" + v.get("destination").toString())
                .mapValues(v -> { Number delay = (Number) v.get("delay_minutes"); return new RouteDelayAggregation(0, 0, 0).add(delay.intValue()); })
                .groupByKey(Grouped.with(stringSerde, new RouteDelaySerde()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
                .aggregate(
                        () -> new RouteDelayAggregation(0, 0, 0),
                        (route, current, aggregate) -> new RouteDelayAggregation(
                                aggregate.eventCount() + current.eventCount(),
                                aggregate.totalDelayMinutes() + current.totalDelayMinutes(),
                                aggregate.delayedFlights() + current.delayedFlights()
                        ),
                        Materialized.with(stringSerde, new RouteDelaySerde())
                )
                .toStream()
                .peek((windowedRoute, value) -> reliabilityService.update(windowedRoute.key(), value))
                .map((windowedRoute, value) -> org.apache.kafka.streams.KeyValue.pair(windowedRoute.key(),
                        "avgDelay=" + String.format("%.2f", value.averageDelay())
                                + ",score=" + String.format("%.2f", value.reliabilityScore())))
                .to("route.delay.analytics.v1", Produced.with(stringSerde, stringSerde));

        return streamsBuilder.build();
    }
}


