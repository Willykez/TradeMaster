import { initializeApp } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

initializeApp();

// A client app can never be the thing that fans a message out to every
// other install -- that has to happen server-side, where the credential to
// call FCM actually lives. This is the missing half of "admin publishes,
// everyone gets a push": the Android app writes a signal to Firestore
// (only if the writer is an allowlisted admin -- see /firestore.rules);
// this function reacts to that write and broadcasts to the shared
// "new_signals" topic every install subscribes to on first launch.
export const notifyOnNewSignal = onDocumentCreated("signals/{signalId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  await getMessaging().send({
    topic: "new_signals",
    notification: {
      title: `New ${String(data.type).toUpperCase()} signal: ${data.pair}`,
      body: `Entry ${data.entry} · TP ${data.tp} · SL ${data.sl}`,
    },
    android: { priority: "high" },
  });
});

export const notifyOnNewPost = onDocumentCreated("posts/{postId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  await getMessaging().send({
    topic: "new_signals",
    notification: {
      title: `New post from ${data.author}`,
      body: String(data.text).slice(0, 120),
    },
    android: { priority: "default" },
  });
});
