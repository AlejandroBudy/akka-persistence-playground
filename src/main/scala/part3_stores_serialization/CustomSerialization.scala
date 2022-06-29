package part3_stores_serialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

case class RegisterUser(email: String, name: String)
case class UserRegistered(id: Int, email: String, name: String)

class UserRegistrationSerializer extends Serializer {
  override def identifier: Int = 53278

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event @ UserRegistered(id, email, name) =>
      println(s"Serializing event $event")
      s"[$id//$email//$name]".getBytes()
    case _ =>
      throw new IllegalAccessException("only user registration event supported in this serializer")
  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val string = new String((bytes))
    val values = string.substring(1, string.length - 1).split("//")
    val id     = values(0).toInt
    val email  = values(1)
    val name   = values(2)

    val result = UserRegistered(id, email, name)
    println(s"Serialized $result")
    result
  }
}

class UserRegistrationActor extends PersistentActor with ActorLogging {
  var currentId = 0
  override def receiveRecover: Receive = { case event @ UserRegistered(id, _, _) =>
    currentId = id
    log.info(s"Recovered: $event")
  }

  override def receiveCommand: Receive = { case RegisterUser(email, name) =>
    persist(UserRegistered(currentId, email, name)) { e =>
      currentId += 1
      log.info(s"Persisted: $e")
    }
  }

  override def persistenceId: String = "user-registration"
}
object CustomSerialization extends App {
  /*
   * send command to the actor
   * actor call persist
   * serializer serialized the event into bytes
   * the journal writes the bytes
   *  */

  val customSerialization =
    ActorSystem("CustomSerialization", ConfigFactory.load().getConfig("customSerializerDemo"))

  val persistentActor = customSerialization.actorOf(Props[UserRegistrationActor])

  //for (i <- 1 to 10)
    //persistentActor ! RegisterUser("email", s"name_$i")

}
