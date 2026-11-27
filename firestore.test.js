const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauth.collection("publicProfiles").where("discoverable", "==", true).get()
  );
  await assertFails(
    unauth.collection("studyRooms").where("privacy", "==", "PUBLIC").get()
  );
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Hack",
      priority: "HIGH",
      category: "Study",
      tags: [],
      reminderEnabled: false,
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("User account: owner create, partial update, cross-user denial, and PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      xp: 0,
      level: 1,
      themePoints: 50,
      currentStreak: 0,
      longestStreak: 0,
      totalFocusMinutes: 0,
      dailyGoalMinutes: 60,
      selectedFrameId: "frame_default",
      unlockedShopIds: ["frame_default"],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob cannot read Alice's private user document containing email
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Alice can update XP & bio while preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 120,
      level: 2,
      bio: "Focused on Physics & Calculus",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow field injection must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 999,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Public profiles: discoverable query alignment and no cross-user writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      friendCode: "ST-ALICE1",
      level: 1,
      xp: 0,
      rankTitle: "Initiate",
      currentStreak: 0,
      totalFocusMinutes: 0,
      selectedFrameId: "frame_default",
      discoverable: true,
      showStats: true,
      showAchievements: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Discoverable query succeeds for Bob
  await assertSucceeds(
    bobDb
      .collection("publicProfiles")
      .where("discoverable", "==", true)
      .where("friendCode", "==", "ST-ALICE1")
      .get()
  );

  // Unfiltered list on publicProfiles fails
  await assertFails(bobDb.collection("publicProfiles").get());

  // A direct document read must also respect discoverability.
  await aliceDb.collection("publicProfiles").doc(ALICE_UID).update({
    discoverable: false,
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertFails(bobDb.collection("publicProfiles").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("publicProfiles").doc(ALICE_UID).get());

  // Bob cannot overwrite Alice's public profile
  await assertFails(
    bobDb.collection("publicProfiles").doc(ALICE_UID).update({
      username: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Academic subcollections: subjects, chapters, tasks, notes, focusSessions, attendance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_1").set({
      userId: ALICE_UID,
      name: "Physics",
      colorHex: "#38BDF8",
      iconKey: "science",
      sortOrder: 0,
      targetChapters: 12,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chapters").doc("ch_1").set({
      userId: ALICE_UID,
      subjectId: "sub_1",
      title: "Electrostatics",
      status: "COMPLETED",
      sortOrder: 0,
      completedAtMillis: Date.now() - 1000,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Solve 20 numericals",
      priority: "HIGH",
      category: "Practice",
      tags: ["physics", "exam"],
      subjectId: "sub_1",
      reminderEnabled: true,
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject task create containing @DocumentId 'id' key
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad").set({
      id: "task_bad",
      userId: ALICE_UID,
      title: "Bad task",
      priority: "LOW",
      category: "General",
      tags: [],
      reminderEnabled: false,
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Gauss Law Summary",
      content: "Flux equals enclosed charge over epsilon zero.",
      subjectId: "sub_1",
      pinned: true,
      favorite: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("focusSessions").doc("fs_1").set({
      userId: ALICE_UID,
      subjectId: "sub_1",
      subjectName: "Physics",
      durationMinutes: 25,
      breakMinutes: 5,
      modeLabel: "25/5 Classic",
      ambientSound: "Rain",
      completed: true,
      startedAtMillis: Date.now() - 1500000,
      endedAtMillis: Date.now() - 1000,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_1").set({
      userId: ALICE_UID,
      dateKey: "2026-11-27",
      subjectId: "sub_1",
      status: "PRESENT",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob cannot list Alice's chapters or tasks
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("chapters").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
});

test("Friend requests: creation, acceptance, and unauthorized tampering blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("friendRequests").doc("req_1").set({
      fromUid: ALICE_UID,
      fromUsername: "Alice",
      fromFriendCode: "ST-ALICE1",
      toUid: BOB_UID,
      toUsername: "Bob",
      status: "PENDING",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Charlie cannot read or update Alice->Bob request
  await assertFails(charlieDb.collection("friendRequests").doc("req_1").get());
  await assertFails(
    charlieDb.collection("friendRequests").doc("req_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can accept
  await assertSucceeds(
    bobDb.collection("friendRequests").doc("req_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Scoped queries succeed
  await assertSucceeds(
    bobDb.collection("friendRequests").where("toUid", "==", BOB_UID).get()
  );
  await assertFails(bobDb.collection("friendRequests").get());
});

test("Friend links require a matching accepted friend request", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const friendLink = {
    userId: BOB_UID,
    friendUid: ALICE_UID,
    friendUsername: "Alice",
    friendCode: "ST-ALICE1",
    relationStatus: "FRIEND",
    createdAt: past,
    updatedAt: past,
  };

  // A user cannot unilaterally add another user as a friend.
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).collection("friends").doc(ALICE_UID).set(friendLink)
  );

  // The target accepts the deterministic friend request.
  await assertSucceeds(
    aliceDb.collection("friendRequests").doc(`freq_${BOB_UID}_${ALICE_UID}`).set({
      fromUid: BOB_UID,
      fromUsername: "Bob",
      fromFriendCode: "ST-BOB456",
      toUid: ALICE_UID,
      toUsername: "Alice",
      status: "PENDING",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("friendRequests").doc(`freq_${BOB_UID}_${ALICE_UID}`).update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // After acceptance, the sender can create their reciprocal friend link.
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("friends").doc(ALICE_UID).set(friendLink)
  );
});

test("Study rooms & chat messages: public vs private isolation and text-only enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Public room
  await assertSucceeds(
    aliceDb.collection("studyRooms").doc("room_pub").set({
      hostUid: ALICE_UID,
      hostName: "Alice",
      title: "Midnight Physics Sprint",
      description: "Quiet Pomodoro study",
      environment: "LIBRARY",
      privacy: "PUBLIC",
      inviteCode: "LIB-9988",
      invitedUids: [],
      memberUids: [ALICE_UID],
      timerState: "IDLE",
      timerDurationMinutes: 25,
      timerRemainingSeconds: 1500,
      timerUpdatedAtMillis: Date.now() - 1000,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Private room inviting Bob only
  await assertSucceeds(
    aliceDb.collection("studyRooms").doc("room_priv").set({
      hostUid: ALICE_UID,
      hostName: "Alice",
      title: "Private Exam Prep",
      description: "Invited friends only",
      environment: "FOREST",
      privacy: "PRIVATE",
      inviteCode: "FOR-1122",
      invitedUids: [BOB_UID],
      memberUids: [ALICE_UID],
      timerState: "RUNNING",
      timerDurationMinutes: 50,
      timerRemainingSeconds: 3000,
      timerUpdatedAtMillis: Date.now() - 1000,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Public room query succeeds for Bob
  await assertSucceeds(
    bobDb.collection("studyRooms").where("privacy", "==", "PUBLIC").get()
  );

  // Private room denied to Charlie, allowed to invited Bob
  await assertFails(charlieDb.collection("studyRooms").doc("room_priv").get());
  await assertSucceeds(bobDb.collection("studyRooms").doc("room_priv").get());

  // Bob joins private room because he was invited.
  await assertSucceeds(
    bobDb.collection("studyRooms").doc("room_priv").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // A non-host cannot change the room timer or add another user.
  await assertFails(
    bobDb.collection("studyRooms").doc("room_priv").update({
      timerState: "PAUSED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("studyRooms").doc("room_priv").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // An unrelated user cannot join a private room.
  await assertFails(
    charlieDb.collection("studyRooms").doc("room_priv").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // An existing member can leave a private room even after invite metadata changes.
  await aliceDb.collection("studyRooms").doc("room_priv").update({
    invitedUids: [],
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(
    bobDb.collection("studyRooms").doc("room_priv").update({
      memberUids: [ALICE_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob sends text message in public room
  await assertSucceeds(
    bobDb
      .collection("studyRooms")
      .doc("room_pub")
      .collection("messages")
      .doc("msg_1")
      .set({
        roomId: "room_pub",
        senderUid: BOB_UID,
        senderName: "Bob",
        text: "Starting 25m focus now!",
        expiresAtMillis: Date.now() + 7 * 24 * 3600 * 1000,
        createdAt: past,
      })
  );

  // Querying public room messages succeeds
  await assertSucceeds(
    aliceDb
      .collection("studyRooms")
      .doc("room_pub")
      .collection("messages")
      .get()
  );

  // Message documents cannot smuggle client-controlled room access metadata.
  await assertFails(
    bobDb.collection("studyRooms").doc("room_pub").collection("messages").doc("msg_spoof").set({
      roomId: "room_pub",
      roomHostUid: BOB_UID,
      roomPrivacy: "PRIVATE",
      roomAllowedUids: [BOB_UID],
      senderUid: BOB_UID,
      senderName: "Bob",
      text: "Spoofed metadata",
      expiresAtMillis: Date.now() + 7 * 24 * 3600 * 1000,
      createdAt: past,
    })
  );

  // Clients cannot keep messages alive by choosing an arbitrarily distant expiry.
  await assertFails(
    bobDb.collection("studyRooms").doc("room_pub").collection("messages").doc("msg_forever").set({
      roomId: "room_pub",
      senderUid: BOB_UID,
      senderName: "Bob",
      text: "This should expire within the retention window",
      expiresAtMillis: Date.now() + 30 * 24 * 3600 * 1000,
      createdAt: past,
    })
  );

  // Charlie cannot send message to private room
  await assertFails(
    charlieDb
      .collection("studyRooms")
      .doc("room_priv")
      .collection("messages")
      .doc("msg_hack")
      .set({
        roomId: "room_priv",
        senderUid: CHARLIE_UID,
        senderName: "Charlie",
        text: "Intruder!",
        expiresAtMillis: Date.now() + 7 * 24 * 3600 * 1000,
        createdAt: past,
      })
  );
});
