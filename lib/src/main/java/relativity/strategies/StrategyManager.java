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

import relativity.brokers.paper.AccountManager;
import relativity.brokers.paper.OrderManager;
import relativity.brokers.paper.PositionManager;
import relativity.brokers.types.PositionTypeEnum;
import relativity.instruments.InstrumentManager;
import relativity.instruments.types.Instrument;
import relativity.strategies.active.MoverStrategy;
import relativity.workers.ThreadPool;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.tinylog.Logger;

public class StrategyManager {
    ConcurrentHashMap<String, MoverStrategy> activeStrategies = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, MoverStrategy> runningStrategies = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, MoverStrategy> waitingStrategies = new ConcurrentHashMap<>();
    int maxConcurrentStrategies = 20;
    final Object lock = new Object();

    /**************************************************************************
     * Injected Dependencies
     *************************************************************************/
    public InstrumentManager instrumentManager;
    public OrderManager orderManager;
    public AccountManager accountManager;
    public PositionManager positionManager;
    public StrategyRunner strategyRunner = new StrategyRunner();
    public ThreadPool pool;

    float averageNumberOfStrategies = 0;

    public void newAnalysis(Instrument instrument) {
        if (activeStrategies.containsKey(instrument.symbol)) {
            MoverStrategy strategy = activeStrategies.get(instrument.symbol);

            pool.runAsync(() -> checkStrategy(strategy, instrument));
        }
    }

//    public void checkStrategy(BaseStrategy strategy, Security security) {
//        // TODO: Anything we need to do before running the strategy
//        if (strategy.hasCompletelyExited()) {
//            removeStrategy(strategy);
//
//            return;
//        }
//
//        strategyRunner.run(strategy, security);
//    }

    public void checkStrategy(MoverStrategy strategy, Instrument instrument) {
        if (strategy.hasCompletelyExited()) {
            removeStrategy(strategy);

            return;
        }

        strategyRunner.run(strategy);
    }

    public void addStrategy(String strategyName, Instrument instrument, PositionTypeEnum positionType) {
//        try {
        synchronized (lock) {
            if (activeStrategies.containsKey(instrument.symbol)) {
                Logger.info("Strategy already exists for " + instrument.symbol + " Strategy Name: " + strategyName);

                return;
            }

//            String className = "strategies.active." + strategyName;
//            Class<?> classObject = Class.forName(className);
//
//            MoverStrategy strategy = (MoverStrategy) classObject
//                .getConstructor(String.class)
//                .newInstance(security.symbol);

            MoverStrategy strategy = new MoverStrategy(instrument.symbol);

            strategy.instrumentManager = instrumentManager;
            strategy.orderManager = orderManager;
            strategy.accountManager = accountManager;
            strategy.positionManager = positionManager;
            strategy.pool = pool;

            strategy.config.positionType = positionType;
            strategy.accountId = "default";

            if (activeStrategies.size() >= maxConcurrentStrategies) {
                Logger.info("Max concurrent strategies reached. Orders: " + orderManager.orders.size() + " Positions: " + positionManager.positions.size() + " Strategies: " + activeStrategies.size());

                strategy.state.startedAt = null;
                // Disable this for now
//                waitingStrategies.put(strategy.symbol, strategy);

                return;
            }

            // Fully activate new strategy
            activeStrategies.put(instrument.symbol, strategy);
            strategy.initialize(activeStrategies.size());
            checkStrategy(strategy, instrument);
//        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
//            e.printStackTrace();
//        }
        }
    }

    public void exitStrategy(MoverStrategy strategy) {
        if (strategy.hasCompletelyExited()) {
            removeStrategy(strategy);

            return;
        }

        strategy.exitStrategy("");
    }

    public void exitStrategy(UUID strategyId) {
        MoverStrategy strategy = activeStrategies.get(strategyId);

        if (strategy != null) {
            if (strategy.hasCompletelyExited()) {
                removeStrategy(strategy);

                return;
            }

            strategy.exitStrategy("");
        }
    }

    public void removeStrategy(BaseStrategy strategy) {
        activeStrategies.remove(strategy.symbol);
        strategyRunner.removeMethodMap(strategy);
        runningStrategies.remove(strategy.id);

        Logger.info("Removed strategy " + strategy.symbol + " Strategies left: " + activeStrategies.size());

        // Remove old strategies
        for (MoverStrategy waitingStrategy : waitingStrategies.values()) {
            if (waitingStrategy.state.startedAt.isBefore(LocalDateTime.now().minusSeconds(20))) {
                waitingStrategies.remove(waitingStrategy.symbol);

                Logger.info("Removed waiting strategy " + waitingStrategy.symbol);
            }
        }

        if (
            waitingStrategies.size() > 0 &&
            activeStrategies.size() < maxConcurrentStrategies
        ) {
            synchronized (lock) {
                if (activeStrategies.size() < maxConcurrentStrategies) {
                    MoverStrategy nextStrategy = waitingStrategies.values().iterator().next();

                    waitingStrategies.remove(nextStrategy.symbol);
                    activeStrategies.put(nextStrategy.symbol, nextStrategy);

                    Logger.info("Activated waiting strategy " + nextStrategy.symbol + " Strategies left: " + activeStrategies.size());

                    nextStrategy.initialize(activeStrategies.size());
                    checkStrategy(nextStrategy, instrumentManager.getInstrument(nextStrategy.symbol));
                }
            }
        }
    }

    public void removeStrategy(UUID strategyId) {
        activeStrategies.remove(strategyId);
        strategyRunner.removeMethodMap(strategyId);
        runningStrategies.remove(strategyId);
    }

    public BaseStrategy getStrategy(UUID strategyId) {
        for (BaseStrategy strategy : activeStrategies.values()) {
            if (strategy.id.equals(strategyId)) {
                return strategy;
            }
        }

        return null;
    }

    public BaseStrategy getStrategy(String symbol) {
        return activeStrategies.get(symbol);
    }
}
