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

package relativity.strategies.active;

import relativity.brokers.types.Position;
import relativity.brokers.types.PositionTypeEnum;
import relativity.instruments.PriceMovement;
import relativity.instruments.signals.MarketValueService;
import relativity.instruments.signals.MomentumService;
import relativity.instruments.types.MovementEnum;
import relativity.instruments.types.Instrument;
import relativity.strategies.CriteriaService;
import relativity.strategies.BaseStrategy;
import relativity.strategies.Criteria;
import relativity.strategies.StrategyHandlers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;
import org.tinylog.Logger;

public class MoverStrategy extends BaseStrategy {
    int stopLossHitCounter = 0;

    public MoverStrategy(String symbol) {
        super(symbol);

        name = "Mover Strategy";

        state.startedAt = LocalDateTime.now();

        config.profitMax = 10_000;
        config.takeProfitRatio = 1.0035F;
        config.stopLossRatio = 0.998F; // This could be updated for each strategy based on ATR

        config.positionType = PositionTypeEnum.LONG;

        config.startTime = LocalTime.of(3, 0);
        config.endTime = LocalTime.of(18, 0);

        config.cancelEnterPositionOrderTimeThreshold = 30_000;
        config.maximumPositionDurationOfStrategy = 120_000;
        config.cancelOrderTimeThreshold = 30_000;

        config.balancePercentageToUse = 0.3f;
        config.marginPercent = 0.08f;

        config.dynamicPositionSizing = true;
        config.useAverageVolume = true;
        config.useTriggerOrders = false;
        config.exitImmediatelyIfEntryOrderNotFilled = true;

        config.handlers = new ArrayList<>(Arrays.asList(
            StrategyHandlers.CHECK_ITERATION,
            StrategyHandlers.CHECK_EXITED,

            StrategyHandlers.CHECK_PROFIT,

            StrategyHandlers.REPLACE_STOP_LOSS_ORDER,
            StrategyHandlers.STOP_LOSS,
            StrategyHandlers.REPLACE_TAKE_PROFIT_ORDER,
            StrategyHandlers.TAKE_PROFIT,
    //                StrategyHandlers.REPLACE_ENTER_POSITION_ORDER,

            StrategyHandlers.CHECK_OLD_ORDERS,
            StrategyHandlers.CHECK_TRADING_HOURS,
            StrategyHandlers.CHECK_CIRCUIT_BREAKER,
            StrategyHandlers.ENTER_POSITION
        ));
    }

    @Override
    public void preRun() {
        super.preRun();

        Instrument instrument = instrumentManager.getInstrument(symbol);

//        if (
//            !config.stopLossRatioFreeze &&
//            security.statistics.volatility.atr != null
//            // TODO: only customize stop-loss during middle of trading day
//        ) {
//            config.stopLossRatio = 1 - (security.statistics.volatility.atrPercentage / 2.5f);
//
//            // TODO: determine a better way to smooth out ATR for stop loss
//            if (config.stopLossRatio < 0.96) {
//                config.stopLossRatio = 0.96f;
//            }
//
//            config.stopLossRatioFreeze = true;
//            Logger.info("Setting stop loss ratio to " + config.stopLossRatio + " for " + symbol + " from ATR% " + security.statistics.volatility.atrPercentage);
//        }

        if (state.startedAt.isBefore(LocalDateTime.now().minusMinutes(4))) {
            exitStrategy("Maximum strategy duration reached");
        }
    }

    public void initialize(int numberOfStrategies) {

        /******************************
         * Timers
         * ****************************/
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                preRun();
            }
        }, config.cancelEnterPositionOrderTimeThreshold);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                preRun();
            }
        }, config.maximumPositionDurationOfStrategy);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                exitStrategy("Maximum strategy duration reached");
            }
        }, Math.round(1000 * 60 * 1.5), 10_000); // 2 minutes and then repeat

        /******************************
         * Dynamic Sizing
         * ****************************/
        if (config.dynamicPositionSizing) {
            LocalDateTime now = LocalDateTime.now();

            if (
                (
                    now.isEqual(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 35))) ||
                    now.isAfter(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 35)))
                ) ||
                (
                    now.isAfter(LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 15))) &&
                    now.isBefore(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 25)))
                )
            ) {
                config.balancePercentageToUse = numberOfStrategies > 3
                    ? (float) 1 / (numberOfStrategies + 2)
                    : 0.2f;

                config.marginPercent = config.balancePercentageToUse / (numberOfStrategies + 2);
            }

            if (
                now.isAfter(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 25))) &&
                now.isBefore(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 35)))
            ) {
                config.balancePercentageToUse = 0.04f;
                config.marginPercent = 0.04f;
            }

            Logger.info("Using " + String.format("%.4f", config.balancePercentageToUse) + " of account balance. Strategies: " + numberOfStrategies);
        }
    }

    @Override
    public boolean enterPositionCriteria() {
        Instrument instrument = instrumentManager.getInstrument(symbol);
        PriceMovement priceMovement = (PriceMovement) MomentumService.getMovement(instrument, 2);

        Criteria criteria = new Criteria();

        criteria.put("Movement", config.positionType == PositionTypeEnum.LONG
            ? priceMovement.movement == MovementEnum.UP
            : priceMovement.movement == MovementEnum.DOWN);
        criteria.put("No Positions", !positionManager.hasPositionsForStrategy(id));
        criteria.put("No previous positions signal", !positionManager.hasPastPositionsForStrategy(id));
        criteria.put("No enter position orders", !orderManager.hasEnterPositionOrders(id));
        criteria.put("No past enter position orders", !orderManager.hasPastOpenOrdersForStrategy(id));

        // This helps with a bounce back action after a large movement
        // Sometimes helping with a better order entry position
        criteria.put("Waited 1.5 seconds", ChronoUnit.MILLIS.between(state.startedAt, LocalDateTime.now()) > 1_500);

        return CriteriaService.allTrue(criteria);
    }

    @Override
    public boolean stopLossCriteria(Position position) {
        Instrument instrument = instrumentManager.getInstrument(symbol);
        float latestTrailingPrice = instrumentManager.getLatestTrailingPrice(symbol);

        PriceMovement movement = (PriceMovement) MomentumService.getMovement(instrument, 2);

        Criteria criteria = new Criteria();
        criteria.put("Market value below threshold", MarketValueService.isMarketValueBelow(position, latestTrailingPrice, config.stopLossRatio));
        criteria.put("Stop Loss Hit Counter", stopLossHitCounter >= 25);
        criteria.put("Movement", config.positionType == PositionTypeEnum.LONG
            ? movement.movement == MovementEnum.DOWN
            : movement.movement == MovementEnum.UP || movement.movement == MovementEnum.NONE);

        if (criteria.get("Market value below threshold")) {
            stopLossHitCounter++;
        }

//        Logger.info("Stop Loss " + position.symbol);
//        Logger.info(criteria);

        return CriteriaService.allTrue(criteria);
    }

    @Override
    public boolean takeProfitCriteria(Position position) {
        Instrument instrument = instrumentManager.getInstrument(symbol);
        float latestTrailingPrice = instrumentManager.getLatestTrailingPrice(symbol);

        PriceMovement movement = (PriceMovement) MomentumService.getMovement(instrument, 2);

        Criteria criteria = new Criteria();
        criteria.put("Market vale above threshold", MarketValueService.isMarketValueAbove(position, latestTrailingPrice, config.takeProfitRatio));
        criteria.put("Movement", config.positionType == PositionTypeEnum.LONG
            ? movement.movement == MovementEnum.DOWN
            : movement.movement == MovementEnum.UP || movement.movement == MovementEnum.NONE);

//        Logger.info("Take Profit " + position.symbol);
//        Logger.info(criteria);

        return CriteriaService.allTrue(criteria);
    }

    @Override
    public void replaceEnterPositionOrder() {
        // No-op We will fill almost immediately or exit the strategy
    }
}
