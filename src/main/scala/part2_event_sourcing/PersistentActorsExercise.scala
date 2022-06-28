package part2_event_sourcing

import akka.actor.ActorLogging
import akka.persistence.PersistentActor

import scala.collection.mutable

object PersistentActorsExercise extends App {

  /*
  Persist actor for a voting station
  Keep:
  - the citizen who voted
  - the poll: mapping between a candidate and the number of received votes so far
  The actor must be able to recover its state if it's shut down or restarted
   */

  case class Vote(citizenPID: String, candidate: String)

  class VotingStationActor extends PersistentActor with ActorLogging {

    var citizens: mutable.Set[String]      = new mutable.HashSet[String]()
    var poll: mutable.HashMap[String, Int] = new mutable.HashMap[String, Int]()

    override def receiveRecover: Receive = { case Vote(citizenPID, candidate) =>
      citizens.add(citizenPID)
      val votes = poll.getOrElse(candidate, 0)
      poll.put(candidate, votes + 1)
    }

    override def receiveCommand: Receive = { case vote @ Vote(citizenPID, candidate) =>
      if (!citizens.contains(citizenPID)) {
        persist(vote) { e =>
          citizens.add(citizenPID)
          val votes = poll.getOrElse(candidate, 0)
          poll.put(candidate, votes + 1)
        }
      }

    }

    override def persistenceId: String = "voting-station-actor"
  }
}
