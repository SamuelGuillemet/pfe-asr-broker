package pfe_broker.quickfix_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.models.domains.User;
import pfe_broker.models.repositories.UserRepository;
import pfe_broker.quickfix_server.mocks.MockOrderListener;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.MDReqID;
import quickfix.field.NoMDEntries;
import quickfix.field.NoRelatedSym;
import quickfix.field.SenderCompID;
import quickfix.field.Symbol;
import quickfix.fix42.MarketDataRequest;
import quickfix.fix42.MarketDataSnapshotFullRefresh;
import quickfix.fix42.NewOrderSingle;

@MicronautTest(
  rollback = false,
  transactional = false,
  transactionMode = TransactionMode.SINGLE_TRANSACTION
)
@Property(
  name = "datasources.default.driver-class-name",
  value = "org.testcontainers.jdbc.ContainerDatabaseDriver"
)
@Property(
  name = "datasources.default.url",
  value = "jdbc:tc:postgresql:16.1:///db"
)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServerApplicationTest implements TestPropertyProvider {
  static {
    Application.setProperties();
  }

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  private ServerApplication serverApplication;

  @Inject
  private UserRepository userRepository;

  private User user;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics("orders", "accepted-trades", "rejected-orders");
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @BeforeAll
  void setup() {
    user = new User("testuser", "testpassword", 1000.0);
    userRepository.save(user);
  }

  @Test
  public void testOnMessageNewOrderSingle(MockOrderListener mockOrderListener) {
    NewOrderSingle newOrderSingle = new NewOrderSingle();
    newOrderSingle.set(new quickfix.field.Symbol("AAPL"));
    newOrderSingle.set(new quickfix.field.OrderQty(10));
    newOrderSingle.set(new quickfix.field.Side(quickfix.field.Side.BUY));
    newOrderSingle.getHeader().setString(SenderCompID.FIELD, "testuser");
    try {
      serverApplication.onMessage(newOrderSingle, new SessionID("FIX.4.2", "testuser", "SERVER"));

      await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.receivedOrders).hasSize(1);
      });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // @Test
  //   void createMarketDataSnapshotTest() throws FieldNotFound {
  //       MarketDataRequest message = new MarketDataRequest();
  //       message.setString(MDReqID.FIELD, "123456");
  //       message.setInt(NoRelatedSym.FIELD, 2);
  //       message.getGroup(1, new MarketDataRequest.NoRelatedSym()).setString(Symbol.FIELD, "GOOGL");
  //       message.getGroup(2, new MarketDataRequest.NoRelatedSym()).setString(Symbol.FIELD, "AAPL");

  //       MarketDataSnapshotFullRefresh result = null;
  //       result = serverApplication.createMarketDataSnapshot(message);
        
  //       assertNotNull(result);
  //       assertEquals("123456", result.getString(MDReqID.FIELD));
  //       assertEquals(2, result.getInt(NoMDEntries.FIELD));
  //   }
}
