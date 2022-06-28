package part2_event_sourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object MultiplePersists extends App {

  /*
  Diligent accountant: with every invoice, will persist two events
  - a tax record for the fiscal authority
  - an invoice record for personal logs or some auditing authority
   */

  // Command
  case class Invoice(recipient: String, date: Date, amount: Int)
  // Events
  case class TaxRecord(taxId: String, recordId: Int, date: Date, totalAmount: Int)
  case class InvoiceRecord(invoiceId: Int, recipient: String, date: Date, amount: Int)

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef): Props = Props(
      new DiligentAccountant(taxId, taxAuthority)
    )
  }

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef)
      extends PersistentActor
      with ActorLogging {
    var latestTaxRecordId     = 0
    var latestInvoiceRecordId = 0
    override def receiveRecover: Receive = { case event =>
      log.info(s"Recovered $event")
    }

    override def receiveCommand: Receive = { case Invoice(recipient, date, amount) =>
      persist(TaxRecord(taxId, latestTaxRecordId, date, amount / 3)) { record =>
        taxAuthority ! record
        latestTaxRecordId += 1
      }
      persist(InvoiceRecord(latestInvoiceRecordId, recipient, date, amount)) { invoiceRecord =>
        taxAuthority ! invoiceRecord
        latestInvoiceRecordId += 1
      }
    }

    override def persistenceId: String = "Diligent-Accountant"
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = { case message =>
      log.info(s"Received: $message")
    }
  }
  val system       = ActorSystem("MultiplePersists")
  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant   = system.actorOf(DiligentAccountant.props("UK2345_0987", taxAuthority))

  accountant ! Invoice("Sofa company", new Date, 2000)

  // Event Order is guarantee
}
