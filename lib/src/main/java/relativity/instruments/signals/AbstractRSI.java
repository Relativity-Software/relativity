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

public class AbstractRSI {
    public static List<Float> getRSIValues(Instrument instrument, int startingPreviousPeriod) {
        return instrument.statistics.indicators.rsiValues.subList(
            Math.max(instrument.statistics.indicators.rsiValues.size() - 1 - startingPreviousPeriod, 0),
            instrument.statistics.indicators.rsiValues.size() - 1
        );
    }
}
