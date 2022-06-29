package part3_stores_serialization

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

class SimplePersistentActor extends PersistentActor with ActorLogging {
    var nMessages = 0

    override def receiveRecover: Receive = {
      case SnapshotOffer(_, payload: Int) =>
        log.info(s"Recovered snapshot $payload")
        nMessages = payload
      case message =>
        log.info(s"Recovered $message")
        nMessages += 1
    }

    override def receiveCommand: Receive = {
      case "print" => log.info(s"I have persisted $nMessages so far")
      case "snap"  => saveSnapshot(nMessages)
      case SaveSnapshotSuccess(_) =>
        log.info(s"Save snapshot was successful")
      case SaveSnapshotFailure(_, cause) =>
        log.warning(s"Save snapshot failure $cause")
      case message =>
        persist(message) { e =>
          log.info(s"Persisting $message")
          nMessages += 1
        }
    }

    override def persistenceId: String = "simple-persistent-actor"
  }