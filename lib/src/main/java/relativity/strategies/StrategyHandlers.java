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

public enum StrategyHandlers {
    PRE_RUN,
    POST_RUN,
    CHECK_ITERATION,
    CHECK_ORDER_LOCK,
    CHECK_PROFIT,
    REPLACE_STOP_LOSS_ORDER,
    STOP_LOSS,
    REPLACE_TAKE_PROFIT_ORDER,
    TAKE_PROFIT,
    REPLACE_ENTER_POSITION_ORDER,
    CHECK_OPEN_ORDERS,
    CHECK_CLOSED_ORDERS,
    CHECK_OLD_ORDERS,
    CHECK_TRADING_HOURS,
    CHECK_CIRCUIT_BREAKER,
    ENTER_POSITION,
    MOMENTUM_SHIFT,
    EXIT_POSITIONS,
    CHECK_POSITIONS,
    CHECK_EXITED,
    CHECK_RUN_QUEUE,
    ERROR;
}
