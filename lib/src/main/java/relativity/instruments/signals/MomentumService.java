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

package relativity.instruments.signals;

import relativity.instruments.PriceMovement;
import relativity.instruments.types.MovementEnum;
import relativity.instruments.types.Instrument;
import org.tinylog.Logger;

import java.util.ArrayList;

public class MomentumService {
    public static PriceMovement getMovement(Instrument instrument, int numberOfPrices) {
        return getMovement(instrument.pricing.trailingPrices, numberOfPrices);
    }

    public static PriceMovement getMovement(ArrayList<Float> prices, int numberOfPrices) {
        int size = prices.size();

        if (size == 0) {
            Logger.error("No prices to calculate movement");
            return null;
        }

        int openIndex = numberOfPrices < size
            ? size - (numberOfPrices + 1)
            : 0;

        Float open = prices.get(openIndex);
        Float close = prices.getLast();
        Float change = close - open;
        Float percentChange = change / open;

        MovementEnum movement = MovementEnum.NONE;

        if (open < close) {
            movement = MovementEnum.UP;
        }

        if (close < open) {
            movement = MovementEnum.DOWN;
        }

        PriceMovement priceMovement = new PriceMovement();

        priceMovement.open = open;
        priceMovement.close = close;
        priceMovement.change = change;
        priceMovement.percentChange = percentChange;
        priceMovement.movement = movement;

        return priceMovement;
    }
}
