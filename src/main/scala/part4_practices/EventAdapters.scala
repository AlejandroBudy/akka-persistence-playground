package part4_practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventSeq, ReadEventAdapter}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object EventAdapters extends App {

  val ACOUSTIC = "acoustic"
  val ELECTRIC = "electric"
  // Store for acoustic guitars
  case class Guitar(id: String, model: String, make: String, guitarType: String)
  // Command
  case class AddGuitar(guitar: Guitar, quantity: Int)
  // Event
  case class GuitarAdded(
      guitarId: String,
      guitarModel: String,
      guitarMake: String,
      quantity: Int
  )
  case class GuitarAddedV2(
      guitarId: String,
      guitarModel: String,
      guitarMake: String,
      quantity: Int,
      guitarType: String
  )

  class InventoryManager extends PersistentActor with ActorLogging {
    val inventory: mutable.Map[Guitar, Int] = new mutable.HashMap[Guitar, Int]()

    override def receiveRecover: Receive = {
      // only handle the latest version
      case event @ GuitarAddedV2(id, model, make, quantity, guitarType) =>
        log.info(s"Recovered $event")
        val guitar          = Guitar(id, model, make, guitarType)
        val currentQuantity = inventory.getOrElse(guitar, 0)
        inventory.put(guitar, currentQuantity + quantity)
    }

    override def receiveCommand: Receive = {
      case AddGuitar(guitar @ Guitar(id, model, make, guitarType), quantity) =>
        persist(GuitarAddedV2(id, model, make, quantity, guitarType)) { _ =>
          val existingQuantity = inventory.getOrElse(guitar, 0)
          inventory.put(guitar, existingQuantity + 1)
          log.info(s"Added $quantity x $guitar to inventory")
        }
      case "print" => log.info(s"Current inventory is $inventory")
    }

    override def persistenceId: String = "guitar-inventory-manger"
  }

  class GuitarReadEventAdapter extends ReadEventAdapter {
    /*
     * journal -> serializer -> read event adapter -> actor
     * (bytes)   (guitarAdded)  (GAV2)                (receiveRecover)
     *  */

    override def fromJournal(event: Any, manifest: String): EventSeq = event match {
      case GuitarAdded(guitarId, guitarModel, guitarMake, quantity) =>
        // Acoustic default value
        EventSeq.single(GuitarAddedV2(guitarId, guitarModel, guitarMake, quantity, ACOUSTIC))
      case other => EventSeq.single(other)
    }
  }

  val system = ActorSystem("EventAdapters", ConfigFactory.load().getConfig("eventAdapters"))
  val inventoryManager = system.actorOf(Props[InventoryManager], "InventoryManager")

  val guitar = for (i <- 1 to 10) yield Guitar(s"$i", s"HakkerV2 $i", "AlejandroMaker", ELECTRIC)
  // guitar.foreach(guitar => inventoryManager ! AddGuitar(guitar, 5))
}
