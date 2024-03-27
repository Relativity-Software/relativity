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

import relativity.instruments.types.Instrument;

import java.util.List;

public class RSIUp extends AbstractRSI {
    public static boolean run(Instrument instrument, int startingPreviousPeriod, int period) {
        // TODO: I'm not sure this part is necessary
//        if (security.statistics.indicators.rsiValues.size() == 0) {
//            List<Float> trailingPrices = security.pricing.trailingPrices.subList(
//                startingPreviousPeriod * period,
//                security.pricing.trailingPrices.size() - 1
//            );
//
//
//        }

        List<Float> rsiValues = getRSIValues(instrument, startingPreviousPeriod);

        if (rsiValues.size() < startingPreviousPeriod) {
            return false;
        }

        boolean result = true;
        Float previous = null;


        for (Float value : rsiValues) {
            if (previous != null) {
                result = result && previous < value;
            }

            previous = value;
        }

        return result;
    }
}
