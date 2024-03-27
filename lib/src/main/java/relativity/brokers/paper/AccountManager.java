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

import relativity.brokers.types.Account;
import relativity.brokers.types.Order;
import relativity.brokers.types.OrderIntentEnum;
import relativity.brokers.types.Position;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {
    public Account defaultAccount = new Account() {{
        id = "default";
        accountId = "default";
        balance = 40_000f;
        cashBalance = 40_000f;
        marginBalance = 80_000f;
        marginPercentage = 2f;
        outstandingMarginBalance = 0.0f;
        buyingPower = 120_000f;
    }};

    public ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Float> dailyProfits = new ConcurrentHashMap<>();
    public float biggestLoss = 0.0f;
    public float biggestGain = 0.0f;

    public OrderManager orderManager;
    public PositionManager positionManager;

    public AccountManager(OrderManager orderManager, PositionManager positionManager) {
        this.orderManager = orderManager;
        this.positionManager = positionManager;

        accounts.put(defaultAccount.accountId, defaultAccount);
        dailyProfits.put(defaultAccount.accountId, 0.0f);
    }

    public void addAccount(Account account) {
        accounts.put(account.id, account);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
    }

    public Account getAccount(@NotNull String accountId) {
        // TODO: returning a default account is a bug
        return accounts.containsKey(accountId) && accountId != null
            ? accounts.get(accountId)
            : defaultAccount;
    }

    public void updateDailyProfits(String accountId, Float profit) {
        Float currentProfit = dailyProfits.get(accountId);

        if (currentProfit == null) {
            currentProfit = 0.0f;
        }

        dailyProfits.put(accountId, currentProfit + profit);
    }

    public Float getAllBalance(String accountId) {
        Account account = getAccount(accountId);

        // TODO: should add cash balance to position values + enter position order values

        return account.balance + account.cashBalance;
    }

    public Float getTotalCash(String accountId) {
        Account account = getAccount(accountId);

        return account.cashBalance + getCashBalanceFromPositionsAndOrders(accountId);
    }

    public Float getTotalMargin(String accountId) {
        Account account = getAccount(accountId);

        return account.marginBalance + account.outstandingMarginBalance;
    }


    public Float getCashBalanceFromPositionsAndOrders(@NotNull String accountId) {
        float orderCash = orderManager.getCashBalanceFromOrders(accountId);
        float positionCash = positionManager.getPositionsCashBalance(accountId);

        return orderCash + positionCash;
    }

    public Float getStartingBalance(String accountId) {
        return this.getAllBalance(accountId) - this.getAllProfits(accountId);
    }

    public Float getAllProfits(String accountId) {
        return this.dailyProfits.get(accountId);
    }

    public Float getBalanceMinusMargin(String accountId) {
        Account account = getAccount(accountId);

        return getAllBalance(accountId) - account.outstandingMarginBalance;
    }

    public Float getBuyingPower(String accountId) {
        return calculateBuyingPower(accountId);
    }

    public Float calculateBuyingPower(String accountId) {
        Account account = getAccount(accountId);

        return account.marginBalance + account.cashBalance;
    }

    public Float calculateMarginBuyingPower(String accountId) {
        Account account = getAccount(accountId);

        account.marginBalance = account.cashBalance * account.marginPercentage;

        return account.marginBalance;
    }

    public void addMargin(String accountId, Float amount) {
        Account account = getAccount(accountId);

        account.marginBalance += amount;
    }

    public void addCash(String accountId, Float amount) {
        Account account = getAccount(accountId);

        account.cashBalance += amount;
    }

    public void updateCashAndMarginBalance(String accountId, Float cash, Float margin) {
        Account account = getAccount(accountId);

        if (margin > account.marginBalance) {
            throw new Error("Margin requested can not be greater than the account's margin balance");
        }

        synchronized (account.balancesLock) {
            account.cashBalance -= Math.abs(cash);
            account.marginBalance -= Math.abs(margin);
            account.outstandingMarginBalance += Math.abs(margin);
        }

        Float totalCash = account.cashBalance + getCashBalanceFromPositionsAndOrders(accountId);

        Logger.info("Total cash: " + totalCash + " cash removed " + cash + " margin removed " + margin);

        if (account.cashBalance < 0) {
//            throw new Error("Cash somehow went negative");
            Logger.warn("Cash somehow went negative");
        }

        // TODO: fire event here
    }

    public void updateBalancesFromOpenOrder(Order order) {
        if (order.intent == OrderIntentEnum.CLOSE) {
            return;
        }

        updateCashAndMarginBalance(
            order.accountId,
            order.cashBalance,
            order.marginBalance
        );
    }

    public void settleOrderFill(Order order) {
        // Re-think when an order fill affects the account balance
        // If the order is a close order, there is a position closed event
        // If the order is an enter order, the cash and margin balance are updated in the updateBalancesFromOpenOrder method
//        if (order.intent == OrderIntentEnum.CLOSE) {
//            Account account = getAccount(order.accountId);
//
//            synchronized (account.cashBalance) {
//                account.cashBalance += order.cashBalance;
//                account.marginBalance += order.marginBalance;
//                account.outstandingMarginBalance -= order.marginBalance;
//            }
//        }
    }

    public void settlePosition(Position position) {
        Account account = getAccount(position.accountId);

        synchronized (account.balancesLock) {
            account.cashBalance += Math.abs(position.cashBalance) + position.realizedProfit;
            account.marginBalance += position.marginBalance;
            account.outstandingMarginBalance -= position.marginBalance;
        }

        float dailyProfit = dailyProfits.get(position.accountId) + position.realizedProfit;
        dailyProfits.put(position.accountId, dailyProfit);

        if (position.realizedProfit > biggestGain) {
            biggestGain = position.realizedProfit;
        }

        if (position.realizedProfit < biggestLoss) {
            biggestLoss = position.realizedProfit;
        }

        Logger.info("Daily profit: $" + String.format("%.2f", dailyProfit) + " account cash balance: " + getTotalCash(account.id) + " margin balance: " + getTotalMargin(account.id));

        // TODO: find average profit
//        Float averageProfit =
    }

    public Float getPositionCash(String accountId) {
        Account account = getAccount(accountId);

        Float cash = 0.0f;

        for (Position position : account.positions) {
            cash += position.cashBalance;
        }

        return cash;
    }
}
