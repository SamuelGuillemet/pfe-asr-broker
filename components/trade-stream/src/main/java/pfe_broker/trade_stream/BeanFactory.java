package pfe_broker.trade_stream;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;

@Requires(bean = AdminClient.class)
@Factory
public class BeanFactory {

  @Bean
  CreateTopicsOptions options() {
    return new CreateTopicsOptions()
      .timeoutMs(5000)
      .validateOnly(false)
      .retryOnQuotaViolation(false);
  }

  @Bean
  NewTopic tradesTopic(
    @Property(name = "kafka.topics.trades") String topicName
  ) {
    return new NewTopic(topicName, 2, (short) 1);
  }

  @Bean
  NewTopic acceptedTradesTopic(
    @Property(name = "kafka.topics.accepted-trades") String topicName
  ) {
    return new NewTopic(topicName, 2, (short) 1);
  }

  @Bean
  NewTopic rejectedOrdersTopic(
    @Property(name = "kafka.topics.rejected-orders") String topicName
  ) {
    return new NewTopic(topicName, 2, (short) 1);
  }
}
