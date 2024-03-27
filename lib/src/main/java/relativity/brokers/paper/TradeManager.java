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
import relativity.events.types.OrderFilledEvent;
import relativity.instruments.PriceMovement;
import relativity.instruments.InstrumentManager;
import relativity.instruments.types.Instrument;
import relativity.brokers.types.*;
import relativity.workers.ThreadPool;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    public InstrumentManager instrumentManager;
    public OrderManager orderManager;
    public TradeDecisionEngine tradeDecisionEngine = new TradeDecisionEngine();

    public EventService eventService;
    public ThreadPool pool;

    ConcurrentHashMap<String, String> ordersInFulfillment = new ConcurrentHashMap<>();

    public void checkOrder(@NotNull Order order, PriceMovement priceMovement) {
//        Logger.info("Checking order " + order.symbol + " " + " " + " " + order.quantity + " " + order.orderType + " " + order.limitPrice + " " + priceMovement.close + " " + order.side + " " + " " + order.accountId);

        if (
            !instrumentManager.hasPricing(order.symbol) ||
            !orderManager.isUnfulfilledOrder(order) ||
            ordersInFulfillment.containsKey(order.id)
        ) {
            return;
        }

        Instrument instrument = instrumentManager.getInstrument(order.symbol);

        if (
            tradeDecisionEngine.checkDefaultCriteria(order, instrument, priceMovement) //&&
//            !ordersInFulfillment.containsKey(order.id) //&&
//            order.filledQuantity < order.quantity
        ) {
            try {
                orderManager.ordersInFulfillment.put(order.id, order.id);

                OrderFill orderFill = new OrderFill();

                orderFill.id = UUID.randomUUID();
                orderFill.orderId = order.id;
                orderFill.price = getPriceFill(order, priceMovement);
                orderFill.quantity = order.quantity - order.filledQuantity;
                orderFill.accountId = order.accountId;
                orderFill.createdAt = System.currentTimeMillis();

                // TODO: This function might should live in the process event
                // before the async flow
                orderManager.fulfillOrder(order, new ArrayList<>(Arrays.asList(orderFill)));
                eventService.processEvent(new OrderFilledEvent(order));

                Logger.info(order.symbol + " Order filled: " + orderFill.price + " " + order.side + " " + orderFill.quantity + " " + order.filledQuantity);

                orderManager.ordersInFulfillment.remove(order.id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Float getPriceFill(@NotNull Order order, PriceMovement priceMovement) {
        Instrument instrument = instrumentManager.getInstrument(order.symbol);

        if (System.getenv("TRADE_MANAGER_RANDOM_PRICE") != "true") {
            if (
                order.type == OrderTypeEnum.LIMIT ||
                order.limitPrice != null
            ) {
                return order.limitPrice;

            }

            return priceMovement.close;
        }

        return getRandomPriceFill(order, instrument, priceMovement);
    }

    public Float getRandomPriceFill(@NotNull Order order, @NotNull Instrument instrument, @NotNull PriceMovement priceMovement) {
        float min = 0.0f;
        float max = 0.0f;

        float high = instrument.pricing.priceStreams.high.getLast();
        float low = instrument.pricing.priceStreams.low.getLast();

        float previousHigh = instrument.pricing.priceStreams.high.get(instrument.pricing.priceStreams.high.size() - 2);
        float previousLow = instrument.pricing.priceStreams.low.get(instrument.pricing.priceStreams.low.size() - 2);

        double percentExactPrice = Math.random() * 100;

        if (percentExactPrice < 30) {
            return order.limitPrice;
        }

        if (order.side == OrderSideEnum.BUY) {
            max = order.limitPrice;
            min = priceMovement.close < order.limitPrice
                ? priceMovement.close
                : low;

            if (min > order.limitPrice) {
                min = previousLow < order.limitPrice
                    ? previousLow
                    : order.limitPrice;
            }
        }

        if (order.side == OrderSideEnum.SELL) {
            max = priceMovement.close > order.limitPrice
                ? priceMovement.close
                : high;
            min = order.limitPrice;

            if (max < order.limitPrice) {
                max = previousHigh > order.limitPrice
                    ? previousHigh
                    : order.limitPrice;
            }
        }

        return (float) (Math.random() * (max - min) + min);
    }

    public void checkOrders(PriceMovement priceMovement) {
        for (Order order : orderManager.orders.values()) {

            // TODO: determine a better spot or event for
            // upgrading an order to working
            if (order.status == OrderStatusEnum.PENDING) {
                order.status = OrderStatusEnum.WORKING;
            }

            if (
                order.status == OrderStatusEnum.WORKING &&
                order.symbol == priceMovement.symbol
            ) {
                pool.runAsync(() -> checkOrder(order, priceMovement));
            }
        }
    }
}
