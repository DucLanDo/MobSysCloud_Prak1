/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */


// Start writing functions
// https://firebase.google.com/docs/functions/typescript

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.

// export const helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

// Serverstatus
export const getServerStatus = functions.https.onRequest((req, res) => {
  const now = new Date().toISOString();
  res.json({
    status: "ok",
    serverTime: now,
    message: "MobSysCloud_Prak1 Server läuft 🚀",
  });
});
export const getUsers = functions.https.onRequest(async (req, res) => {
  try {
    const db = admin.firestore();
    const snapshot = await db.collection("users").limit(50).get(); 

    const users = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    res.json({
      count: users.length,
      users,
    });
  } catch (err) {
    console.error("getUsers error", err);
    res.status(500).json({ error: "Internal server error" });
  }
});
export const getLocations = functions.https.onRequest(async (req, res) => {
  try {
    const db = admin.firestore();
    const userId = req.query.userId as string | undefined;

    let query = db.collection("locations").orderBy("timestamp", "desc").limit(100);

    if (userId) {
      query = db
        .collection("locations")
        .where("userId", "==", userId)
        .orderBy("timestamp", "desc")
        .limit(100);
    }

    const snapshot = await query.get();

    const locations = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    res.json({
      count: locations.length,
      filteredByUserId: userId ?? null,
      locations,
    });
  } catch (err) {
    console.error("getLocations error", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

export const getCounts = functions.https.onRequest(async (req, res) => {
  try {
    const db = admin.firestore();

    const [locSnap, tempSnap, accelSnap] = await Promise.all([
      db.collection("locations").get(),
      db.collection("temperatures").get(),
      db.collection("accelerations").get(),
    ]);

    res.json({
      locations: locSnap.size,
      temperatures: tempSnap.size,
      accelerations: accelSnap.size,
    });
  } catch (err) {
    console.error("getCounts error", err);
    res.status(500).json({ error: "Internal server error" });
  }
});

export const getTemperatures = functions.https.onRequest(async (req, res) => {
  try {
    const db = admin.firestore();
    const userId = req.query.userId as string | undefined;

    let query = db
      .collection("temperatures")
      .orderBy("timestamp", "desc")
      .limit(100);

    // Optional filtern nach userId
    if (userId) {
      query = db
        .collection("temperatures")
        .where("userId", "==", userId)
        .orderBy("timestamp", "desc")
        .limit(100);
    }

    const snapshot = await query.get();

    const temps = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    res.json({
      count: temps.length,
      filteredByUserId: userId ?? null,
      temperatures: temps,
    });
  } catch (err) {
    console.error("getTemperatures error", err);
    res.status(500).json({ error: "Internal server error" });
  }
});
