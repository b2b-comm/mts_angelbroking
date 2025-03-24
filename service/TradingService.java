package com.tradingbot.service;

import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.SuperTrendIndicator;
import org.ta4j.core.indicators.ichimoku.*;
import org.ta4j.core.num.*;
import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.models.*;
import com.angelbroking.smartapi.utils.Constants;
import java.util.*;
import java.time.Duration;
import java.time.ZonedDateTime;

@Service
public class TradingService {

    private SmartConnect smartConnect;

    public TradingService() {
        this.smartConnect = new SmartConnect();
        this.smartConnect.setClientCode("YOUR_CLIENT_CODE");
        this.smartConnect.setApiKey("YOUR_API_KEY");
        this.smartConnect.generateSession("YOUR_PASSWORD", "YOUR_TOTP");
    }

    public void executeTrade(String symbol) {
        List<Double> prices = fetchHistoricalData(symbol);
        TimeSeries series = createTimeSeries(prices);

        boolean supertrendSignal = checkSupertrend(series);
        boolean kumoBreakoutSignal = checkKumoBreakout(series);

        if (supertrendSignal && kumoBreakoutSignal) {
            placeBuyOrder(symbol);
        } else if (checkSupertrendExit(series)) {
            placeSellOrder(symbol);
        }
    }

    private List<Double> fetchHistoricalData(String symbol) {
        List<Double> prices = new ArrayList<>();
        try {
            HistoricalDataParams params = new HistoricalDataParams();
            params.setExchange(Constants.EXCHANGE_NSE);
            params.setSymbol(symbol);
            params.setInterval("DAY");
            params.setFromDate("2024-01-01");
            params.setToDate("2025-01-01");
            
            List<HistoricalData> historicalData = smartConnect.getHistoricalData(params);
            for (HistoricalData data : historicalData) {
                prices.add(data.getClose());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prices;
    }

    private TimeSeries createTimeSeries(List<Double> prices) {
        TimeSeries series = new BaseTimeSeries("Stock Prices");
        for (int i = 0; i < prices.size(); i++) {
            series.addBar(new BaseBar(Duration.ofDays(1), ZonedDateTime.now().minusDays(i),
                    prices.get(i), prices.get(i), prices.get(i), prices.get(i), 1000));
        }
        return series;
    }

    private boolean checkSupertrend(TimeSeries series) {
        SuperTrendIndicator supertrend = new SuperTrendIndicator(series, 10, 3);
        return supertrend.getValue(series.getEndIndex()).isGreaterThan(series.getBar(series.getEndIndex()).getClosePrice());
    }

    private boolean checkSupertrendExit(TimeSeries series) {
        SuperTrendIndicator supertrend = new SuperTrendIndicator(series, 10, 3);
        return supertrend.getValue(series.getEndIndex()).isLessThan(series.getBar(series.getEndIndex()).getClosePrice());
    }

    private boolean checkKumoBreakout(TimeSeries series) {
        IchimokuKijunSenIndicator kijun = new IchimokuKijunSenIndicator(series);
        IchimokuSenkouSpanAIndicator spanA = new IchimokuSenkouSpanAIndicator(series);
        IchimokuSenkouSpanBIndicator spanB = new IchimokuSenkouSpanBIndicator(series);
        return series.getBar(series.getEndIndex()).getClosePrice().isGreaterThan(spanA.getValue(series.getEndIndex()))
                && series.getBar(series.getEndIndex()).getClosePrice().isGreaterThan(spanB.getValue(series.getEndIndex()));
    }

    private void placeBuyOrder(String symbol) {
        OrderParams order = new OrderParams();
        order.setVariety(Constants.VARIETY_REGULAR);
        order.setTradingSymbol(symbol);
        order.setTransactionType(Constants.TRANSACTION_TYPE_BUY);
        order.setQuantity(1);
        order.setOrderType(Constants.ORDER_TYPE_LIMIT);
        order.setExchange(Constants.EXCHANGE_NSE);
        order.setProductType(Constants.PRODUCT_INTRADAY);

        try {
            OrderResponse response = smartConnect.placeOrder(order);
            System.out.println("Buy Order Placed: " + response.getOrderId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void placeSellOrder(String symbol) {
        OrderParams order = new OrderParams();
        order.setVariety(Constants.VARIETY_REGULAR);
        order.setTradingSymbol(symbol);
        order.setTransactionType(Constants.TRANSACTION_TYPE_SELL);
        order.setQuantity(1);
        order.setOrderType(Constants.ORDER_TYPE_MARKET);
        order.setExchange(Constants.EXCHANGE_NSE);
        order.setProductType(Constants.PRODUCT_INTRADAY);

        try {
            OrderResponse response = smartConnect.placeOrder(order);
            System.out.println("Sell Order Placed: " + response.getOrderId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
