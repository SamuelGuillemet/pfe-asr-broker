package pfe_broker.trade_stream;

import static pfe_broker.log.Log.LOG;

import io.micronaut.configuration.kafka.streams.event.BeforeKafkaStreamStart;
import io.micronaut.context.event.ApplicationEventListener;
import jakarta.inject.Singleton;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;

@Singleton
public class KafkaStreamUncaughtExceptionHandler
  implements
    ApplicationEventListener<BeforeKafkaStreamStart>,
    StreamsUncaughtExceptionHandler {

  @Override
  public void onApplicationEvent(BeforeKafkaStreamStart event) {
    event.getKafkaStreams().setUncaughtExceptionHandler(this);
  }

  @Override
  public StreamThreadExceptionResponse handle(Throwable exception) {
    LOG.error("Uncaught exception in Kafka Streams", exception);
    return StreamThreadExceptionResponse.REPLACE_THREAD;
  }
}