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

package relativity.events.types;

import relativity.brokers.types.PositionTypeEnum;
import relativity.events.EventTypesEnum;
import relativity.instruments.types.Instrument;

import java.util.UUID;

public class ActivateStrategyEvent extends BaseEvent {
    public String strategyName;
    public Instrument instrument;
    public PositionTypeEnum positionType;

    public ActivateStrategyEvent(String strategyName, Instrument instrument, PositionTypeEnum positionType) {
        name = EventTypesEnum.ACTIVATE_STRATEGY.name();
        this.strategyName = strategyName;
        this.instrument = instrument;
        this.id = UUID.randomUUID();
        this.positionType = positionType;
    }
}
