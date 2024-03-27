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
import relativity.events.types.PositionClosedEvent;
import relativity.events.types.PositionOpenedEvent;
import relativity.instruments.PriceMovement;
import relativity.instruments.InstrumentManager;
import org.tinylog.Logger;
import relativity.brokers.types.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PositionManager {
    public ConcurrentHashMap<String, Position> positions = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Position> pastPositions = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, String> positionStrategies = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, String> positionsInFulfillment = new ConcurrentHashMap<>();

    public InstrumentManager instrumentManager;
    public OrderManager orderManager;
    public EventService eventService;

    public int wins = 0;
    public int losses = 0;

    public void removePosition(String positionId) {
        Position position = positions.remove(positionId);
        position.status = PositionStatusEnum.CLOSED;
        position.closedAt = System.currentTimeMillis();

        pastPositions.put(positionId, position);
        positionStrategies.remove(positionId);

        eventService.processEvent(new PositionClosedEvent(position));
    }

    public void removePosition(Position position) {
        positions.remove(position.id);
        position.status = PositionStatusEnum.CLOSED;
        position.closedAt = System.currentTimeMillis();

        pastPositions.put(position.id, position);
        positionStrategies.remove(position.id);

        if (position.realizedProfit > 5) {
            wins++;
        }
        // Anything within $10 is breakeven
        if (position.realizedProfit < -5) {
            losses++;
        }

        eventService.processEvent(new PositionClosedEvent(position));
    }

    public void addPosition(Position position) {
        positions.put(position.id, position);
    }

    public ArrayList<Position> getPositionsForInstrument(String symbol) {
        ArrayList<Position> instrumentPositions = new ArrayList<>();

        for (Position position : positions.values()) {
            if (position.symbol.equals(symbol)) {
                instrumentPositions.add(position);
            }
        }

        return instrumentPositions;
    }

    public boolean hasPositionsForStrategy(UUID strategyId) {
        for (Position position : positions.values()) {
            if (position.strategyId.equals(strategyId)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPastPositionsForStrategy(UUID strategyId) {
        for (Position position : pastPositions.values()) {
            if (position.strategyId.equals(strategyId)) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Position> getPositionsForInstrument(String symbol, String accountId) {
        ArrayList<Position> instrumentPositions = new ArrayList<>();

        for (Position position : positions.values()) {
            if (
                position.symbol.equals(symbol) &&
                position.accountId.equals(accountId)
            ) {
                instrumentPositions.add(position);
            }
        }

        return instrumentPositions;
    }

    public ArrayList<Position> getPositionsForStrategy(UUID strategyId) {
        ArrayList<Position> strategyPositions = new ArrayList<>();

        for (Position position : positions.values()) {
            if (position.strategyId.equals(strategyId)) {
                strategyPositions.add(position);
            }
        }

        return strategyPositions;
    }

    public ArrayList<Position> getPositionsForAccount(String accountId) {
        ArrayList<Position> accountPositions = new ArrayList<>();

        for (Position position : positions.values()) {
            if (position.accountId.equals(accountId)) {
                accountPositions.add(position);
            }
        }

        return accountPositions;
    }

    public ArrayList<Position> getPositionsForSecurities(ArrayList<String> symbols) {
        ArrayList<Position> positions = new ArrayList<>();

        for (String symbol : symbols) {
            positions.addAll(getPositionsForInstrument(symbol));
        }

        return positions;
    }

    public Position findPosition(String symbol, Float quantity, String accountId) {
        for (Position position : positions.values()) {
            if (
                position.symbol.equals(symbol) &&
                position.quantity.equals(quantity) &&
                position.accountId.equals(accountId)
            ) {
                return position;
            }
        }

        return null;
    }

    public Position findPosition(String symbol, String accountId) {
        for (Position position : positions.values()) {
            if (
                position.symbol.equals(symbol) &&
                position.accountId.equals(accountId)
            ) {
                return position;
            }
        }

        return null;
    }

    public Position findPositionForOrder(String orderId) {
        for (Position position : positions.values()) {
            for (Order order : position.orders) {
                if (order.id.equals(orderId)) {
                    return position;
                }
            }
        }

        return null;
    }

    public Position findPositionForOrder(String orderId, String accountId) {
        for (Position position : positions.values()) {
            for (Order order : position.orders) {
                if (
                    order.id.equals(orderId) &&
                    position.accountId.equals(accountId)
                ) {
                    return position;
                }
            }
        }

        return null;
    }

    public Position findPositionForOrder(Order order) {
        for (Position position : positions.values()) {
            for (Order positionOrder : position.orders) {
                if (positionOrder.id.equals(order.id)) {
                    return position;
                }
            }
        }

        return null;
    }

    public Position findShortPositionForOrder(Order order) {
        for (Position position : positions.values()) {
            if (
                position.symbol.equals(order.symbol) &&
                position.accountId.equals(order.accountId) &&
                position.type.equals(PositionTypeEnum.SHORT)
            ) {
                return position;
            }
        }

        return null;
    }

    public Position findLongPositionForOrder(Order order) {
        for (Position position : positions.values()) {
            if (
                position.symbol.equals(order.symbol) &&
                position.accountId.equals(order.accountId) &&
                position.type.equals(PositionTypeEnum.LONG)
            ) {
                return position;
            }
        }

        return null;
    }

    public Position addCloseOrderToPosition(Order order) {
        Position position = findPositionForOrder(order);
        position.orders.add(order);

        return position;
    }

    public void setMarketValueForPosition(Position position, Float latestPrice) {
        position.marketValue = latestPrice * position.quantity;

        position.unrealizedProfit = position.type == PositionTypeEnum.LONG
            ? (latestPrice - position.filledAveragePrice) * position.quantity
            : (position.filledAveragePrice - latestPrice) * position.quantity;

        if (position.unrealizedProfit > position.takeProfit) {
            Logger.info(position.symbol + " " + position.type + " " + position.status + " Position Market Value: " + position.marketValue + " Unrealized Profit: " + position.unrealizedProfit + " Take profit: " + position.takeProfit + " Stop loss: " + position.stopLoss);
        }
    }

    public void setMarketValueForPosition(Position position) {
//        Float latestPrice = securityManager.getLatestTrailingPrice(position.symbol);
//        position.marketValue = latestPrice * position.quantity;

//        position.unrealizedProfit = position.type == PositionType.LONG
//            ? (latestPrice - position.filledAveragePrice) * position.quantity
//            : (position.filledAveragePrice - latestPrice) * position.quantity;
    }

    public Float getPositionsValue(String accountId) {
        Float value = 0.0f;

        for (Position position : positions.values()) {
            if (position.accountId.equals(accountId)) {
                value += position.marketValue;
            }
        }

        return value;
    }

    public Float getPositionsCashBalance(String accountId) {
        Float value = 0.0f;

        for (Position position : positions.values()) {
            if (position.accountId.equals(accountId)) {
                value += position.cashBalance;
            }
        }

        return value;
    }

    public void updatePositions(String symbol, Float latestPrice) {
        for (Position position : positions.values()) {
            if (
                position.status == PositionStatusEnum.OPEN &&
                position.symbol.equals(symbol)
            ) {
                setMarketValueForPosition(position, latestPrice);
            }
        }
    }

    public ArrayList<Order> exitPosition(Position position, boolean flat) {
        Logger.info("About to try to exit position " + position.symbol);

        ArrayList<Order> exitOrders = new ArrayList<>();
        ArrayList<Order> ordersToCancel = new ArrayList<>(); // TODO: is this necessary

        Float price = instrumentManager.getLatestTrailingPrice(position.symbol);

        Logger.info("Fetching orders for position " + position.symbol);
        ArrayList<Order> closingOrders = orderManager.getClosingOrdersForPosition(position);

        Float sumQuantity = closingOrders.stream().reduce(0.0f, (acc, order) -> acc + order.quantity, Float::sum);
        Float differenceQuantity = sumQuantity > 0
            ? position.quantity - sumQuantity
            : 0;

        // TODO: We aren't properly creating an order if one doesn't exist

        // TODO: In some cases, to flat a position should do a market order

        if (differenceQuantity > 0 && closingOrders.size() == 0) {
            OrderOptions exitOptions = new OrderOptions();
            exitOptions.quantity = differenceQuantity;
            exitOptions.intent = OrderIntentEnum.CLOSE;
            exitOptions.symbol = position.symbol;
            exitOptions.accountId = position.accountId;
            exitOptions.limitPrice = price;
            exitOptions.orderType = OrderTypeEnum.LIMIT;
            exitOptions.reason = "Exit position";
            exitOptions.strategyId = position.strategyId;
            exitOptions.side = position.type == PositionTypeEnum.LONG
                ? OrderSideEnum.SELL
                : OrderSideEnum.BUY;

            exitOrders.add(orderManager.createOrderIfNotExists(exitOptions));
        }


        if (closingOrders.size() > 0) {
            for (Order order : closingOrders) {
                OrderOptions exitOptions = new OrderOptions();
                exitOptions.quantity = differenceQuantity;
                exitOptions.intent = OrderIntentEnum.CLOSE;
                exitOptions.symbol = position.symbol;
                exitOptions.accountId = position.accountId;
                exitOptions.limitPrice = price;
                exitOptions.orderType = OrderTypeEnum.LIMIT;
                exitOptions.reason = "Exit position";
                exitOptions.strategyId = position.strategyId;
                exitOptions.side = order.side;

                Logger.info("Replacing closing order for " + position.symbol);
                exitOrders.remove(order);
                exitOrders.add(orderManager.replaceOrder(order, exitOptions));
            }
        } else if (closingOrders.size() == 0) {
            OrderOptions exitOptions = new OrderOptions();
            exitOptions.quantity = position.quantity;
            exitOptions.intent = OrderIntentEnum.CLOSE;
            exitOptions.symbol = position.symbol;
            exitOptions.accountId = position.accountId;
            exitOptions.limitPrice = price;
            exitOptions.orderType = OrderTypeEnum.LIMIT;
            exitOptions.reason = "Exit position";
            exitOptions.strategyId = position.strategyId;
            exitOptions.side = position.type == PositionTypeEnum.LONG
                ? OrderSideEnum.SELL
                : OrderSideEnum.BUY;

            Logger.info("Creating closing order for " + position.symbol);
            exitOrders.add(orderManager.createOrderIfNotExists(exitOptions));
        }

        Logger.info("Created " + exitOrders.size() + " to exit position for " + position.symbol);

        position.liquidateLock = true;

        return exitOrders;
    }

    public Position getLatestPosition(String symbol) {
        Position latestPosition = null;

        for (Position position : pastPositions.values()) {
            if (position.symbol.equals(symbol)) {
                if (latestPosition == null) {
                    latestPosition = position;

                    continue;
                }

                if (position.createdAt > latestPosition.createdAt) {
                    latestPosition = position;
                }
            }
        }

        return latestPosition;
    }

    public void updatePositionsFromOrder(Order order) {
        if (order.side == OrderSideEnum.BUY) {
            if (order.intent == OrderIntentEnum.CLOSE) {
                Position position = findShortPositionForOrder(order);

                if (position != null) {
                    closePositionFromOrder(position, order);
                }

                throw new Error("No short position found to close for buy order: " + order.symbol + " " + order.id + " " + order.quantity);
            }

            createPositionFromOrder(order);
        }

        if (order.side == OrderSideEnum.SELL) {
            if (order.intent == OrderIntentEnum.CLOSE) {
                Position position = findLongPositionForOrder(order);

                if (position != null) {
                    closePositionFromOrder(position, order);
                }

                throw new Error("No long position found to close for sell order: " + order.symbol + " " + order.id + " " + order.quantity);
            }

            createPositionFromOrder(order);
        }
    }

    public void closePositionFromOrder(Position position, Order order) {
        position.quantity = position.quantity - order.quantity;
        position.filledQuantity += order.quantity; // TODO: this should be a closedFilledQuantity
        position.closedAveragePrice = order.filledAveragePrice;

        if (position.quantity != 0) {
            Logger.info("Position not fully closed: " + position.id + " " + position.symbol + " " + position.quantity);

            position.quantity = 0.0f;
        }

        if (position.quantity == 0) {
            position.closedValue = order.filledAveragePrice * order.quantity;
            position.marketValue = 0.0f;

            Logger.info("Calculating realized profit for " + position.symbol + " " + position.type);
            position.realizedProfit = position.type == PositionTypeEnum.LONG
                ? position.closedValue - position.purchasedValue
                : position.purchasedValue - position.closedValue;

            position.orders.add(order);
            position.durationSeconds = (System.currentTimeMillis() - position.createdAt) / 1000;

            removePosition(position);
        }

    }

    public void createPositionFromOrder(Order order) {
        boolean longPosition = order.intent == OrderIntentEnum.OPEN && order.side == OrderSideEnum.BUY;

        Position position = new Position();
        position.id = UUID.randomUUID().getLeastSignificantBits() + "";
        position.symbol = order.symbol;
        position.accountId = order.accountId;
        position.strategyId = order.strategyId;
        position.type = !longPosition
            ? PositionTypeEnum.SHORT
            : PositionTypeEnum.LONG;
        position.quantity = order.quantity;
        position.filledQuantity = order.quantity;
        position.filledAveragePrice = order.filledAveragePrice;
        position.purchasedValue = order.filledAveragePrice * order.quantity; // Should I make this negative if short?
        position.createdAt = System.currentTimeMillis();
        position.status = PositionStatusEnum.OPEN;

        position.marginBalance = order.marginBalance;
        position.cashBalance = order.cashBalance;
        position.marketValue = position.purchasedValue;
        position.unrealizedProfit = 0.0f;
        position.realizedProfit = 0.0f;
        position.positionPercent = 0.0f;

        position.orders.add(order);

        addPosition(position);

        eventService.processEvent(new PositionOpenedEvent(position));
    }

    public void updatePositionsWithPrice(PriceMovement priceMovement) {
        for (Position position : positions.values()) {
            if (
                position.symbol.equals(priceMovement.symbol) &&
                position.status == PositionStatusEnum.OPEN &&
                !positionsInFulfillment.containsKey(position.id)
            ) {
                setMarketValueForPosition(position, priceMovement.close);
            }
        }
    }
}
