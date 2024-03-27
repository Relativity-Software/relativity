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

package relativity.instruments;

import relativity.events.EventService;
import relativity.instruments.data.polygon.types.PolygonQuote;
import relativity.instruments.data.polygon.types.PolygonTrade;
import relativity.workers.ThreadPool;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InstrumentQuoteAndTradeManager {
    public InstrumentManager instrumentManager;
    public InstrumentStatisticsService instrumentStatisticsService;
    public EventService eventService;
    public ThreadPool pool;
    private static final ConcurrentHashMap<String, EquityQuoteAndTradeProcessor> equityQuoteAndTradeMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, EquityQuoteAndTradeProcessor> equityQuoteAndTradeMinutesMap = new ConcurrentHashMap<>();

    protected Set<Integer> nonEligibleTradeQuoteUpdateConditions = Set.of(
            InstrumentTradeConditions.NON_ELIGIBLE_TRADE.value,
            InstrumentTradeConditions.NON_ELIGIBLE_EXTENDED_HOURS_TRADE.value,
            InstrumentTradeConditions.CANCELLED.value,
            InstrumentTradeConditions.RECOVERY.value,
            InstrumentTradeConditions.CORRECTION.value,
            InstrumentTradeConditions.AS_OF.value,
            InstrumentTradeConditions.AS_OF_CORRECTION.value,
            InstrumentTradeConditions.AS_OF_CANCEL.value,
            InstrumentTradeConditions.OOB.value,
            InstrumentTradeConditions.SUMMARY.value,
            InstrumentTradeConditions.ERRORED.value,
            InstrumentTradeConditions.OPENING_REOPENING_TRADE_DETAIL.value,
            InstrumentTradeConditions.PLACEHOLDER.value,
            InstrumentTradeConditions.PLACEHOLDER_FOR_611_EXEMPT.value,
            InstrumentTradeConditions.AVERAGE_PRICE_TRADE.value,
            InstrumentTradeConditions.CASH_SALE.value,
            InstrumentTradeConditions.MARKET_CENTER_OFFICIAL_CLOSE.value,
            InstrumentTradeConditions.MARKET_CENTER_OFFICIAL_OPEN.value,
            InstrumentTradeConditions.PRICE_VARIATION_TRADE.value
    );
    protected Set<Integer> eligibleTradeQuoteUpdateConditions = Set.of(
            InstrumentTradeConditions.REGULAR_SALE.value,
            InstrumentTradeConditions.QUALIFIED_CONTINGENT.value,
            InstrumentTradeConditions.AVERAGE_PRICE_TRADE.value,
            InstrumentTradeConditions.ACQUISITION.value,
            InstrumentTradeConditions.AUTOMATIC_EXECUTION.value,
            InstrumentTradeConditions.BUNCHED_TRADE.value,
            InstrumentTradeConditions.BUNCHED_SOLD_TRADE.value,
            InstrumentTradeConditions.CONTINGENT.value,
            InstrumentTradeConditions.CLOSING_PRINTS.value,
            InstrumentTradeConditions.CROSS_TRADE.value,
            InstrumentTradeConditions.DERIVATIVELY_PRICED.value,
            InstrumentTradeConditions.DISTRIBUTION.value,
            InstrumentTradeConditions.INTERMARKET_SWEEP.value,
            InstrumentTradeConditions.PRIOR_REFERENCE_PRICE.value,
            InstrumentTradeConditions.RULE_155_TRADE.value,
            InstrumentTradeConditions.OPENING_PRINTS.value,
            InstrumentTradeConditions.STOPPED_STOCK_REGULAR_TRADE.value,
            InstrumentTradeConditions.REOPENING_PRINTS.value,
//            SecurityTradeConditions.SELLER.value,
            InstrumentTradeConditions.SOLD_OUT.value,
            InstrumentTradeConditions.SOLD_LAST.value,
            InstrumentTradeConditions.SOLD_OUT_OF_SEQUENCE.value,
            InstrumentTradeConditions.SPLIT_TRADE.value,
            InstrumentTradeConditions.YELLOW_FLAG_REGULAR_TRADE.value,
            InstrumentTradeConditions.CORRECTED_CONSOLIDATED_CLOSE_PRICE_AS_PER_LISTING_MARKET.value,
            InstrumentTradeConditions.FORM_T.value,
            InstrumentTradeConditions.EXTENDED_HOURS_TRADE_SOLD_OUT_OF_SEQUENCE.value,
            InstrumentTradeConditions.ODD_LOT_TRADE.value,
            InstrumentTradeConditions.SSR.value,
            InstrumentTradeConditions.MARKET_CENTER_OFFICIAL_OPEN.value,
            InstrumentTradeConditions.MARKET_CENTER_OPENING.value,
            InstrumentTradeConditions.MARKET_CENTER_OFFICIAL_CLOSE.value,
            InstrumentTradeConditions.MARKET_CENTER_CLOSING.value,
            InstrumentTradeConditions.MARKET_CENTER_REOPENING.value,
            InstrumentTradeConditions.CASH_SALE.value,
            InstrumentTradeConditions.NEXT_DAY.value
            // SecurityCondition.STOCK_OPTION_TRADE,
    );

    public InstrumentQuoteAndTradeManager() {}

    public void processTradeData(List<PolygonTrade> trades) {
        for (final PolygonTrade trade : trades) {
            if (trade != null) {
                addTrade(trade);
            }
        }
    }

    public void processQuoteData(List<PolygonQuote> quotes) {
        for (final PolygonQuote quote : quotes) {
            if (quote != null) {
                addQuote(quote);
            }
        }
    }

    public void addTrade(final @NotNull PolygonTrade trade) {
        try {
            if (trade.c != null) {
                for (Number condition : trade.c) {
                    if (
                        !condition.equals(2) &&
                        !condition.equals(7) &&
                        !condition.equals(9) &&
                        !condition.equals(10) &&
                        !condition.equals(12) &&
                        !condition.equals(13) &&
                        !condition.equals(14) &&
                        !condition.equals(16) &&
                        !condition.equals(17) &&
                        !condition.equals(18) &&
                        !condition.equals(20) &&
                        !condition.equals(22) &&
                        !condition.equals(29) &&
                        !condition.equals(32) &&
                        !condition.equals(41) &&
                        !condition.equals(37) &&
                        !condition.equals(52) &&
                        !condition.equals(53) &&
                        !condition.equals(60) &&
                        !condition.equals(63) &&
                        !condition.equals(64) &&
                        !condition.equals(67) &&
                        !condition.equals(70)
                    ) {
                        Logger.info(trade.sym + " " + condition);
                    }

                    if (!eligibleTradeQuoteUpdateConditions.contains(condition)) {
//                        if (!condition.equals(41) && !condition.equals(63)) {
//                            Logger.info(trade.sym + " Not adding trade for condition " + condition + " " + trade.p + " " + trade.s);
//                        }

                        return;
                    }
                }
            }

            equityQuoteAndTradeMap
                .computeIfAbsent(
                    trade.sym,
                    symbol -> new EquityQuoteAndTradeProcessor(
                        symbol,
                    instrumentManager,
                        pool,
                        eventService,
                    instrumentStatisticsService
                    )
                )
                .aggregateTrade(trade);
        } catch (Error e) {
            Logger.info(e.getMessage() + " " + e.getCause());
            Logger.info(trade);
        }
    }

    public void addQuote(final @NotNull PolygonQuote quote) {
        equityQuoteAndTradeMap
        .computeIfAbsent(
            quote.sym,
            symbol -> new EquityQuoteAndTradeProcessor(
                symbol,
            instrumentManager,
                pool,
                eventService,
            instrumentStatisticsService
            )
        )
        .aggregateQuote(quote);
    }
}
