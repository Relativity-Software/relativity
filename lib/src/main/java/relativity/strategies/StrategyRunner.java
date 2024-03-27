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

import org.tinylog.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StrategyRunner {
    ConcurrentHashMap<String, Object> machines = new ConcurrentHashMap<>();
    ConcurrentHashMap<UUID, ConcurrentHashMap<StrategyHandlers, Runnable>> methodMaps = new ConcurrentHashMap<>();

    public void run(BaseStrategy strategy) {
        try {
            if (!methodMaps.containsKey(strategy.id)) {
                setMethodMap(strategy);
            }

            ConcurrentHashMap<StrategyHandlers, Runnable> methodMap = methodMaps.get(strategy.id);

            if ( methodMap == null ) {
                Logger.error("No method map for " + strategy.id + " " + strategy.name);

                return;
            }

            synchronized (strategy.state.runLock) {
                strategy.state.handler = StrategyHandlers.PRE_RUN;
                strategy.preRun();

                for (StrategyHandlers handler : strategy.config.handlers) {
                    if (strategy.state.shortCircuit) {
                        Logger.info(strategy.symbol + " Short circuiting strategy runner" + strategy.name + " reason: " + strategy.state.shortCircuitReason);

                        break;
                    }

                    if (strategy.state.ordersSubmitted) {
                        // TODO: Should we short circuit if orders still in play
                        Logger.info(strategy.symbol + " Short circuiting because orders submitted for " + strategy.name);

                        break;
                    }

                    try {
                        strategy.state.handler = handler;
                        Runnable runnable = methodMap.get(handler);

                        if (runnable != null) {
                            runnable.run();
                        }
                    } catch (Exception e) {
                        Logger.error("Error running handler " + handler + " for " + strategy.name + " " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Run these after the handlers
                strategy.postRun();
            }
        } catch (Exception e) {
            Logger.error("Error running strategy " + strategy.name + " " + e.getMessage());
        }
    }

    public void setMethodMap(BaseStrategy strategy) {
        ConcurrentHashMap<StrategyHandlers, Runnable> methodMap = new ConcurrentHashMap<>();

        methodMap.put(StrategyHandlers.PRE_RUN, strategy::preRun);
        methodMap.put(StrategyHandlers.CHECK_ITERATION, strategy::checkIteration);
        methodMap.put(StrategyHandlers.CHECK_EXITED, strategy::checkExited);
        methodMap.put(StrategyHandlers.CHECK_ORDER_LOCK, strategy::checkOrderLock);
        methodMap.put(StrategyHandlers.CHECK_PROFIT, strategy::checkProfit);
        methodMap.put(StrategyHandlers.REPLACE_STOP_LOSS_ORDER, strategy::replaceStopLossOrder);
        methodMap.put(StrategyHandlers.STOP_LOSS, strategy::stopLoss);
        methodMap.put(StrategyHandlers.REPLACE_TAKE_PROFIT_ORDER, strategy::replaceTakeProfitOrder);
        methodMap.put(StrategyHandlers.TAKE_PROFIT, strategy::takeProfit);
        methodMap.put(StrategyHandlers.REPLACE_ENTER_POSITION_ORDER, strategy::replaceEnterPositionOrder);
        methodMap.put(StrategyHandlers.CHECK_OLD_ORDERS, strategy::checkOldOrders);
        methodMap.put(StrategyHandlers.CHECK_TRADING_HOURS, strategy::checkTradingHours);
        methodMap.put(StrategyHandlers.CHECK_CIRCUIT_BREAKER, strategy::checkCircuitBreaker);
        methodMap.put(StrategyHandlers.ENTER_POSITION, strategy::enterPosition);
        methodMap.put(StrategyHandlers.POST_RUN, strategy::postRun);
        methodMap.put(StrategyHandlers.CHECK_RUN_QUEUE, strategy::checkRunQueue);

        methodMaps.put(strategy.id, methodMap);
    }

    public void removeMethodMap(BaseStrategy strategy) { methodMaps.remove(strategy.id); }
    public void removeMethodMap(UUID strategyId) { methodMaps.remove(strategyId); }

    /**************************************************************************
     * State Machine Methods
     *************************************************************************/
    public void createStateMachine(BaseStrategy strategy) {
        if (machines.containsKey(strategy)) {

        }
    }

    public void runWithStateMachine(BaseStrategy strategy) {

    }

    public void removeMachine(BaseStrategy strategy) {
        machines.remove(strategy.id);
    }
}
