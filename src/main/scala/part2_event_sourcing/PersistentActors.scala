package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object PersistentActors extends App {

  /*
  Scenario: we have a business and an accountant which keeps track of invoices
   */
  // Command
  case class Invoice(recipient: String, date: Date, amount: Int)
  // Event
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  class Accountant extends PersistentActor with ActorLogging {

    var latestInvoiceId = 0
    var totalAmount     = 0

    /**
     * Handler will be call on recovery
     */
    override def receiveRecover: Receive = { case InvoiceRecorded(id, _, _, amount) =>
      latestInvoiceId = id
      totalAmount += amount
    }

    /**
     * The "normal receive" method
     */
    override def receiveCommand: Receive = { case Invoice(recipient, date, amount) =>
      /*
        When you receive an event:
        1. you create event to persist in the store
        2. you persist the event, then pass in a callback that will get triggered once the event is written
        3. we update the actor state when the event has persisted
       */
      log.info(s"Receive invoice for amount: $amount")
      persist(
        InvoiceRecorded(latestInvoiceId, recipient, date, amount)
      ) /* Time gap: all other messages during this gap are stashed */
      { e =>
        // safe to access mutable state here
        latestInvoiceId += 1
        totalAmount += amount
        log.info(s"Persisted $e as invoice #${e.id}, for total amount $totalAmount")
      }
    }

    // Should be unique for Actor
    override def persistenceId: String = "simple-accountant"
  }

  val system     = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant])
  for (i <- 1 to 10)
    accountant ! Invoice("The sofa company", new Date, i * 1000)
}
