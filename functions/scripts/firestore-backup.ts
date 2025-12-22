import admin from "firebase-admin";
import fs from "fs";

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
});

const db = admin.firestore();

async function backupCollection(name: string) {
  const snapshot = await db.collection(name).get();
  const data = snapshot.docs.map(doc => ({
    id: doc.id,
    ...doc.data(),
  }));

  fs.writeFileSync(
    `backup-${name}.json`,
    JSON.stringify(data, null, 2)
  );

  console.log(`Backup for ${name} created`);
}

async function runBackup() {
  await backupCollection("users");
  await backupCollection("locations");
  await backupCollection("temperatures");
  await backupCollection("accelerations");
}

runBackup().catch(console.error);
