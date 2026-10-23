# Coin Shop v0.5

Language and currency display are linked:
- Vietnamese (VI): show prices and wallet balance in **VND**.
- English (EN): show the same amounts as **approximate USD**, formatted `≈$0.00`.
- Indicative fixed reference: **1 USD = 25,850 VND (October 23, 2026)**. This is not a live exchange rate and is **not** an USD account.
- Purchases and wallet balances always remain denominated **VND in local device storage**. Switching languages does not exchange, credit, or debit funds.
- ADMIN credit amount input follows the selected language: VND integer in Vietnamese, approximate USD decimal in English (converted to VND for wallet credit).
- Customer prices, custom-quantity sheet, wallet, checkout confirmation, success screen, orders and ADMIN balances all share one currency presentation formatter.

## Limitations
The app currently has an **on-device offline wallet**, not a shared server wallet or a licensed payment provider. ADMIN can only manage accounts stored on the same phone. The illustrative $10 million ADMIN amount is display-only, not spendable money. Wallet payment success is only a local-wallet transaction; it does not establish TikTok coin fulfillment. Do not use this version for real customer funds.

## Android build
GitHub Actions → Build APK Coin Shop → latest successful run → artifact `CoinShop-v0.5-debug` → extract `app-debug.apk`.
