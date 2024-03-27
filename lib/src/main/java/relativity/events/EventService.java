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

package relativity.events;

import relativity.brokers.paper.AccountManager;
import relativity.brokers.paper.OrderManager;
import relativity.brokers.paper.PositionManager;
import relativity.brokers.paper.TradeManager;
import relativity.instruments.InstrumentManager;
import relativity.instruments.InstrumentQuoteAndTradeManager;
import relativity.events.types.*;
import relativity.strategies.BaseStrategy;
import relativity.strategies.StrategyManager;
import relativity.strategies.listeners.MoverStrategyListener;
import relativity.workers.ThreadPool;
import org.tinylog.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Service to process events
 * and route them to the appropriate handler
 */
public class EventService {

    public StrategyManager strategyManager;
    public final MoverStrategyListener moverStrategyListener = new MoverStrategyListener();

    /**************************************************************************
     * Injected Dependencies
     *************************************************************************/

    public InstrumentManager instrumentManager;
    public AccountManager accountManager;
    public OrderManager orderManager;
    public PositionManager positionManager;
    public TradeManager tradeManager;
    public InstrumentQuoteAndTradeManager instrumentQuoteAndTradeManager;
    public ThreadPool pool;

    public EventService() {
        moverStrategyListener.eventService = this;
    }

    public void processEvent(BaseEvent event) {
        Logger.info("Unknown event type");
    }

    public void processEvent(InstrumentAnalysisEvent event) {
        try {
            CompletableFuture.allOf(
                pool.runAsync(() -> moverStrategyListener.second(
                    event.instrument,
                    event.lastTradeAggregate
                )),
                pool.runAsync(() -> strategyManager.newAnalysis(event.instrument))// ,
    //            pool.runAsync(() -> pennyBreakoutStrategyListener.minute(event.security, event.lastTradeAggregate))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Logger.info("Processing Security Analysis Event");
    }

    public void processEvent(InstrumentMinuteAnalysisEvent event) {
        CompletableFuture.allOf(
            pool.runAsync(() -> moverStrategyListener.minute(
                event.instrument,
                event.lastTradeAggregate
            ))
        );
    }

    public void processEvent(InstrumentPriceChangeEvent event) throws ExecutionException, InterruptedException {
        try {
            CompletableFuture.allOf(
                pool.runAsync(() -> orderManager.updateOrdersWithPrice(event.priceMovement)),
                pool.runAsync(() -> positionManager.updatePositionsWithPrice(event.priceMovement)),
                pool.runAsync(() -> tradeManager.checkOrders(event.priceMovement))
            );

        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

//        Logger.info("Processing Security Price Change Event");
    }

    public void processEvent(ActivateStrategyEvent event) {
        pool.runAsync(() -> {
            strategyManager.addStrategy(
                event.strategyName,
                event.instrument,
                event.positionType
            );
        });
    }

    public void processEvent(PositionOpenedEvent event) {
        CompletableFuture.allOf(
            pool.runAsync(() -> {
                BaseStrategy strategy = strategyManager.getStrategy(event.position.strategyId);

                if (strategy != null) {
                    strategy.setThresholdsOnPosition(event.position);
                }
            })
        );

        Logger.info(event.position.type + " " + event.position.symbol + " Created: " + event.position.quantity + " " + event.position.filledAveragePrice + " " + event.position.status + " " + event.position.marketValue);
    }

    public void processEvent(PositionClosedEvent event) {
        CompletableFuture.allOf(
            pool.runAsync(() -> accountManager.settlePosition(event.position)),
            pool.runAsync(() -> strategyManager.exitStrategy(event.position.strategyId))
        );

        Logger.info(event.position.symbol + " " + event.position.type + " Position Closed: $" + String.format("%.2f", event.position.realizedProfit) + " Price: $" + String.format("%.2f", event.position.filledAveragePrice) + " Closed Price: $" + String.format("%.2f", event.position.closedAveragePrice) + " purchasedValue " + event.position.purchasedValue + " closedValue " + event.position.closedValue);
        Logger.info("Wins " + positionManager.wins + " Losses " + positionManager.losses + " out of " + positionManager.pastPositions.size());
    }

    public void processEvent(OrderCreatedEvent event) {
        pool.runAsync(() -> accountManager.updateBalancesFromOpenOrder(event.order));

        Logger.info(event.order.symbol + " Order created " + event.order.side + " " + event.order.intent + " " + event.order.limitPrice + " " + event.order.quantity + " " + event.order.reason);
    }

    public void processEvent(OrderFilledEvent event) {
        CompletableFuture.allOf(
            pool.runAsync(() -> accountManager.settleOrderFill(event.order)),
            pool.runAsync(() -> positionManager.updatePositionsFromOrder(event.order))
        );

//        Logger.info("Processing Order Filled Event");
    }

    public void processEvent(TradeEvent event) {
        pool.runAsync(() -> instrumentQuoteAndTradeManager.processTradeData(event.trades));
    }

    public void processEvent(QuoteEvent event) {
//        Logger.info("Processing Quote Event");
        pool.runAsync(() -> instrumentQuoteAndTradeManager.processQuoteData(event.quotes));
    }

    // TODO: Also need an order fill event for partial fills
}
