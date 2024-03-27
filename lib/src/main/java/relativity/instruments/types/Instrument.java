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

package relativity.instruments.types;

import java.util.Date;

public class Instrument {
    public String symbol;
    public String name;
    public String description;
    public String status;
    public String companyDescription;
    public Float marketCap;
    public String sector;
    public Float sharesOutstanding;
    public InstrumentStatistics statistics = new InstrumentStatistics();
    public InstrumentPricing pricing = new InstrumentPricing();
    public InstrumentQuoteStatistics quoteStatistics = new InstrumentQuoteStatistics();
    public Date createdAt = new Date();
    public long updatedAt;
}
