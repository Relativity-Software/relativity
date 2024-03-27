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

import relativity.brokers.types.Order;
import relativity.brokers.types.OrderSideEnum;
import relativity.brokers.types.OrderTypeEnum;
import relativity.instruments.PriceMovement;
import relativity.instruments.types.Instrument;

public class TradeDecisionEngine {

    public boolean checkDefaultCriteria(Order order, Instrument instrument, PriceMovement priceMovement) {
        float high = instrument.pricing.priceStreams.high.getLast();
        float low = instrument.pricing.priceStreams.low.getLast();

        float previousHigh = instrument.pricing.priceStreams.high.get(instrument.pricing.priceStreams.high.size() - 2);
        float previousLow = instrument.pricing.priceStreams.low.get(instrument.pricing.priceStreams.low.size() - 2);
        float previousVolume = instrument.pricing.priceStreams.volume.get(instrument.pricing.priceStreams.volume.size() - 2);

        return (
            order.type == OrderTypeEnum.MARKET ||
            (
                (
                    order.type == OrderTypeEnum.LIMIT ||
                    order.limitPrice != null
                ) &&
                (
                    (
                        order.side == OrderSideEnum.SELL
                            ? order.limitPrice <= priceMovement.close
                            : order.limitPrice >= priceMovement.close
                    ) ||
                    (
                        order.side == OrderSideEnum.SELL
                            ? order.limitPrice <= high
                            : order.limitPrice >= low
                    ) ||
                    (
                        order.side == OrderSideEnum.SELL
                            ? order.limitPrice <= previousHigh
                            : order.limitPrice >= previousLow
                    )
                ) &&
                (
                    order.quantity <= priceMovement.volume * 3 ||
                    (
                        order.quantity <= previousVolume * 3 ||
                        order.quantity <= (previousVolume + priceMovement.volume) * 3 ||
                        order.quantity <= instrument.statistics.volume.average
                    )
                )
            )
        );
    }
}
