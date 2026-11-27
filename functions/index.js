const { initializeApp, getApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");

initializeApp();

const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-syllabyt-cce7c989-174e-4113-9c26-d0477de1bb99";
const db = getFirestore(getApp(), DATABASE_ID);
const BATCH_LIMIT = 400;

/**
 * Permanently removes chat messages after their seven-day retention window.
 * Security rules constrain client-created expiry values to the next seven days.
 */
exports.deleteExpiredRoomMessages = onSchedule(
  {
    schedule: "every 60 minutes",
    region: "us-central1",
    timeZone: "Etc/UTC",
    retryCount: 3,
    maxInstances: 1,
  },
  async () => {
    const now = Date.now();
    let deleted = 0;

    while (true) {
      const snapshot = await db
        .collectionGroup("messages")
        .where("expiresAtMillis", "<=", now)
        .orderBy("expiresAtMillis", "asc")
        .limit(BATCH_LIMIT)
        .get();

      if (snapshot.empty) break;

      const batch = db.batch();
      snapshot.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
      deleted += snapshot.size;

      if (snapshot.size < BATCH_LIMIT) break;
    }

    logger.info("Expired study-room messages deleted", { deleted });
  }
);
