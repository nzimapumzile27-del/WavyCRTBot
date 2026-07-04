package com.example.service

object Mql5CodeGenerator {

    fun generateEA(): String {
        return """//+------------------------------------------------------------------+
//|                                             CRT_TurtlesSoup_EA.mq5|
//|                                  Copyright 2026, Precise Trader  |
//|                             https://www.aistudio.crttrader.com   |
//+------------------------------------------------------------------+
#property copyright "Copyright 2026, Precise Trader"
#property link      "https://www.aistudio.crttrader.com"
#property version   "1.00"
#property description "CRT + FVG Multi-Timeframe Turtles Body Soup Strategy"
#property description "Synthesized with SAST Session sweeps & 4 Scale-in Entries"

//--- Inputs for Risk Management & Strategy Rules
input group "=== Institutional Range Settings ==="
input ENUM_TIMEFRAMES htf_timeframe = PERIOD_D1;      // Higher Timeframe (D1/H4)
input int            htf_candles_back = 30;         // Lookback for dominant candle

input group "=== Session Sweep Configuration (SAST) ==="
input int            sast_timezone_offset = 2;       // SAST offset from UTC (GMT+2)
input bool           trade_weekend_crypto = true;    // Allow trading Crypto on weekends
input int            asian_start_sast = 2;          // Asian Killzone Start SAST
input int            asian_end_sast = 9;            // Asian Killzone End SAST
input int            london_start_sast = 9;         // London Killzone Start SAST
input int            london_end_sast = 12;          // London Killzone End SAST
input int            ny_start_sast = 14;            // NY Killzone Start SAST
input int            ny_end_sast = 17;              // NY Killzone End SAST

input group "=== Structure & Confluences ==="
input int            rsi_period = 14;               // RSI period
input double         rsi_overbought = 70.0;         // Bearish threshold
input double         rsi_oversold = 30.0;           // Bullish threshold
input int            ema_fast_period = 9;           // Fast EMA (Structure Shift)
input int            ema_slow_period = 200;         // Slow EMA (Structure Filter)

input group "=== Position Sizing & 4-Entry Split ==="
input double         risk_percent_per_trade = 1.0;  // Risk % of Account Equity
input double         fixed_lot_fallback = 0.1;      // Lot fallback if Auto-size off
input bool           use_auto_position_sizing = true; // Use Account Balance Calculation
input int            split_entries_count = 4;       // Number of entry limits inside FVG

//--- System handles & state structures
int hEma9, hEma200, hRsi;
double ema9_val[], ema200_val[], rsi_val[];
MqlRates htf_rates[], ltf_rates[];

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
{
   // Initialize Indicators
   hEma9 = iMA(_Symbol, _Period, ema_fast_period, 0, MODE_EMA, PRICE_CLOSE);
   hEma200 = iMA(_Symbol, _Period, ema_slow_period, 0, MODE_EMA, PRICE_CLOSE);
   hRsi = iRSI(_Symbol, rsi_period, PRICE_CLOSE);
   
   if(hEma9 == INVALID_HANDLE || hEma200 == INVALID_HANDLE || hRsi == INVALID_HANDLE)
   {
      Print("Failed to initialize Technical Indicator Handles.");
      return(INIT_FAILED);
   }
   
   // Set buffer directions
   ArraySetAsSeries(ema9_val, true);
   ArraySetAsSeries(ema200_val, true);
   ArraySetAsSeries(rsi_val, true);
   ArraySetAsSeries(htf_rates, true);
   ArraySetAsSeries(ltf_rates, true);
   
   Print("CRT Turtle Soup Precise Trading Bot initialized successfully.");
   return(INIT_SUCCEEDED);
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
   IndicatorRelease(hEma9);
   IndicatorRelease(hEma200);
   IndicatorRelease(hRsi);
}

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
{
   // 1. Check Weekend Traded Assets
   if(IsWeekend() && !IsCryptoAsset()) return;
   
   // 2. Filter session on SAST (South African Standard Time)
   if(!IsInsideKillzone()) return;
   
   // 3. Update Indicator Buffers
   if(CopyBuffer(hEma9, 0, 0, 5, ema9_val) < 0) return;
   if(CopyBuffer(hEma200, 0, 0, 5, ema200_val) < 0) return;
   if(CopyBuffer(hRsi, 0, 0, 15, rsi_val) < 0) return;
   
   // Copy candles for current & HTF
   if(CopyRates(_Symbol, htf_timeframe, 0, htf_candles_back, htf_rates) < 0) return;
   if(CopyRates(_Symbol, _Period, 0, 10, ltf_rates) < 0) return;
   
   // 4. Find Higher Timeframe Institutional Range
   double htf_high = 0;
   double htf_low = 99999999;
   FindHTFRange(htf_high, htf_low);
   
   // Check if a trade is already active for this asset
   if(PositionsTotal() > 0 && CountAssetPositions(_Symbol) > 0) return;
   
   // 5. Look for Turtle Soup Sweeps (Liquidity sweep + rejection inside)
   double last_high = ltf_rates[1].high;
   double last_low = ltf_rates[1].low;
   double last_close = ltf_rates[1].close;
   
   bool is_bullish_sweep = (last_low < htf_low) && (last_close > htf_low);
   bool is_bearish_sweep = (last_high > htf_high) && (last_close < htf_high);
   
   if(!is_bullish_sweep && !is_bearish_sweep) return;
   
   // 6. Confluence Check: RSI Divergence
   bool rsi_div_confirmed = false;
   if(is_bullish_sweep)
   {
      double earlier_min_rsi = 100.0;
      for(int i = 2; i < 15; i++) {
         if(rsi_val[i] < earlier_min_rsi) earlier_min_rsi = rsi_val[i];
      }
      if(rsi_val[1] > earlier_min_rsi) rsi_div_confirmed = true; // Price made low, RSI did not
   }
   else if(is_bearish_sweep)
   {
      double earlier_max_rsi = 0.0;
      for(int i = 2; i < 15; i++) {
         if(rsi_val[i] > earlier_max_rsi) earlier_max_rsi = rsi_val[i];
      }
      if(rsi_val[1] < earlier_max_rsi) rsi_div_confirmed = true; // Price made high, RSI did not
   }
   
   if(!rsi_div_confirmed) return; // Strict confluence entry criteria
   
   // 7. Market Structure Shift (MSS) with EMAs
   double current_close = ltf_rates[0].close;
   bool mss_confirmed = false;
   if(is_bullish_sweep && current_close > ema9_val[0] && current_close > ema200_val[0]) mss_confirmed = true;
   if(is_bearish_sweep && current_close < ema9_val[0] && current_close < ema200_val[0]) mss_confirmed = true;
   
   if(!mss_confirmed) return;
   
   // 8. Fair Value Gap Midpoint & Order Parameters
   double fvg_midpoint = 0.0;
   bool fvg_found = false;
   for(int i = 0; i < 3; i++)
   {
      // Bullish FVG
      if(ltf_rates[i].low > ltf_rates[i+2].high && ltf_rates[i+1].close > ltf_rates[i+1].open)
      {
         fvg_midpoint = (ltf_rates[i].low + ltf_rates[i+2].high) / 2.0;
         fvg_found = true;
         break;
      }
      // Bearish FVG
      if(ltf_rates[i].high < ltf_rates[i+2].low && ltf_rates[i+1].close < ltf_rates[i+1].open)
      {
         fvg_midpoint = (ltf_rates[i].high + ltf_rates[i+2].low) / 2.0;
         fvg_found = true;
         break;
      }
   }
   
   if(!fvg_found)
   {
      fvg_midpoint = is_bullish_sweep ? 
         (current_close - (current_close - last_low)*0.5) : 
         (current_close + (last_high - current_close)*0.5);
   }
   
   // Set SL and Trailing targets
   double stop_loss = is_bullish_sweep ? last_low : last_high;
   double take_profit = is_bullish_sweep ? htf_high : htf_low;
   
   // 9. Automated Position Sizing & Spacing of 4 precise entry limit orders
   double account_equity = AccountInfoDouble(ACCOUNT_EQUITY);
   double risk_amount = account_equity * (risk_percent_per_trade / 100.0);
   double points_risk = MathAbs(fvg_midpoint - stop_loss);
   
   double total_lot_size = fixed_lot_fallback;
   if(use_auto_position_sizing && points_risk > 0)
   {
      double tick_value = SymbolInfoDouble(_Symbol, SYMBOL_TRADE_TICK_VALUE);
      double tick_size = SymbolInfoDouble(_Symbol, SYMBOL_TRADE_TICK_SIZE);
      if(tick_size > 0)
      {
         total_lot_size = risk_amount / ((points_risk / tick_size) * tick_value);
      }
   }
   
   // Split into 4 Precise Entries
   double split_lot = NormalizeDouble(total_lot_size / split_entries_count, 2);
   if(split_lot < SymbolInfoDouble(_Symbol, SYMBOL_VOLUME_MIN)) 
      split_lot = SymbolInfoDouble(_Symbol, SYMBOL_VOLUME_MIN);
      
   Execute4SplitEntries(is_bullish_sweep, fvg_midpoint, current_close, stop_loss, take_profit, split_lot);
}

//+------------------------------------------------------------------+
//| Execute 4 Split Limit and Market Orders for Precision Entry      |
//+------------------------------------------------------------------+
void Execute4SplitEntries(bool is_buy, double fvg_midpoint, double mss_price, double sl, double tp, double lot)
{
   // Compute the 4 precise entry prices
   double p1 = fvg_midpoint;
   double p2 = mss_price;
   double p3 = (fvg_midpoint + mss_price) / 2.0;
   double p4 = (fvg_midpoint + sl) / 2.0;
   
   double entries[4] = {p1, p2, p3, p4};
   
   Print("🚨 PLACING 4-SPLIT PRECISE SETUP LIMITS...");
   for(int i=0; i<4; i++)
   {
      MqlTradeRequest request={};
      MqlTradeResult  result={};
      
      request.action       = TRADE_ACTION_PENDING;
      request.symbol       = _Symbol;
      request.volume       = lot;
      request.sl           = sl;
      request.tp           = tp;
      request.price        = entries[i];
      request.type_filling = ORDER_FILLING_FOK;
      
      if(is_buy)
      {
         request.type = ORDER_TYPE_BUY_LIMIT;
      }
      else
      {
         request.type = ORDER_TYPE_SELL_LIMIT;
      }
      
      if(!OrderSend(request, result))
      {
         PrintFormat("Failed to place limit entry #%d at %.5f. Error: %d", i+1, entries[i], GetLastError());
      }
      else
      {
         PrintFormat("Limit entry #%d successfully placed at %.5f", i+1, entries[i]);
      }
   }
}

//+------------------------------------------------------------------+
//| Find the D1/H4 High and Low Candle Ranges                        |
//+------------------------------------------------------------------+
void FindHTFRange(double &htf_high, double &htf_low)
{
   double max_spread = 0;
   int best_idx = 0;
   
   for(int i=0; i<htf_candles_back; i++)
   {
      double spread = htf_rates[i].high - htf_rates[i].low;
      if(spread > max_spread)
      {
         max_spread = spread;
         best_idx = i;
      }
   }
   
   htf_high = htf_rates[best_idx].high;
   htf_low = htf_rates[best_idx].low;
}

//+------------------------------------------------------------------+
//| Helper Check if SAST timezone hour is in Killzones               |
//+------------------------------------------------------------------+
bool IsInsideKillzone()
{
   MqlDateTime dt;
   TimeToStruct(TimeCurrent(), dt);
   
   // Convert server time to SAST (GMT+2)
   int current_hour_sast = (dt.hour + sast_timezone_offset) % 24;
   
   if(current_hour_sast >= asian_start_sast && current_hour_sast < asian_end_sast) return true;
   if(current_hour_sast >= london_start_sast && current_hour_sast < london_end_sast) return true;
   if(current_hour_sast >= ny_start_sast && current_hour_sast < ny_end_sast) return true;
   
   return false;
}

//+------------------------------------------------------------------+
//| Check if today is weekend                                        |
//+------------------------------------------------------------------+
bool IsWeekend()
{
   MqlDateTime dt;
   TimeToStruct(TimeCurrent(), dt);
   return (dt.day_of_week == 0 || dt.day_of_week == 6);
}

//+------------------------------------------------------------------+
//| Check if Crypto asset for 24/7 weekend execution                 |
//+------------------------------------------------------------------+
bool IsCryptoAsset()
{
   string name = _Symbol;
   return (StringFind(name, "BTC") >= 0 || StringFind(name, "ETH") >= 0);
}

//+------------------------------------------------------------------+
//| Count active positions for a given symbol                        |
//+------------------------------------------------------------------+
int CountAssetPositions(string symbol)
{
   int count = 0;
   for(int i=0; i<PositionsTotal(); i++)
   {
      if(PositionGetSymbol(i) == symbol) count++;
   }
   return count;
}
"""
    }
}
