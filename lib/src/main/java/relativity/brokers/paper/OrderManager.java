/*
 * Copyright (c) 2024. Relativity Software. All Rights Reserved.
 *
 * Licensed under the Functional Source License, Version 1.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the license at
 *
 * https://github.com/Relativity-Software/relativity/blob/main/LICENSE.md
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============================================================================
 */

package relativity.brokers.paper;

import relativity.events.EventService;
import relativity.events.types.OrderCreatedEvent;
import relativity.instruments.PriceMovement;
import relativity.instruments.InstrumentManager;
import relativity.instruments.types.Instrument;
import org.tinylog.Logger;
import relativity.brokers.types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OrderManager {
    public ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Order> pastOrders = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, String> orderStrategies = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, String> ordersInFulfillment = new ConcurrentHashMap<>();

    public InstrumentManager instrumentManager;

    public AccountManager accountManager;

    public EventService eventService;

    public OrderManager(
        InstrumentManager instrumentManager
    ) {
        this.instrumentManager = instrumentManager;
    }

    public ArrayList<Order> getBuyOrdersForSymbol(String symbol, String accountId) {
        ArrayList<Order> buyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.BUY &&
                order.accountId== accountId
            ) {
                buyOrders.add(order);
            }
        }

        return buyOrders;
    }

    public ArrayList<Order> getBuyOrdersForSymbol(String symbol) {
        ArrayList<Order> buyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.BUY
            ) {
                buyOrders.add(order);
            }
        }

        return buyOrders;
    }

    public boolean hasBuyOrdersForSymbol(String symbol) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.BUY
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean hasBuyOrdersForSymbol(String symbol, String accountId) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.BUY &&
                order.accountId== accountId
            ) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Order> getSellOrdersForSymbol(String symbol, String accountId) {
        ArrayList<Order> buyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.SELL &&
                order.accountId== accountId
            ) {
                buyOrders.add(order);
            }
        }

        return buyOrders;
    }

    public ArrayList<Order> getSellOrdersForSymbol(String symbol) {
        ArrayList<Order> buyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.SELL
            ) {
                buyOrders.add(order);
            }
        }

        return buyOrders;
    }

    public boolean hasSellOrdersForSymbol(String symbol) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.SELL
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean hasSellOrdersForSymbol(String symbol, String accountId) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.side == OrderSideEnum.SELL &&
                order.accountId== accountId
            ) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Order> getOpenOrdersForSymbol(String symbol) {
        ArrayList<Order> openOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            // TODO: check if unfulfilled order
        }

        return openOrders;
    }

    public boolean isUnfulfilledOrder(Order order) {
        List<OrderStatusEnum> fulfilledList = Arrays.asList(
            OrderStatusEnum.FILLED,
            OrderStatusEnum.EXPIRED,
            OrderStatusEnum.REPLACED,
            OrderStatusEnum.REJECTED,
            OrderStatusEnum.SUSPENDED,
            OrderStatusEnum.CANCELED
        );

        return !fulfilledList.contains(order.status);
    }

    public boolean isAbleToBeReplaced(Order order) {
        List<OrderStatusEnum> fulfilledList = Arrays.asList(
            OrderStatusEnum.FILLED,
            OrderStatusEnum.EXPIRED,
            OrderStatusEnum.REPLACED,
            OrderStatusEnum.REJECTED,
            OrderStatusEnum.SUSPENDED,
            OrderStatusEnum.SUSPENDED,
            OrderStatusEnum.CANCELED
        );

        return !fulfilledList.contains(order.status);
    }

    public boolean isPendingOrder(Order order) {
        List<OrderStatusEnum> pendingList = Arrays.asList(
            OrderStatusEnum.PENDING,
            OrderStatusEnum.PENDING_CANCEL,
            OrderStatusEnum.PENDING_REPLACE,
            OrderStatusEnum.PENDING_ACTIVATION
        );

        return pendingList.contains(order.status);
    }

    public boolean hasOrderForSymbol(String symbol, String accountId) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.accountId== accountId
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean hasOrderForSymbol(String symbol) {
        for (Order order : orders.values()) {
            if (order.symbol == symbol) {
                return true;
            }
        }

        return false;
    }

    public boolean hasOrderForSymbols(ArrayList<String> symbols) {
        for (Order order : orders.values()) {
            if (symbols.contains(order.symbol)) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Order> cancelEnterPositionOrders(String symbol, String accountId) {
        ArrayList<Order> enterPositionOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.intent == OrderIntentEnum.OPEN &&
                order.accountId== accountId
            ) {
                enterPositionOrders.add(order);
            }
        }

        // TODO: cancel orders

        return enterPositionOrders;
    }

    public void updateOpenOrder(Order order) {
        if (order.strategyId == null) {
            // TODO: orphan order
        }

        orders.put(order.id, order);

        // TODO: find position for order and add order to position
    }

    public void removeOpenOrder(String orderId) {
        Order order = orders.get(orderId);

        if (order == null) {
            // TODO: order not found

            return;
        }

        pastOrders.put(orderId, order);
        orders.remove(orderId);
        orderStrategies.remove(orderId);

        // TODO: find position for order and if position, add order to position
    }

    public void removeCloseOrderFromPosition(String orderId) {
        // TODO: call cancel order from this class
    }

    public void addToOpenOrders(Order order) {
        orders.put(order.id, order);


    }

    public Order findBuyOrderForSymbol(String symbol, String accountId) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.accountId== accountId &&
                order.side == OrderSideEnum.BUY
            ) {
                return order;
            }
        }

        return null;
    }

    public ArrayList<Order> findEnterPositionOrders(String accountId) {
        ArrayList<Order> enterPositionOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.intent == OrderIntentEnum.OPEN &&
                order.accountId== accountId
            ) {
                enterPositionOrders.add(order);
            }
        }

        return enterPositionOrders;
    }

    public ArrayList<Order> findEnterPositionOrders(UUID strategyId, String accountId) {
        ArrayList<Order> enterPositionOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
            order.intent == OrderIntentEnum.OPEN &&
            order.accountId== accountId &&
            order.strategyId == strategyId
            ) {
                enterPositionOrders.add(order);
            }
        }

        return enterPositionOrders;
    }

    public Order findClosePositionOrder(Position position) {
        for (Order order : orders.values()) {
            if (
                order.intent == OrderIntentEnum.OPEN &&
                order.accountId == position.accountId &&
                order.strategyId == position.strategyId
            ) {
                return order;
            }
        }

        return null;
    }

    public Float getEnterPositionOrdersValues(String accountId) {
        Float value = 0.0f;

        for (Order order : orders.values()) {
            if (
                order.intent == OrderIntentEnum.OPEN &&
                order.accountId== accountId
            ) {
                value += Math.abs(order.marketValue);
            }
        }

        return value;
    }

    public Float getOrdersValue(String accountId) {
        Float value = 0.0f;

        for (Order order : orders.values()) {
            if (order.accountId== accountId) {
                value += Math.abs(order.marketValue);
            }
        }

        return value;
    }

    public Float getOrdersValue() {
        Float value = 0.0f;

        for (Order order : orders.values()) {
            value += Math.abs(order.marketValue);
        }

        return value;
    }

    public Order replaceOrder(Order order, OrderOptions update) {
        if (!isAbleToBeReplaced(order)) {
            return order;
        }

        order.status = OrderStatusEnum.REPLACED;

        // TODO: account manager settle order

        if (update.intent == null) {
            update.intent = OrderIntentEnum.OPEN;
        }

        removeOpenOrder(order.id);

        return createOrderIfNotExists(update);
    }

    public Order replaceOrderWithPrice(Order order, Float price) {
        OrderOptions update = new OrderOptions();
        update.limitPrice = price;
        update.symbol = order.symbol;
        update.quantity = order.quantity;
        update.side = order.side;
        update.orderType = order.type;
        update.timeInForce = order.timeInForce;
        update.accountId = order.accountId;
        update.userId = order.userId;
        update.intent = order.intent;
        update.strategyId = order.strategyId;
        update.strategyName = order.strategyName;
        update.reason = "Replace with new price";

        return replaceOrder(order, update);
    }

    public ArrayList<Order> getOpenOrders() {
        return (ArrayList<Order>) orders.values();
    }

    public Order createOrderIfNotExists(OrderOptions options) {
        for (Order order : orders.values()) {
            if (
                order.symbol == options.symbol &&
                order.side == options.side &&
                order.intent == options.intent &&
                order.accountId== options.accountId
            ) {
                return order;
            }
        }

//        for (Order order : pastOrders.values()) {
//            if (
//                order.symbol == options.symbol &&
//                order.side == options.side &&
//                order.intent == options.intent &&
//                order.accountId== options.accountId &&
//                (order.filledAt != null && order.filledAt > System.currentTimeMillis() - 1000)
//            ) {
//                return order;
//            }
//        }

        return createOrder(options, false);
    }

    public Order createOrder(OrderOptions options, boolean isChild) {
        // TODO: Bring over all the account and security logic too
        Instrument instrument = instrumentManager.getInstrument(options.symbol);

        if (instrument == null) {
            throw new Error("Security not found: " + options.symbol);
        }

        Float marketValue = options.limitPrice * options.quantity;

        if(
            options.intent == OrderIntentEnum.OPEN &&
            accountManager.calculateBuyingPower(options.accountId) < marketValue
        ) {
            throw new Error("Insufficient buying power for " + options.symbol + " " + options.accountId + " " + marketValue);
        }

        Float cashBalance = 0.0f;
        Float marginBalance = 0.0f;

        if (options.intent == OrderIntentEnum.OPEN) {
//            marketValue *= options.side == OrderSideEnum.BUY
//                ? 1
//                : -1;

            Account account = accountManager.getAccount(options.accountId);

            if (account.cashBalance < marketValue) {
               cashBalance = account.cashBalance;
               marginBalance = marketValue - account.cashBalance;
            }

            if (account.cashBalance >= marketValue) {
                cashBalance = marketValue;
            }

            if (marginBalance > account.marginBalance) {
                marginBalance = account.marginBalance - 30;
            }

            // TODO: Should error if not enough cash or margin
        }

        Order order = new Order();
        order.id = UUID.randomUUID().toString();
        order.instrument = instrument;
        order.strategyId = options.strategyId;
        order.strategyName = options.strategyName;
        order.accountId = options.accountId;
        order.symbol = options.symbol;
        order.timeInForce = options.timeInForce;
        order.quantity = options.quantity;
        order.filledQuantity = 0.0f;
        order.side = options.side;
        order.type = options.orderType;
        order.intent = options.intent;
        order.userId = options.userId;
        order.createdAt = System.currentTimeMillis();
        order.updatedAt = System.currentTimeMillis();
        order.status = !isChild
            ? OrderStatusEnum.PENDING
            : OrderStatusEnum.PENDING_ACTIVATION;
        order.limitPrice = options.limitPrice;

        order.cashBalance = cashBalance;
        order.marginBalance = marginBalance;
        order.marketValue = marketValue;

        if (cashBalance < 0) {
            Logger.info(order.symbol + " " + order.side + " " + order.intent + " Cash balance is less than 0: " + cashBalance + " margin balance: " + marginBalance);
        }

        order.reason = options.reason;
//        order.orderClass = options.orderClass;
//        order.raw = options.raw;

        // TODO: stop loss

        if (
            options.takeProfit != null &&
            options.takeProfit.limitPrice != null
        ) {
            OrderOptions takeProfitOptions = new OrderOptions();
            takeProfitOptions.symbol = options.symbol;
            takeProfitOptions.quantity = options.quantity;
            takeProfitOptions.side = options.side == OrderSideEnum.BUY ? OrderSideEnum.SELL : OrderSideEnum.BUY;
            takeProfitOptions.orderType = OrderTypeEnum.LIMIT;
            takeProfitOptions.limitPrice = options.takeProfit.limitPrice;
            takeProfitOptions.intent = OrderIntentEnum.CLOSE;
            takeProfitOptions.strategyId = options.strategyId;
            takeProfitOptions.strategyName = options.strategyName;
            takeProfitOptions.timeInForce = options.timeInForce;
            takeProfitOptions.accountId = options.accountId;
            takeProfitOptions.reason = "Child Take Profit";

            order.childOrders.add(createOrder(takeProfitOptions, true));
        }

        if (isChild) {
            // This is attached to the parent order and will be activated
            // from that object
            return order;
        }

        orders.put(order.id, order);
        eventService.processEvent(new OrderCreatedEvent(order));

        return order;
    }

    public ArrayList<Order> splitOrderIntoLots(OrderOptions options) {
        ArrayList<Order> orders = new ArrayList<>();

        if (options.quantity <= 100) {
            return orders;
        }

        int lots = (int) Math.ceil(options.quantity / 100);

        for (int i = 0; i < lots; i++) {
            OrderOptions lotOptions = new OrderOptions();
            lotOptions.symbol = options.symbol;
            lotOptions.quantity = (float) Math.floor(options.quantity / lots);
            lotOptions.side = options.side;
            lotOptions.orderType = options.orderType;
            lotOptions.intent = options.intent;
            lotOptions.strategyId = options.strategyId;
            lotOptions.strategyName = options.strategyName;
            lotOptions.timeInForce = options.timeInForce;
            lotOptions.accountId = options.accountId;
            lotOptions.userId = options.userId;

            orders.add(createOrder(lotOptions, false));
        }

        return orders;
    }

    public Order buyStopLoss(OrderOptions options) {
        options.orderType = OrderTypeEnum.STOP;
        options.side = OrderSideEnum.BUY;

        // TODO: double check this

        return createOrder(options, false);
    }

    public Order sellStopLoss(OrderOptions options) {
        options.orderType = OrderTypeEnum.STOP;
        options.side = OrderSideEnum.SELL;

        return createOrder(options, false);
    }

    public Order cancelOrder(Order order) {
        if (isPendingOrder(order)) {
            return order;
        }

        order.status = OrderStatusEnum.CANCELED;

        if (order.childOrders.size() > 0) {
            for (Order childOrder : order.childOrders) {
                cancelOrder(childOrder);
            }
        }

        removeOpenOrder(order.id);
        pastOrders.put(order.id, order);

        // TODO: Handle position and order
        // TODO: account manager settle order

        return order;
    }

    public Order findOrder(String orderId) {
        return orders.get(orderId);
    }

    public Order findPastOrder(String orderId) {
        return pastOrders.get(orderId);
    }

    public Order findOrderForSymbol(String symbol, String accountId) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.accountId== accountId
            ) {
                return order;
            }
        }

        return null;
    }

    public Order findOrderForSymbol(String symbol) {
        for (Order order : orders.values()) {
            if (order.symbol == symbol) {
                return order;
            }
        }

        return null;
    }

    public ArrayList<Order> findOrdersForStrategy(UUID strategyId) {
        ArrayList<Order> strategyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (order.strategyId == strategyId) {
                strategyOrders.add(order);
            }
        }

        return strategyOrders;
    }

    public ArrayList<Order> findOrderForStrategy(UUID strategyId, String accountId) {
        ArrayList<Order> strategyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.strategyId == strategyId &&
                order.accountId== accountId
            ) {
                strategyOrders.add(order);
            }
        }

        return strategyOrders;
    }

    public ArrayList<Order> findOrdersForStrategy(UUID strategyId, String symbol, String accountId) {
        ArrayList<Order> strategyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.strategyId == strategyId &&
                order.symbol == symbol &&
                order.accountId== accountId &&
                isUnfulfilledOrder(order)
            ) {
                strategyOrders.add(order);
            }
        }

        return strategyOrders;
    }

    public ArrayList<Order> findOrdersForStrategy(UUID strategyId, String symbol) {
        ArrayList<Order> strategyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.strategyId == strategyId &&
                order.symbol == symbol &&
                isUnfulfilledOrder(order)
            ) {
                strategyOrders.add(order);
            }
        }

        return strategyOrders;
    }

    public ArrayList<Order> findOrdersForStrategy(UUID strategyId, String symbol, OrderSideEnum side) {
        ArrayList<Order> strategyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.strategyId == strategyId &&
                order.symbol == symbol &&
                order.side == side &&
                isUnfulfilledOrder(order)
            ) {
                strategyOrders.add(order);
            }
        }

        return strategyOrders;
    }

    public ArrayList<Order> findOrdersForStrategy(UUID strategyId, String symbol, OrderSideEnum side, String accountId) {
        ArrayList<Order> strategyOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.strategyId == strategyId &&
                order.symbol == symbol &&
                order.side == side &&
                order.accountId== accountId &&
                isUnfulfilledOrder(order)
            ) {
                strategyOrders.add(order);
            }
        }

        return strategyOrders;
    }

    public boolean hasOrdersForStrategy(UUID strategyId) {
        for (Order order : orders.values()) {
            if (order.strategyId == strategyId) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPastOrdersForStrategy(UUID strategyId) {
        for (Order order : pastOrders.values()) {
            if (order.strategyId == strategyId) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPastOpenOrdersForStrategy(UUID strategyId) {
        for (Order order : pastOrders.values()) {
            if (
            order.strategyId == strategyId &&
            order.intent == OrderIntentEnum.OPEN
            ) {
                return true;
            }
        }

        return false;
    }

    public void updateOrders(String symbol, Float price) {
        for (Order order : orders.values()) {
            if (
                order.symbol == symbol &&
                order.status != OrderStatusEnum.FILLED &&
                order.status != OrderStatusEnum.CANCELED
            ) {
                order.marketValue = price * order.quantity;

                if (order.status == OrderStatusEnum.ACCEPTED) {
                    order.status = OrderStatusEnum.WORKING;
                }

                // TODO: Fire Order event
            }
        }
    }

    public boolean isClosingOrder(Order order) {
        return order.intent == OrderIntentEnum.CLOSE;
    }

    public Order getOpenOrderForPosition(Position position) {
        for (Order order : orders.values()) {
            if (order.positions.contains(position)) {
                return order;
            }
        }

        return null;
    }

    public Order getOpenOrderForPosition(String positionId) {
        for (Order order : orders.values()) {
            for (Position position : order.positions) {
                if (position.id == positionId) {
                    return order;
                }
            }
        }

        return null;
    }

    public boolean hasCloseOrderForPosition(Position position) {
        for (Order order : orders.values()) {
            if (
                order.positions.contains(position) &&
                order.intent == OrderIntentEnum.CLOSE
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean hasCloseOrderForPosition(String positionId) {
        for (Order order : orders.values()) {
            for (Position position : order.positions) {
                if (
                    position.id == positionId &&
                    order.intent == OrderIntentEnum.CLOSE
                ) {
                    return true;
                }
            }
        }

        return false;
    }

    public Order getCloseOrderForPosition(Position position) {
        for (Order order : orders.values()) {
            if (
                order.positions.contains(position) &&
                order.intent == OrderIntentEnum.CLOSE
            ) {
                return order;
            }
        }

        return null;
    }

    public Order getCloseOrderForPosition(String positionId) {
        for (Order order : orders.values()) {
            for (Position position : order.positions) {
                if (
                    position.id == positionId &&
                    order.intent == OrderIntentEnum.CLOSE
                ) {
                    return order;
                }
            }
        }

        return null;
    }

    public ArrayList<Order> getClosingOrdersForPosition(Position position) {
        ArrayList<Order> closingOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.positions.contains(position) &&
                order.intent == OrderIntentEnum.CLOSE &&
                isUnfulfilledOrder(order)
            ) {
                closingOrders.add(order);
            }
        }

        return closingOrders;
    }

    public ArrayList<Order> getFulfilledClosingOrdersForPosition(Position position) {
        ArrayList<Order> closingOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            if (
                order.positions.contains(position) &&
                order.intent == OrderIntentEnum.CLOSE &&
                order.status == OrderStatusEnum.FILLED
            ) {
                closingOrders.add(order);
            }
        }

        return closingOrders;
    }

    public ArrayList<Order> getClosingOrdersForPosition(String positionId) {
        ArrayList<Order> closingOrders = new ArrayList<>();

        for (Order order : orders.values()) {
            for (Position position : order.positions) {
                if (
                    position.id == positionId &&
                    order.intent == OrderIntentEnum.CLOSE &&
                    isUnfulfilledOrder(order)
                ) {
                    closingOrders.add(order);
                }
            }
        }

        return closingOrders;
    }

    public boolean hasActiveClosingOrdersForPosition(Position position) {
        for (Order order : orders.values()) {
            if (
                order.positions.contains(position) &&
                order.intent == OrderIntentEnum.CLOSE &&
                isUnfulfilledOrder(order)
            ) {
                return true;
            }
        }

        return false;
    }

    public boolean isCloseOrderForAPosition(Order order) {
        return order.intent == OrderIntentEnum.CLOSE;
    }

    public boolean isCloseOrderForAPosition(String orderId) {
        Order order = orders.get(orderId);

        return order.intent == OrderIntentEnum.CLOSE;
    }

    public void fulfillOrder(Order order, ArrayList<OrderFill> fills) {
        if (
            order.status == OrderStatusEnum.FILLED ||
            order.status == OrderStatusEnum.CANCELED ||
            (
               order.orderFills.size() > 0 &&
               // TODO: check why this might be submitted twice
               order.quantity == order.filledQuantity
            )
        ) {
            return;
        }

        ordersInFulfillment.put(order.id, order.id);

        for (OrderFill fill : fills) {
            order.orderFills.add(fill);
        }

        for (OrderFill fill : fills) {
            order.filledQuantity += fill.quantity;
        }

        if (order.filledQuantity.equals(order.quantity)) {
            order.status = OrderStatusEnum.FILLED;
            order.filledAt = System.currentTimeMillis();

            if (order.childOrders.size() > 0) {
                for (Order childOrder : order.childOrders) {
                    activateOrder(childOrder);
                }
            }

            ArrayList<Float> prices = new ArrayList<>();

            for (OrderFill fill : order.orderFills) {
                prices.add(fill.price);
            }

            order.filledAveragePrice = prices.stream().reduce(0.0f, Float::sum) / prices.size();

            pastOrders.put(order.id, order);
            orders.remove(order.id);
        }

        ordersInFulfillment.remove(order.id);

        // TODO: handle cash and margin balance when there is a difference
    }

    public void activateOrder(Order order) {
        Logger.info("Activitating order: " + order.symbol + " " + order.side + " " + order.status);
        if (order.status != OrderStatusEnum.PENDING_ACTIVATION) {
            return;
        }

        order.status = OrderStatusEnum.WORKING;
        order.updatedAt = System.currentTimeMillis();

        // TODO: possibly create an alternative to checking for child orders
        // in strategies. This helps with canceling child orders too early
        order.createdAt = System.currentTimeMillis();

        orders.put(order.id, order);

        // TODO: fire event here
    }

    public void updateAcceptedOrdersToWorking() {
        for (Order order : orders.values()) {
            if (order.status == OrderStatusEnum.ACCEPTED) {
                order.status = OrderStatusEnum.WORKING;
                order.updatedAt = System.currentTimeMillis();
                order.createdAt = System.currentTimeMillis();

                orders.put(order.id, order);
            }
        }
    }

    public Order getParentOrder(Order order) {
        return order.parentOrder;
    }

    public boolean hasEnterPositionOrders(UUID strategyId) {
        for (Order order : orders.values()) {
            if (
                order.intent == OrderIntentEnum.OPEN &&
                order.strategyId == strategyId
            ) {
                return true;
            }
        }

        return false;
    }

    public float getCashBalanceFromOrders(String accountId) {
        float cash = 0.0f;

        for (Order order : orders.values()) {
            if (
                order.intent == OrderIntentEnum.OPEN &&
                order.accountId == accountId
            ) {
                cash += order.cashBalance;
            }
        }

        return cash;
    }

    public void updateOrdersWithPrice(PriceMovement price) {
        for (Order order : orders.values()) {
            if (
                order.symbol == price.symbol &&
                !ordersInFulfillment.containsKey(order.id)
            ) {
                order.marketValue = price.close * order.quantity;
            }
        }
    }
}
