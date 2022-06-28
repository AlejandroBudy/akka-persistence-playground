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
  case class InvoiceBulk(invoice: List[Invoice])
  case object Shutdown

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
    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
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
          totalAmount += e.amount
          log.info(s"Persisted $e as invoice #${e.id}, for total amount $totalAmount")
        }

      case InvoiceBulk(invoices) =>
        // 1. Create events (plural)
        // 2. Persist all the events
        // 3. Update the actor state when each event is persisted
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map { pair =>
          val id      = pair._2
          val invoice = pair._1
          InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }
        persistAll(events) {
          // executed after each event is stored
          e =>
            latestInvoiceId += 1
            totalAmount += e.amount
            log.info(s"Persisted $e as invoice #${e.id}, for total amount $totalAmount")
        }

      case Shutdown => context.stop(self)
    }

    // Should be unique for Actor
    override def persistenceId: String = "simple-accountant"

    // this method is called if persisting failed
    // the actor will be stopped
    // Best practice: Start the actor again after a while. Use Backoff supervisor
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persis $event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    // Called if journal fails to persist the event
    // the actor is resumed
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persist rejected $event because of $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val system     = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant])
  //  for (i <- 1 to 10)
  //    accountant ! Invoice("The sofa company", new Date, i * 1000)

  val newInvoices = for (i <- 1 to 5) yield Invoice("awesome chairs", new Date, i * 2000)
  accountant ! InvoiceBulk(newInvoices.toList)

  /**
   * Persist multiple events
   *
   * persistAll
   *
   * Never ever call persist or persistAll from future -> two thread calling persist breaks actor
   * encapsulation
   */

  /**
   * Shutdown of persist actor. Poison pill or kill is handled in other thread, we can risk stash
   * message before persisting
   *
   * Best practice => Declare own shutdown
   */

}
