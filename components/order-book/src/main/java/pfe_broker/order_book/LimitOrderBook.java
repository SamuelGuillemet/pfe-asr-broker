package pfe_broker.order_book;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;

public class LimitOrderBook {

  private static final Logger LOG = LoggerFactory.getLogger(
    LimitOrderBook.class
  );

  private final String symbol;

  private final OrderTree buyOrderTree;

  private final OrderTree sellOrderTree;

  public LimitOrderBook(String symbol) {
    this.symbol = symbol;
    this.buyOrderTree = new OrderTree(Side.BUY);
    this.sellOrderTree = new OrderTree(Side.SELL);
  }

  public void addOrder(String id, Order order) {
    if (order.getSide() == Side.BUY) {
      buyOrderTree.addOrder(id, order);
    } else {
      sellOrderTree.addOrder(id, order);
    }
  }

  public Order removeOrder(String id) {
    Order order = null;
    if (buyOrderTree.contains(id)) {
      order = buyOrderTree.removeOrder(id);
    } else if (sellOrderTree.contains(id)) {
      order = sellOrderTree.removeOrder(id);
    }
    return order;
  }

  public Order replaceOrder(String id, Order order) {
    Order oldOrder = null;
    if (buyOrderTree.contains(id)) {
      oldOrder = buyOrderTree.replaceOrder(id, order);
    } else if (sellOrderTree.contains(id)) {
      oldOrder = sellOrderTree.replaceOrder(id, order);
    }
    return oldOrder;
  }

  public Map<String, Trade> matchOrdersToTrade(MarketData marketData) {
    Double low = marketData.getLow();
    Double high = marketData.getHigh();

    Map<String, Order> orders = new HashMap<>();
    orders.putAll(buyOrderTree.matchOrders(low));
    orders.putAll(sellOrderTree.matchOrders(high));

    Map<String, Trade> trades = new HashMap<>();
    orders.forEach((id, order) -> {
      Trade trade = new Trade(
        order,
        symbol,
        order.getPrice(),
        order.getQuantity()
      );
      trades.put(id, trade);
    });
    return trades;
  }

  public Map<String, Order> getBuyOrders() {
    return buyOrderTree.getOrders();
  }

  public Map<String, Order> getSellOrders() {
    return sellOrderTree.getOrders();
  }

  public String getSymbol() {
    return symbol;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LimitOrderBook: ").append(symbol).append("\n");
    sb.append("Buy Orders: ").append("\n");
    sb.append(buyOrderTree.toString()).append("\n");
    sb.append("Sell Orders: ").append("\n");
    sb.append(sellOrderTree.toString()).append("\n");
    return sb.toString();
  }
}