package part3_stores_serialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Cassandra extends App {

  val cassandraActorSystem =
    ActorSystem("cassandraActorSystem", ConfigFactory.load().getConfig("cassandraDemo"))

  val persistentActor = cassandraActorSystem.actorOf(Props[SimplePersistentActor])

  for (i <- 1 to 10)
    persistentActor ! s"i love Akka $i"

  persistentActor ! "print"
  persistentActor ! "snap"

  for (i <- 11 to 20)
    persistentActor ! s"i love Akka $i"
}
