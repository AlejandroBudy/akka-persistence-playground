package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable

object Snapshots extends App {

  // commands
  case class ReceivedMessage(contents: String)
  case class SentMessage(contents: String)

  // events
  case class ReceivedMessageRecord(id: Int, contents: String)
  case class SentMessageRecord(id: Int, contents: String)

  object Chat {
    def props(owner: String, contact: String): Props = Props(new Chat(owner, contact))
  }
  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging {
    val MAX_MESSAGES = 10

    var commandsWithoutCheckpoint = 0
    var currentMessageId          = 0
    var lastMessages              = new mutable.Queue[(String, String)]()

    override def persistenceId: String = s"$owner-$contact-chat"

    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id, contents) =>
        log.info(s"Recovered receive message $id: $contents")
        maybeReplaceMessage(contact, contents)
        currentMessageId = id

      case SentMessageRecord(id, contents) =>
        log.info(s"Recovered sent message $id: $contents")
        maybeReplaceMessage(contact, contents)
        currentMessageId = id

      case SnapshotOffer(metadata, contents) =>
        log.info(s"Recovered snapshot: $metadata")
        contents
          .asInstanceOf[mutable.Queue[(String, String)]]
          .foreach(lastMessages.enqueue(_))
    }

    override def receiveCommand: Receive = {
      case ReceivedMessage(contents) =>
        persist(ReceivedMessageRecord(currentMessageId, contents)) { e =>
          log.info(s"Received message: $contents")
          maybeReplaceMessage(contact, contents)
          maybeCheckpoint()
          currentMessageId += 1
        }
      case SentMessage(contents) =>
        log.info(s"Sent message: $contents")
        maybeReplaceMessage(owner, contents)
        maybeCheckpoint()
        currentMessageId += 1
      // snapshot-related messages
      case SaveSnapshotSuccess(metadata) =>
        log.info(s"saving snapshot succeeded: $metadata")
      case SaveSnapshotFailure(metadata, reason) =>
        log.warning(s"saving snapshot $metadata failed because of $reason")
    }

    private def maybeCheckpoint(): Unit = {
      commandsWithoutCheckpoint += 1
      if (commandsWithoutCheckpoint >= MAX_MESSAGES) {
        log.info(s"Saving checkpoints...")
        saveSnapshot(lastMessages)
        commandsWithoutCheckpoint = 0
      }
    }

    private def maybeReplaceMessage(sender: String, contents: String) = {
      if (lastMessages.size >= MAX_MESSAGES) {
        lastMessages.dequeue()
      }
      lastMessages.enqueue((sender, contents))
    }
  }

  val system = ActorSystem("ChatDemo")
  val chat   = system.actorOf(Chat.props("alex", "budy"))

  for (i <- 1 to 100000) {
    chat ! ReceivedMessage(s"akka rocks $i")
    chat ! SentMessage(s"Akka rules $i")
  }

  // Replace takes "long" -> Snapshots

  /*
    pattern:
    - after each persist, maybe save a snapshot (logic is up to you)
    - if you save a snapshot, handle the SnapshotOffer message in receiveRecover
    - (optional, but best practice) handle SaveSnapshotSuccess and SaveSnapshotFailure in receiveCommand
   */

}
