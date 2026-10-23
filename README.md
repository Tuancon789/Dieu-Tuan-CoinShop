# Coin Shop v0.2 — Android

A two-language independent coin-order interface with a light background, slate typography, pink controls and a gold music-note coin icon.

## Features
- Vietnamese / English selector
- Local registration and sign-in; first local user has sample ADMIN access
- Change TikTok recipient ID, saved per local username
- Choose 30 / 100 / 200 / 500 / 1,000 coins or custom 1–2,500,000
- Browse local request history and sample admin dashboard
- PayPal is shown as the requested single payment method, **but is not integrated**

## Important
This build saves order **requests locally only**, with status 'Awaiting confirmation'. It does not process PayPal payments, deliver TikTok coins, contact a fulfillment server, or verify receipts. The ADMIN's $10m balance is labeled as illustrative, not actual money. This prototype is **not suitable for real customer transactions** until authorization, real payment verification, a secure backend, and manual fulfillment workflow have been implemented.

## Build APK
Open Actions -> Build APK Coin Shop -> latest green run -> Artifacts -> CoinShop-v0.2-debug ZIP -> app-debug.apk.
Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, SDK 35.
