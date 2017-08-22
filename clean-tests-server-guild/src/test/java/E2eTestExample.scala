import java.util.UUID

import TpaInstance.PROVISIONED
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import EventCount._

class E2eTestExample extends SpecificationWithJUnit with Mockito {

  "MetaSiteTpaCollector" should {

    "retry sending notifications on provisioned tpas" in new Context {
      val tpaId = UUID.randomUUID()
      givenFailedProvisionedNotificationFor(tpaId)

      SystemDriver.collectProvisionedTpas(tpaId) must contain(exactly(tpaId))

      there was one(TestEnv.overTheNetworkEventNotifier).notify(TpaProvisionedEvent(tpaId))
      TestEnv.biEventGenerator received (1.successfulEvent and 1.failureEvents)
    }
  }

  trait Context extends Scope {

    def givenFailedProvisionedNotificationFor(tpaId: UUID) = {
      TestEnv.overTheNetworkEventNotifier.notify(TpaProvisionedEvent(tpaId)) throws new RuntimeException("No service for you")
      SystemDriver.collectProvisionedTpas(tpaId)

      //make the notifier go back to returning the implicit default 'Unit'
      org.mockito.Mockito.reset(TestEnv.overTheNetworkEventNotifier)
    }
  }

}

object TestEnv extends Mockito {
  val biEventGenerator = new InMemoryBiGenerator
  val tpaTypeProviderFacade = mock[TpaTypeProviderFacade]
  val overTheNetworkEventNotifier = new SomeImplOfEventNotifier(tpaTypeProviderFacade)

  val aspects = mock[RequestAspectStore]

  //would be an rpc proxy in an actual e2e
  val metaSiteTpasCollector = new MetaSiteTpaCollector(new StatefulProvisioningHandler(overTheNetworkEventNotifier, new MysqlUnacknowledgedEventsDao, biEventGenerator, aspects))

}

object SystemDriver {
  def collectProvisionedTpas(tpaId: UUID) = TestEnv.metaSiteTpasCollector.collectProvisionedTpas(Seq(TpaInstance(tpaId, PROVISIONED)))
}

//using this simple impl just for simplicity - we dont want to write real db code for now
class MysqlUnacknowledgedEventsDao extends InMemoryUnacknowledgedEventsDao

class InMemoryBiGenerator extends BiEventGenerator {
  private var successfulEventsCounter = 0
  private var failureEventsCounter = 0

  override def generateProvisionEvent(tpaId: UUID, wasSuccessful: Boolean, user: Option[UUID]): Unit =
    if (wasSuccessful) successfulEventsCounter += 1
    else failureEventsCounter += 1

  def successfulEventsCounterIs(count: Int) = {
    assert(successfulEventsCounter == count, s"successfulEventsCounter num should be $count but is $successfulEventsCounter")
  }

  def failureEventsCounterIs(count: Int) = {
    assert(failureEventsCounter == count, s"failureEventsCounter num should be $count but is $failureEventsCounter")
  }

  def received(eventCount: EventCount) = {
    successfulEventsCounterIs(eventCount.successful)
    failureEventsCounterIs(eventCount.failure)
  }

}

case class EventCount(successful: Int, failure: Int) {
  def and(other: EventCount) = EventCount(successful + other.successful, failure + other.failure)
}

object EventCount {
  implicit class `event counter int creators`(i: Int) {
    def successfulEvent = successfulEvents
    def successfulEvents = EventCount(i, 0)

    def failureEvent = failureEvents
    def failureEvents = EventCount(0, i)
  }
}