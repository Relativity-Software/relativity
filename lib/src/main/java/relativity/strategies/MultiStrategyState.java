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

import java.time.LocalDateTime;

public class MultiStrategyState {
    public StrategyStatusEnum status;

    public boolean shortCircuit = false;
    public String shortCircuitReason;
    public StrategyHandlers handler;
    public StrategyHandlers nextHandler;

    public Boolean orderlock = false;

    public LocalDateTime startedAt = LocalDateTime.now();
    public LocalDateTime endedAt;
    public String exitingReason;

    final Object runLock = new Object();

    public void resetStrategyRun() {
        shortCircuit = false;
        shortCircuitReason = null;
        handler = null;
        nextHandler = null;
    }
}
