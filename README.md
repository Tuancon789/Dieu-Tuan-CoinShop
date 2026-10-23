# Coin Shop v0.3 — Android

Independent mobile coin request UI with light background, slate labels, pink highlights, bilingual Vietnamese / English support, editable recipient TikTok ID, and a gold music-note coin icon.

## Custom-quantity bottom sheet
Tapping **Tùy chỉnh / Custom** opens a bottom sheet with large read-only quantity display, coin icon, dynamic VND estimated price, dedicated 1–9 keypad, **000**, delete (long-press to clear), a PayPal method label, and a persistent **Continue** button. Values are validated from 1 to 2,500,000 coins. Continue selects the amount and opens the existing request confirmation flow.

## Payment and delivery limitations
**PayPal is NOT integrated, no money is debited, and TikTok coins are NOT delivered.** Orders are local requests marked awaiting confirmation; never treat this app as proof of payment or coin delivery. The ADMIN's displayed $10,000,000 is an illustrative sample, not real funds. Do not accept real customers until proper authorization, verified payment integration, and an authenticated backend are in place.

## Build APK
GitHub Actions → Build APK Coin Shop → latest successful workflow → Artifacts → CoinShop-v0.3-debug → app-debug.apk.
