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

package relativity.brokers;

import relativity.instruments.types.MarketHoursEnum;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class MarketHoursService {
    public static boolean isPreMarket() {
        LocalDateTime now = LocalDateTime.now();

        return now.toLocalTime().isBefore(LocalTime.of(8, 30));
    }

    public static boolean isMarketHours() {
        LocalDateTime now = LocalDateTime.now();

        return (
            now.toLocalTime().isAfter(LocalTime.of(8, 30)) ||
            now.toLocalTime().equals(LocalTime.of(8, 30))
        ) && now.getHour() < 15;
    }

    public static boolean isPostMarket() {
        LocalDateTime now = LocalDateTime.now();

        return now.toLocalTime().isAfter(LocalTime.of(15, 0));
    }

    public static MarketHoursEnum getMarketHours() {
        if (isPreMarket()) {
            return MarketHoursEnum.PRE;
        }

        if (isMarketHours()) {
            return MarketHoursEnum.MARKET;
        }

        if (isPostMarket()) {
            return MarketHoursEnum.POST;
        }

        return MarketHoursEnum.NONE;
    }

    public static boolean isInExtendedHours() {
        return isPreMarket() || isPostMarket();
    }
}
