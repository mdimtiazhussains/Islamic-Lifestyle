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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

// 1. Unauthenticated users cannot read or write anything
test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("Unauthenticated user: cannot write tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      id: "t1",
      userId: ALICE_UID,
      date: "2026-10-08",
      title: "Task 1",
      status: "pending"
    })
  );
});

// 2. Cross-user isolation: Alice cannot read or write Bob's data
test("Cross-user isolation: Alice cannot read Bob's tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").get());
});

test("Cross-user isolation: Alice cannot write to Bob's tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("t1").set({
      id: "t1",
      userId: BOB_UID,
      date: "2026-10-08",
      title: "Bob Task",
      status: "pending"
    })
  );
});

// 3. Authenticated owner: can read and write their own data
test("Owner: Alice can create and read her own tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      id: "t1",
      userId: ALICE_UID,
      date: "2026-10-08",
      title: "Alice Focus Task",
      status: "pending"
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").get()
  );
});

test("Owner: Alice can create and read her diary entries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("diary_entries").doc("d1").set({
      id: "d1",
      userId: ALICE_UID,
      date: "2026-10-08",
      title: "Reflection",
      content: "Mindset reflection today"
    })
  );
});

test("Owner: Alice can create and read her morning plans", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("morning_plans").doc("m1").set({
      id: "m1",
      userId: ALICE_UID,
      date: "2026-10-08",
      completed: true
    })
  );
});

test("Owner: Alice can create and read her daily progress records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("p1").set({
      id: "p1",
      userId: ALICE_UID,
      date: "2026-10-08",
      completedTasks: 5,
      totalTasks: 8,
      completionRate: 0.625
    })
  );
});
