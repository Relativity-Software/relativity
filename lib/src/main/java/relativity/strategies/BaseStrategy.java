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

package relativity.strategies;

import relativity.brokers.MarketHoursService;
import relativity.brokers.paper.AccountManager;
import relativity.brokers.paper.OrderManager;
import relativity.brokers.paper.PositionManager;
import relativity.brokers.types.*;
import relativity.instruments.InstrumentManager;
import relativity.instruments.signals.MarketValueService;
import relativity.workers.ThreadPool;
import org.tinylog.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Timer;
import java.util.UUID;

public abstract class BaseStrategy {
    public UUID id;
    public String name;
    public String accountId = "default";
    public String userId;

    /**************************************************************************
     * Symbols for trading
     *************************************************************************/

    public String symbol;
    /**
     * If multiple symbol trading strategy, this will be a list of symbols
     */
    public ArrayList<String> tradingSymbols = new ArrayList<>();

    // Timer is used for tracking time in the strategy
    // And ending the strategy if it runs too long
    public Timer timer = new Timer();

    /**************************************************************************
     * Injected Dependencies
     *************************************************************************/

    public InstrumentManager instrumentManager;
    public OrderManager orderManager;

    public AccountManager accountManager;
    public PositionManager positionManager;
    public ThreadPool pool;

    /**************************************************************************
     * Data Structures
     *************************************************************************/

    public StrategyConfig config = new StrategyConfig();
    public StrategyState state = new StrategyState();

    public BaseStrategy(String symbol) {
        this.symbol = symbol;
        id = UUID.randomUUID();
    }

    /**************************************************************************
     * Runnable Methods for StrategyHandlers
     *************************************************************************/

    public void preRun() {
        if (state.startedAt == null) {
            state.startedAt = LocalDateTime.now();
        }

        if (
            state.handler != null ||
            state.shortCircuit
        ) {
            state.resetStrategyRun();
        }

        state.replaceStatus = EnterPositionStatusEnum.READY;
        boolean hasPastPositions = positionManager.hasPastPositionsForStrategy(id);
        boolean hasPositions = positionManager.hasPositionsForStrategy(id);
        boolean hasOrders = orderManager.hasOrdersForStrategy(id);

        if (
            hasPastPositions &&
            (
                hasOrders ||
                hasPositions
            )
        ) {
            exitStrategy("Past positions exist and orders: " + hasOrders + " positions: " + hasPositions);

            return;
        }

        if (hasPastPositions) {
            state.status = StrategyStatusEnum.EXITED;

            return;
        }

        if (
            (
                state.status == StrategyStatusEnum.EXITED ||
                state.status == StrategyStatusEnum.EXITING
            ) &&
            !hasPositions &&
            !hasOrders
        ) {
            state.status = StrategyStatusEnum.EXITED;

            return;
        }

        if (
            state.status != StrategyStatusEnum.EXITED &&
            state.status != StrategyStatusEnum.EXITING
        ) {
            state.status = StrategyStatusEnum.WORKING;
        }

        if (!hasPositions) {
            state.enterPositionStatus = EnterPositionStatusEnum.READY;
        }

        if (symbol == null) {
            Logger.warn("Symbol not set for strategy " + name);

            state.shortCircuit = true;
            state.shortCircuitReason = "Symbol not set";

            exitStrategy("Symbol not set");
        }
    }

    public void checkIteration() {

    }

    public void checkExited() {
        // TODO: check if past maximumPositionDurationOfStrategy

        ArrayList<Position> positions = positionManager.getPositionsForInstrument(symbol, accountId);

        if (positions.size() == 0) {
            return;
        }

        Position position = positions.get(0);

        if (position.takeProfit == null) {
            position.takeProfit = MarketValueService.getTakeProfitThreshold(position, config.takeProfitRatio) - position.purchasedValue;
            position.stopLoss = MarketValueService.getStopLossThreshold(position, config.stopLossRatio) - position.purchasedValue;
        }

        long currentPositionDuration = System.currentTimeMillis() - position.createdAt;
        if (
            currentPositionDuration > config.maximumPositionDurationOfStrategy &&
            state.status != StrategyStatusEnum.EXITED &&
            state.status != StrategyStatusEnum.EXITING
        ) {
            Logger.info("Maximum position duration exceeded for strategy " + name);

            exitStrategy("Maximum position duration exceeded");
        }

        if (
            LocalDateTime.now().isAfter(LocalDateTime.of(LocalDate.now(), this.config.endTime)) &&
            state.status != StrategyStatusEnum.EXITED &&
            state.status != StrategyStatusEnum.EXITING
        ) {
            Logger.info("Exiting strategy " + name + " because specified strategy end time has passed");

            exitStrategy("Specified strategy end time has passed");
        }

        if (
//            LocalDateTime.now().minus().isLocalDateTime.of(LocalDate.now(), config.startTime)) > 30 &&
            LocalDateTime.now().isBefore(LocalDateTime.of(LocalDate.now(), config.startTime)) &&
            state.status != StrategyStatusEnum.EXITED &&
            state.status != StrategyStatusEnum.EXITING
        ) {
            Logger.info("Exiting strategy " + name + " because specified strategy start time has not yet passed");

            exitStrategy("Specified strategy start time has not yet passed");
        }
    }

    public void checkOrderLock() {
        // TODO: check if we need this anymore
        // With the synchronized lock for the strategy runner
        // we might be safe without order locks
    }

    public void checkProfit() {
        Float profits = accountManager.getAllProfits(accountId);
        Float startingBalance = accountManager.getStartingBalance(accountId);

        if (
            profits >= startingBalance * config.profitThresholdPercentage ||
            profits <= -startingBalance * config.lossThresholdPercentage
        ) {
            exitStrategy("Profit/loss threshold exceeded");
        }
    }

    public void replaceStopLossOrder() {
        ArrayList<Position> positions = positionManager.getPositionsForStrategy(id);

        for (Position position : positions) {
            Float price = instrumentManager.getBuyOrSellPrice(symbol);

            if (
                state.replaceStatus == EnterPositionStatusEnum.READY &&
                !state.orderlock &&
                orderManager.hasCloseOrderForPosition(position) &&
                Math.abs(price - position.filledAveragePrice) >= 0.01 &&
                stopLossCriteria(position)
            ) {
                try {
                    state.orderlock = true;
                    state.replaceStatus = EnterPositionStatusEnum.WORKING;

                    if (config.enableCircuitBreakers) {
                    }

                    Order closeOrder = orderManager.findClosePositionOrder(position);

                    if (
                        closeOrder != null &&
                        orderManager.isAbleToBeReplaced(closeOrder) &&
                        Math.abs(price - closeOrder.limitPrice) >= 0.01
                    ) {
                        OrderOptions orderOptions = new OrderOptions();
                        orderOptions.limitPrice = price;
                        orderOptions.symbol = symbol;
                        orderOptions.quantity = closeOrder.quantity;
                        orderOptions.intent = OrderIntentEnum.CLOSE;
                        orderOptions.side = closeOrder.side;
                        orderOptions.orderType = OrderTypeEnum.LIMIT;
                        orderOptions.limit = true;
                        orderOptions.accountId = accountId;
                        orderOptions.strategyId = id;

                        Order replaceOrder = orderManager.replaceOrder(closeOrder, orderOptions);

                        if (replaceOrder != null) {
                            position.orders.add(replaceOrder);
                            position.orders.remove(closeOrder);
                            state.ordersSubmitted = true;
                            state.orders.add(replaceOrder);
                        }
                    }

                } catch (Exception e) {
                    Logger.error("Error replacing stop loss order for position " + position.id + " " + e.getMessage());
                } finally {
                    state.replaceStatus = EnterPositionStatusEnum.READY;
                    state.orderlock = false;
                }
            }
        }
    }

    public void stopLoss() {
        ArrayList<Position> positions = positionManager.getPositionsForStrategy(id);

        for (Position position : positions) {
            Float price = instrumentManager.getBuyOrSellPrice(symbol);

            if (
                state.replaceStatus == EnterPositionStatusEnum.READY &&
                !state.orderlock &&
                Math.abs(price - position.filledAveragePrice) >= 0.01 &&
                !orderManager.hasCloseOrderForPosition(position) &&
                stopLossCriteria(position)
            ) {
                try {
                    state.orderlock = true;
                    state.replaceStatus = EnterPositionStatusEnum.WORKING;

                    if (config.enableCircuitBreakers) {

                    }

                    OrderOptions orderOptions = new OrderOptions();
                    orderOptions.limitPrice = price;
                    orderOptions.symbol = symbol;
                    orderOptions.quantity = position.quantity;
                    orderOptions.intent = OrderIntentEnum.CLOSE;
                    orderOptions.side = config.positionType == PositionTypeEnum.LONG
                        ? OrderSideEnum.SELL
                        : OrderSideEnum.BUY;
                    orderOptions.orderType = OrderTypeEnum.LIMIT;
                    orderOptions.limit = true;
                    orderOptions.accountId = accountId;
                    orderOptions.strategyId = id;
                    orderOptions.reason = "Stop Loss";

                    Order order = orderManager.createOrderIfNotExists(orderOptions);

                    if (order != null) {
                        position.orders.add(order);
                        state.ordersSubmitted = true;
                        state.orders.add(order);
                    }
                } catch (Exception e) {
                    Logger.error("Error replacing stop loss order for position " + position.id + " " + e.getMessage());
                } finally {
                    state.replaceStatus = EnterPositionStatusEnum.READY;
                    state.orderlock = false;
                }
            }
        }
    }

    public void replaceTakeProfitOrder() {
        ArrayList<Position> positions = positionManager.getPositionsForStrategy(id);

        for (Position position : positions) {
            Float price = instrumentManager.getBuyOrSellPrice(symbol);

            Criteria criteria = new Criteria();

            criteria.put("Replace Status", state.replaceStatus == EnterPositionStatusEnum.READY);
            criteria.put("Order Lock", !state.orderlock);
            criteria.put("Price Difference", Math.abs(price - position.filledAveragePrice) >= 0.01);
            criteria.put("Has Close Order", orderManager.hasCloseOrderForPosition(position));
            criteria.put("Replace Take Profit Criteria", replaceTakeProfitCriteria(position, price));

            if (criteria.get("Replace Take Profit Criteria")) {
//                Logger.info(criteria);
            }

            if (CriteriaService.allTrue(criteria)) {
                try {
                    state.orderlock = true;
                    state.replaceStatus = EnterPositionStatusEnum.WORKING;

                    if (config.enableCircuitBreakers) {
                    }

                    Order closeOrder = orderManager.findClosePositionOrder(position);

                    if (
                        closeOrder != null &&
                        orderManager.isAbleToBeReplaced(closeOrder) &&
                        Math.abs(price - closeOrder.limitPrice) >= 0.01
                    ) {
                        OrderOptions orderOptions = new OrderOptions();
                        orderOptions.limitPrice = price;
                        orderOptions.symbol = symbol;
                        orderOptions.quantity = closeOrder.quantity;
                        orderOptions.intent = OrderIntentEnum.CLOSE;
                        orderOptions.side = closeOrder.side;
                        orderOptions.orderType = OrderTypeEnum.LIMIT;
                        orderOptions.limit = true;
                        orderOptions.accountId = accountId;
                        orderOptions.strategyId = id;
                        orderOptions.reason = "Replace Take Profit";

                        Order replaceOrder = orderManager.replaceOrder(closeOrder, orderOptions);

                        if (replaceOrder != null) {
                            position.orders.add(replaceOrder);
                            position.orders.remove(closeOrder);
                            state.ordersSubmitted = true;
                            state.orders.add(replaceOrder);
                        }
                    }

                } catch (Exception e) {
                    Logger.error("Error replacing take profit order for position " + position.id + " " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    state.replaceStatus = EnterPositionStatusEnum.READY;
                    state.orderlock = false;
                }
            }
        }
    }

    public void takeProfit() {
        ArrayList<Position> positions = positionManager.getPositionsForStrategy(id);

        for (Position position : positions) {
            Float price = instrumentManager.getBuyOrSellPrice(symbol);

            Criteria criteria = new Criteria();

            criteria.put("Replace Status", state.replaceStatus == EnterPositionStatusEnum.READY);
            criteria.put("Order Lock", !state.orderlock);
            criteria.put("Price Difference", Math.abs(price - position.filledAveragePrice) >= 0.01);
            criteria.put("No Close Order", !orderManager.hasCloseOrderForPosition(position));
            criteria.put("Take Profit Criteria", takeProfitCriteria(position));

            if (CriteriaService.allTrue(criteria)) {
                try {
                    state.orderlock = true;
                    state.replaceStatus = EnterPositionStatusEnum.WORKING;

                    if (config.enableCircuitBreakers) {
                        // TODO: Determine if this is still necessary
                    }

                    OrderOptions orderOptions = new OrderOptions();
                    orderOptions.limitPrice = price;
                    orderOptions.symbol = symbol;
                    orderOptions.quantity = position.quantity;
                    orderOptions.intent = OrderIntentEnum.CLOSE;
                    orderOptions.side = config.positionType == PositionTypeEnum.LONG
                        ? OrderSideEnum.SELL
                        : OrderSideEnum.BUY;
                    orderOptions.orderType = OrderTypeEnum.LIMIT;
                    orderOptions.limit = true;
                    orderOptions.accountId = accountId;
                    orderOptions.strategyId = id;
                    orderOptions.reason = "Take Profit";

                    Order order = orderManager.createOrderIfNotExists(orderOptions);

                    if (order != null) {
                        position.orders.add(order);
                        state.ordersSubmitted = true;
                        state.orders.add(order);
                    }

                    state.orderlock = false;
                } catch (Exception e) {
                    Logger.error("Error replacing take profit order for position " + position.id + " " + e.getMessage());
                } finally {
                    state.orderlock = false;
                }
            }
        }
    }

    public void checkOldOrders() {
        Float price = instrumentManager.getLatestTrailingPrice(symbol);
        ArrayList<Order> orders = orderManager.findOrdersForStrategy(id);
        long currentTime = System.currentTimeMillis();

        for (Order order : orders) {
            long millisecondDifference = currentTime - order.createdAt;

            if (order.createdAt < currentTime - config.cancelOrderTimeThreshold) {

//                if (millisecondDifference > config.cancelOrderTimeThreshold) {
                    if (
                        config.exitImmediatelyIfEntryOrderNotFilled &&
                        order.intent == OrderIntentEnum.OPEN &&
                        (
                            order.side == OrderSideEnum.BUY
                                ? price > order.limitPrice
                                : price < order.limitPrice
                        )
                    ) {
                        Logger.info("Exiting strategy " + name + " because entry order not filled");

                        exitStrategy("Entry order not filled. Exiting immediately");

                        return;
                    }

                    if (Math.abs(price - order.limitPrice) > 0.0075) {
                        pool.runAsync(() -> orderManager.cancelOrder(order));

                        Logger.info("Cancelling order " + order.symbol + " because it is old. Price: " + price + " Limit: " + order.limitPrice + " Millisecond difference: " + millisecondDifference);
                    }
//                }
            }
        }
    }

    public void checkTradingHours() {

    }

    public void checkCircuitBreaker() {

    }

    public void enterPosition() {
        Float buyPrice = instrumentManager.getBuyOrSellPrice(symbol);

        if (
            !state.orderlock &&
            state.status != StrategyStatusEnum.EXITED &&
            state.status != StrategyStatusEnum.EXITING &&
            enterPositionCriteria() &&
            !orderManager.hasOrderForSymbol(symbol)
        ) {
            state.orderlock = true;

            // TODO: circuit breaker logic

            float quantity = StrategyPositionSizing.calculatePositionSize(this, buyPrice);

            if (quantity == 0) {
                state.orderlock = false;

                // TODO: Somehow wait until account balance is sufficient
                exitStrategy("Account balance not sufficient to enter position");

                return;
            }

            state.enterPositionStatus = EnterPositionStatusEnum.ENTER_POSITION;

            OrderOptions orderOptions = new OrderOptions();
            orderOptions.limitPrice = buyPrice;
            orderOptions.symbol = symbol;
            orderOptions.quantity = quantity;
            orderOptions.intent = OrderIntentEnum.OPEN;
            orderOptions.side = config.positionType == PositionTypeEnum.LONG
                ? OrderSideEnum.BUY
                : OrderSideEnum.SELL;
            orderOptions.orderType = OrderTypeEnum.LIMIT;
            orderOptions.limit = true;
            orderOptions.accountId = accountId;
            orderOptions.strategyId = id;
            orderOptions.reason = "Enter Position";

            if (
                config.useTriggerOrders &&
                !MarketHoursService.isInExtendedHours()
            ) {
                orderOptions.takeProfit.limitPrice = config.positionType == PositionTypeEnum.LONG
                    ? buyPrice * config.takeProfitRatio
                    : buyPrice * (1 - (config.takeProfitRatio - 1));

                if (
                    orderOptions.limitPrice != null &&
                    Math.abs(orderOptions.limitPrice - orderOptions.takeProfit.limitPrice) < 0.01
                ) {
                    orderOptions.takeProfit.limitPrice += config.positionType == PositionTypeEnum.LONG
                        ? 0.01f
                        : -0.01f;
                }
            }

            if (orderManager.hasOrdersForStrategy(id)) {
                state.orderlock = false;

                return;
            }

            if (config.splitOrderIntoLots && quantity > 100) {
                state.orders = orderManager.splitOrderIntoLots(orderOptions);
                state.handler = StrategyHandlers.ENTER_POSITION;
                state.ordersSubmitted = true;
                state.orderlock = false;

                Logger.info("Strategy " + name + " enter position with balance percentage to use " + config.balancePercentageToUse + " margin percent " + config.marginPercent + " quantity " + quantity + " buy price " + buyPrice + " limit price " + orderOptions.limitPrice + " take profit limit price " + orderOptions.takeProfit.limitPrice + " stop loss limit price " + orderOptions.stopLoss.limitPrice);

                return;
            }

            state.orders.add(orderManager.createOrderIfNotExists(orderOptions));
            state.handler = StrategyHandlers.ENTER_POSITION;
            state.ordersSubmitted = true;

            state.orderlock = false;
            state.enterPositionStatus = EnterPositionStatusEnum.READY;
        }
    }

    public void postRun() {

    }

    public void checkRunQueue() {

    }

    public void replaceEnterPositionOrder() {
        Float price = instrumentManager.getBuyOrSellPrice(symbol);

        ArrayList<Order> orders = orderManager.findEnterPositionOrders(id, accountId);

        for (Order order : orders) {
            if (
                state.replaceStatus == EnterPositionStatusEnum.READY &&
                !state.orderlock &&
                Math.abs(price - order.limitPrice) >= 0.01 &&
                orderManager.isAbleToBeReplaced(order)
            ) {
                try {
                    state.orderlock = true;
                    state.replaceStatus = EnterPositionStatusEnum.WORKING;

                    if (config.enableCircuitBreakers) {
                    }

                    long currentTime = System.currentTimeMillis();
                    long millisecondDifference = currentTime - order.createdAt;

                    // Must be greater than eight seconds
                    if (
                        millisecondDifference / 1000 > 8 &&
                        orderManager.isAbleToBeReplaced(order) &&
                        Math.abs(price - order.limitPrice) >= 0.01
                    ) {
                        OrderOptions orderOptions = new OrderOptions();
                        orderOptions.limitPrice = price;
                        orderOptions.symbol = symbol;
                        orderOptions.quantity = order.quantity;
                        orderOptions.intent = OrderIntentEnum.OPEN;
                        orderOptions.side = order.side;
                        orderOptions.orderType = OrderTypeEnum.LIMIT;
                        orderOptions.limit = true;
                        orderOptions.accountId = accountId;
                        orderOptions.strategyId = id;
                        orderOptions.reason = "Replace Enter Position";

                        Order replaceOrder = orderManager.replaceOrder(order, orderOptions);

                        if (replaceOrder != null) {
                            state.ordersSubmitted = true;
                            state.orders.add(replaceOrder);
                        }
                    }
                } catch (Exception e) {
                    Logger.error("Error replacing enter position order for strategy " + name + " " + e.getMessage());
                } finally {
                    state.replaceStatus = EnterPositionStatusEnum.READY;
                    state.orderlock = false;
                }
            }
        }
    }

    /**************************************************************************
     * Criteria Methods
     *************************************************************************/

    public boolean enterPositionCriteria() {
        throw new Error("Not implemented");
    }

    public boolean stopLossCriteria(Position position) {
        throw new Error("Not implemented");
    }

    public boolean takeProfitCriteria(Position position) {
        throw new Error("Not implemented");
    }

    public boolean replaceTakeProfitCriteria(Position position, Float price) {
        Criteria criteria = new Criteria();

        criteria.put("Take Profit", takeProfitCriteria(position));
        criteria.put("Sell Price", Math.abs(price - Math.floor(position.filledAveragePrice)) >= 0.01);

        return CriteriaService.allTrue(criteria);
    }

    public void determineSymbol() {
        symbol = symbol != null ? symbol : tradingSymbols.get(0);
    }

    /**************************************************************************
     * Helper Methods
     *************************************************************************/

    public void exitStrategy(String reason) {
        state.exitingReason = reason;

        // TODO: double check this - it could stop important functions from running
        state.shortCircuit = true;
        state.shortCircuitReason = "Exiting strategy because " + reason;

        if (state.status != StrategyStatusEnum.EXITED) {
            state.status = StrategyStatusEnum.EXITING;

            Logger.info("Exiting strategy " + name + " " + symbol + " because " + reason);
        }

        if (
            !positionManager.hasPositionsForStrategy(id) &&
            orderManager.hasOrdersForStrategy(id)
        ) {
            Logger.info("Cancelling orders for strategy " + name + " " + symbol + " because " + reason);
            ArrayList<Order> orders = orderManager.findOrdersForStrategy(id);

            for (Order order : orders) {
                pool.runAsync(() -> orderManager.cancelOrder(order));
            }

            return;
        }

        if (positionManager.hasPositionsForStrategy(id)) {
            Logger.info("Cancelling positions for strategy " + name + " " + symbol + " because " + reason);
            try {
                exitPositions();
            } catch (Exception e) {
                Logger.error("Error cancelling positions for strategy " + name + " " + symbol + " " + e.getMessage());
            }

            return;
        }

        // If we reach here, the strategy has no positions and no orders
        state.status = StrategyStatusEnum.EXITED;
    }

    public void exitPositions() {
        ArrayList<Position> positions = positionManager.getPositionsForStrategy(id);

        for (Position position : positions) {
            pool.runAsync(() -> positionManager.exitPosition(position, false));
        }
    }

    public boolean hasCompletelyExited() {
        return !positionManager.hasPositionsForStrategy(id) &&
            !orderManager.hasOrdersForStrategy(id) &&
            state.status == StrategyStatusEnum.EXITED;

    }

    public void setThresholdsOnPosition(Position position) {
        try {
            if (position.type == PositionTypeEnum.LONG) {
                position.stopLoss = position.purchasedValue * config.stopLossRatio;
                position.takeProfit = position.purchasedValue * config.takeProfitRatio;

                Logger.info("Setting thresholds on position " + position.symbol + " stop loss " + position.stopLoss + " take profit " + position.takeProfit);
                return;
            }

            position.stopLoss = position.purchasedValue - (position.purchasedValue * config.stopLossRatio - 1) + position.purchasedValue;
            position.takeProfit = position.purchasedValue - (position.purchasedValue * (config.takeProfitRatio - 1));

            Logger.info("Setting thresholds on position " + position.symbol + " stop loss " + position.stopLoss + " take profit " + position.takeProfit);
        } catch (Exception e) {
            Logger.error("Error setting thresholds on position " + position.symbol + " " + e.getMessage());
        }
    }
}
