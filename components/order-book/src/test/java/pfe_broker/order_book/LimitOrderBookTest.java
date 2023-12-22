package pfe_broker.order_book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.avro.Type;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.order_book.mocks.MockMarketDataProducer;
import pfe_broker.order_book.mocks.MockOrderProducer;
import pfe_broker.order_book.mocks.MockTradeListener;

@MicronautTest(transactional = false)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LimitOrderBookTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  OrderBookCatalog orderBooks;

  private final Double lowValue = 90.0;
  private final Double highValue = 120.0;
  private final String symbol = "AAPL";

  @Inject
  MockTradeListener mockTradeListener;

  @Inject
  MockMarketDataProducer mockMarketDataProducer;

  @Inject
  MockOrderProducer mockOrderProducer;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics(
      "market-data.AAPL",
      "accepted-orders-order-book",
      "trades"
    );
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @BeforeEach
  void reset(MockTradeListener mockTradeListener) {
    mockTradeListener.trades.clear();
    orderBooks.clear();
  }

  @Test
  void testAddOrder() {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.BUY,
      Type.LIMIT,
      80.0,
      "1"
    );

    mockOrderProducer.sendOrder("user:1", order);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(1);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(0);
      });
  }

  @Test
  void testMarketDataWithoutMatchingOrdersBuy() {
    Double limitPrice = lowValue - 1;

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.BUY,
      Type.LIMIT,
      limitPrice,
      "1"
    );

    mockOrderProducer.sendOrder("user:1", order);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(1);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(0);
      });

    MarketData marketData = new MarketData(
      100.0,
      highValue,
      lowValue,
      100.0,
      10
    );
    mockMarketDataProducer.sendMarketData(marketData);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(1);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(0);
      });
  }

  @Test
  void testMarketDataWithMatchingOrdersBuy() {
    Double limitPrice = lowValue + 1;

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.BUY,
      Type.LIMIT,
      limitPrice,
      "1"
    );

    mockOrderProducer.sendOrder("user:1", order);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(1);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(0);
      });

    MarketData marketData = new MarketData(
      100.0,
      highValue,
      lowValue,
      100.0,
      10
    );
    mockMarketDataProducer.sendMarketData(marketData);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(0);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(0);
        assertThat(mockTradeListener.trades).hasSize(1);
        assertThat(mockTradeListener.trades.get(0).getOrder()).isEqualTo(order);
        assertThat(mockTradeListener.trades.get(0).getPrice())
          .isEqualTo(limitPrice);
      });
  }

  @Test
  void testMarketDataWithoutMatchingOrdersSell() {
    Double limitPrice = highValue + 1;

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.SELL,
      Type.LIMIT,
      limitPrice,
      "1"
    );

    mockOrderProducer.sendOrder("user:1", order);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(0);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(1);
      });

    MarketData marketData = new MarketData(
      100.0,
      highValue,
      lowValue,
      100.0,
      10
    );
    mockMarketDataProducer.sendMarketData(marketData);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(0);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(1);
      });
  }

  @Test
  void testMarketDataWithMatchingOrdersSell() {
    Double limitPrice = highValue - 1;

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.SELL,
      Type.LIMIT,
      limitPrice,
      "1"
    );

    mockOrderProducer.sendOrder("user:1", order);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(0);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(1);
      });

    MarketData marketData = new MarketData(
      100.0,
      highValue,
      lowValue,
      100.0,
      10
    );
    mockMarketDataProducer.sendMarketData(marketData);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(0);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(0);
        assertThat(mockTradeListener.trades).hasSize(1);
        assertThat(mockTradeListener.trades.get(0).getOrder()).isEqualTo(order);
        assertThat(mockTradeListener.trades.get(0).getPrice())
          .isEqualTo(limitPrice);
      });
  }
}
