# Security deployment checklist

The Firestore rules test workflow is required to pass before this branch is considered ready.

This repository contains a scheduled Cloud Function that permanently deletes expired study-room messages.

## Required deployment steps

1. Confirm the Firebase project selected in the Firebase CLI is the project used by this Android app.
2. Confirm the named Firestore database ID in `firebase.json` is present in that Firebase project.
3. Use a Firebase project on the Blaze plan. Cloud Scheduler and scheduled Cloud Functions can incur charges.
4. Install the Functions dependencies from the repository root:

   ```sh
   cd functions
   npm install
   cd ..
   ```

5. Deploy the Firestore rules and scheduled cleanup function:

   ```sh
   firebase deploy --only firestore:rules,functions:deleteExpiredRoomMessages
   ```

6. In the Firebase Console, verify the scheduled job exists and inspect Cloud Function logs after its first run.

## Retention behavior

- New room messages carry an `expiresAtMillis` value approximately seven days after creation.
- Firestore rules reject expiry values outside the allowed seven-day window (with a one-minute clock-skew allowance).
- The scheduled function runs hourly and deletes expired message documents in batches.
- A message may remain stored for up to roughly one additional hour after its expiry time.
- The Android app also filters expired messages locally for immediate UI behavior.

## Before production

- From the repository root, run `npm install` followed by `npm test`. The root test script starts the Firestore emulator on the configured port and runs `firestore.test.js`.
- Run the Android debug build and test the public/private room flows.
- Verify Cloud Scheduler and Functions logs after deployment.
- Never commit service-account JSON files, signing keys, OAuth secrets, or local `.env` files.
- Do not merge the security pull request until CI and emulator checks pass.

## Important remaining security migration

XP, Theme Points, achievement rewards, streaks, and shop purchases still have legacy client-side write paths. They must be migrated together to authenticated, rate-limited server operations before those fields can safely be locked in Firestore rules. Removing the client write permissions before that migration would break existing app features.
