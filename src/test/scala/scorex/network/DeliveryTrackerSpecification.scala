package scorex.core.network

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import scorex.ObjectGenerators
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.crypto.hash.Blake2b256

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@SuppressWarnings(Array(
  "org.wartremover.warts.Null",
  "org.wartremover.warts.TraversableOps",
  "org.wartremover.warts.OptionPartial"))
class DeliveryTrackerSpecification extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with ObjectGenerators {

  property("basic ops") {
    implicit val system = ActorSystem()
    val probe = TestProbe("p")(system)
    implicit val nvsStub: ActorRef = probe.testActor

    val dt = FiniteDuration(3, MINUTES)

    val tracker = new DeliveryTracker(system, deliveryTimeout = dt, maxDeliveryChecks = 2, nvsStub)

    val cp = ConnectedPeer(new InetSocketAddress(55), null, null, null)

    val mtid = ModifierTypeId @@ (0: Byte)

    val modids = Seq(Blake2b256("1"), Blake2b256("2"), Blake2b256("3")).map(ModifierId @@ _)

    val notAdded = ModifierId @@ Blake2b256("4")

    tracker.expect(cp, mtid, modids)

    tracker.isExpecting(modids.head) shouldBe true

    tracker.isExpecting(notAdded) shouldBe false

    tracker.onReceive(mtid, modids.head, cp)

    tracker.isExpecting(modids.head) shouldBe false

    tracker.peerWhoDelivered(modids.head).get shouldBe cp


    tracker.onReceive(mtid, notAdded, cp)
    tracker.isSpam(notAdded) shouldBe true

    tracker.reexpect(cp, mtid, modids(1))
    tracker.isExpecting(modids(1)) shouldBe true

    tracker.reexpect(cp, mtid, modids(1))
    tracker.isExpecting(modids(1)) shouldBe false

    tracker.reexpect(cp, mtid, modids(1))
    tracker.isExpecting(modids(1)) shouldBe true

    tracker.reexpect(cp, mtid, modids.head)
    tracker.isExpecting(modids.head) shouldBe true
  }
}