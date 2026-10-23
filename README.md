# Coin Shop v0.4 — Android local wallet

## Changes
- Payment method is now **Coin Shop wallet only**. No PayPal UI and no "Create order" button.
- Customers choose a coin bundle or use a fixed **custom numeric keypad** to select 1–2,500,000 coins. Displayed price refreshes as they type.
- On wallet payment confirmation, app checks the user's VND wallet balance, deducts the displayed amount, records a local payment, and shows a green tick **Wallet payment successful**, including coin quantity and pending manual fulfillment.
- ADM local administrator can credit the VND balance of accounts registered **on the same phone**. Balances and basic ledger are saved in app private storage.
- On a fresh install, the first account must be named **ADM**. The administrator chooses their own password inside the app. Existing local administrators can add or reset ADM credentials through the ADMIN screen.
- ADMIN continues to display $10 million as an **illustrative** amount, not money that can be spent.

## Important limitations
This is a **local proof of concept**, not a real shared-wallet financial system. Accounts are NOT synchronized between phones. Local app storage can be reset, modified, or lost; do not store or sell real-money balances in this version. Only an authenticated server with ledger transactions and administrator access controls can support real customers. The app does **not** transfer or automatically provision TikTok coins; wallet payment is separate from fulfillment. The application must not be used to claim coin delivery without fulfillment.

## Build
Open the GitHub Actions run **Build APK Coin Shop**, download the `CoinShop-v0.4-debug` artifact and install `app-debug.apk`.
