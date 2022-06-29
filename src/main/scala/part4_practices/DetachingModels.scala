package part4_practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventAdapter, EventSeq}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object DomainModel {
  case class User(id: String, email: String, name: String)
  case class Coupon(code: String, promotionAmount: Int)

  // command
  case class ApplyCoupon(coupon: Coupon, user: User)
  // event
  case class CouponApplied(code: String, user: User)
}

object DataModel {
  case class WrittenCouponApplied(code: String, userId: String, userEmail: String)
  case class WrittenCouponAppliedV2(
      code: String,
      userId: String,
      userEmail: String,
      username: String
  )
}

class ModelAdapter extends EventAdapter {
  import DataModel._
  import DomainModel._

  override def manifest(event: Any): String = "CMA"

  // actor -> serializer -> fromJournal -> to the actor
  override def toJournal(event: Any): Any = event match {
    case event @ CouponApplied(code, user) =>
      println(s"Converting $event to DATA model")
      WrittenCouponAppliedV2(code, user.id, user.email, user.name)
  }

  // journal -> serializer -> fromJournal -> to the actor
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case e @ WrittenCouponApplied(code, userId, userEmail) =>
      println(s"Converting $e to DOMAIN model")
      EventSeq.single(CouponApplied(code, User(userId, userEmail, "")))
    case e @  WrittenCouponAppliedV2(code, userId, userEmail, username) =>
      println(s"Converting $e to DOMAIN model")
      EventSeq.single(CouponApplied(code, User(userId, userEmail, username)))
    case other => EventSeq.single(other)

  }
}

object DetachingModels extends App {

  class CouponManagers extends PersistentActor with ActorLogging {
    import DomainModel._
    val coupons: mutable.Map[String, User] = new mutable.HashMap[String, User]()

    override def receiveRecover: Receive = { case event @ CouponApplied(code, user) =>
      log.info(s"Recovered $event")
      coupons.put(code, user)
    }

    override def receiveCommand: Receive = { case ApplyCoupon(coupon, user) =>
      // Coupon not used before
      if (!coupons.contains(coupon.code)) {
        persist(CouponApplied(coupon.code, user)) { event =>
          log.info(s"Persisted $event")
          coupons.put(coupon.code, user)
        }
      }
    }

    override def persistenceId: String = "coupon-manager"
  }

  val system = ActorSystem("DetachingModels", ConfigFactory.load().getConfig("detachingModels"))
  val couponManager = system.actorOf(Props[CouponManagers], "CouponManager")

  import DomainModel._

//  for (i <- 20 to 30) {
//    val coupon = Coupon(s"Mega coupon_$i", 100)
//    val user   = User(s"User_$i", "email", "alejandro")
//    couponManager ! ApplyCoupon(coupon, user)
//  }
}
