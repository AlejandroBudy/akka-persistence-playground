package part3_stores_serialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object LocalStores extends App {

  val localStoresActorSystem =
    ActorSystem("localStoresSystem", ConfigFactory.load().getConfig("localStores"))
  val persistentActor = localStoresActorSystem.actorOf(Props[SimplePersistentActor])

  for (i <- 1 to 10)
    persistentActor ! s"i love Akka $i"

  persistentActor ! "print"
  persistentActor ! "snap"

  for (i <- 11 to 20)
    persistentActor ! s"i love Akka $i"

}
