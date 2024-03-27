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

package relativity.instruments;

public enum InstrumentTradeConditions {
    REGULAR_SALE(0),
    ACQUISITION(1),
    AVERAGE_PRICE_TRADE(2),
    AUTOMATIC_EXECUTION(3),
    BUNCHED_TRADE(4),
    BUNCHED_SOLD_TRADE(5),
    CAP_ELECTION(6),
    CASH_SALE(7),
    CLOSING_PRINTS(8),
    CROSS_TRADE(9),
    DERIVATIVELY_PRICED(10),
    DISTRIBUTION(11),
    FORM_T(12),
    EXTENDED_HOURS_TRADE_SOLD_OUT_OF_SEQUENCE(13),
    INTERMARKET_SWEEP(14),
    MARKET_CENTER_OFFICIAL_CLOSE(15),
    MARKET_CENTER_OFFICIAL_OPEN(16),
    MARKET_CENTER_OPENING(17),
    MARKET_CENTER_REOPENING(18),
    MARKET_CENTER_CLOSING(19),
    NEXT_DAY(20),
    PRICE_VARIATION_TRADE(21),
    PRIOR_REFERENCE_PRICE(22),
    RULE_155_TRADE(23),
    RULE_127_TRADE(24),
    OPENING_PRINTS(25),
    OPENED(26),
    STOPPED_STOCK_REGULAR_TRADE(27),
    REOPENING_PRINTS(28),
    SELLER(29),
    SOLD_LAST(30),
    SOLD_OUT(32),
    SOLD_OUT_OF_SEQUENCE(33),
    SPLIT_TRADE(34),
    STOCK_OPTION_TRADE(35),
    YELLOW_FLAG_REGULAR_TRADE(36),
    ODD_LOT_TRADE(37),
    CORRECTED_CONSOLIDATED_CLOSE_PRICE_AS_PER_LISTING_MARKET(38),
    UNKNOWN(39),
    HELD(40),
    TRADE_THROUGH_EXEMPT(41), // Could be what is causing stray pricing values
    NON_ELIGIBLE_TRADE(42),
    NON_ELIGIBLE_EXTENDED_HOURS_TRADE(43),
    CANCELLED(44),
    RECOVERY(45),
    CORRECTION(46),
    AS_OF(47),
    AS_OF_CORRECTION(48),
    AS_OF_CANCEL(49),
    OOB(50),
    SUMMARY(51),
    CONTINGENT(52),
    QUALIFIED_CONTINGENT(53),
    ERRORED(54),
    OPENING_REOPENING_TRADE_DETAIL(55),
    PLACEHOLDER(56),
    PLACEHOLDER_FOR_611_EXEMPT(59),
    SSR(60),
    DEFICIENT(63),
    DELINQUENT(64),
    BANKRUPT_DEFICIENT(65),
    BANKRUPT_DELINQUENT(66),
    DEFICIENT_AND_DELINQUENT(67),
    DELINQUENT_BANKRUPT(68),
    LIQUIDATION(69),
    CREATIONS_SUSPENDED(70),
    REDEMPTIONS_SUSPENDED(71),
    SIP_GENERATED(82),
    CROSSED_MARKET(84),
    LOCKED_MARKET(85),
    CQS_GENERATED(94);


    public final int value;

    InstrumentTradeConditions(int value) {
        this.value = value;
    }
}
