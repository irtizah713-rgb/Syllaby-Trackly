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
 * Permanently removes expired study-room messages.
 * Expiry values are bounded by Firestore rules to the seven-day retention window.
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
    let cursor = null;

    while (true) {
      let query = db
        .collectionGroup("messages")
        .where("expiresAtMillis", "<=", now)
        .orderBy("expiresAtMillis", "asc")
        .limit(BATCH_LIMIT);

      if (cursor) query = query.startAfter(cursor);

      const snapshot = await query.get();
      if (snapshot.empty) break;

      const batch = db.batch();
      let batchDeletes = 0;

      for (const doc of snapshot.docs) {
        const segments = doc.ref.path.split("/");
        // Avoid deleting documents from unrelated future subcollections named "messages".
        if (
          segments.length === 4 &&
          segments[0] === "studyRooms" &&
          segments[2] === "messages"
        ) {
          batch.delete(doc.ref);
          batchDeletes += 1;
        }
      }

      if (batchDeletes > 0) {
        await batch.commit();
        deleted += batchDeletes;
      }

      cursor = snapshot.docs[snapshot.docs.length - 1];
      if (snapshot.size < BATCH_LIMIT) break;
    }

    logger.info("Expired study-room messages deleted", { deleted });
  }
);
