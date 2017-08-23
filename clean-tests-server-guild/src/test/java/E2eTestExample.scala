import java.util.UUID

import TpaInstance.PROVISIONED
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class E2eTestExample extends SpecificationWithJUnit with Mockito {

  "MetaSiteTpaCollector" should {

    "retry sending notifications on provisioned tpas" in new Context {
      val tpaId = UUID.randomUUID()
      givenFailedProvisionedNotificationFor(tpaId)

      SystemDriver.collectProvisionedTpas(tpaId) must contain(exactly(tpaId))

      there was one(TestEnv.overTheNetworkEventNotifier).notify(TpaProvisionedEvent(tpaId))
      there was one(TestEnv.biEventGenerator).generateProvisionEvent(tpaId, wasSuccessful = false) //should be failing - but it's green!
      // the validation should be on 'wasSuccessful=true' - this was an honest mistake
      // the test passes because there indeed was one call with wasSuccessful=false
      // also stays green with true!
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
  val biEventGenerator = mock[BiEventGenerator]
  val overTheNetworkEventNotifier = mock[EventNotifier] //would be an rpc proxy in an actual e2e
  val metaSiteTpasCollector = new MetaSiteTpaCollector(new StatefulProvisioningHandler(overTheNetworkEventNotifier, new MysqlUnacknowledgedEventsDao, biEventGenerator))
}

object SystemDriver {
  def collectProvisionedTpas(tpaId: UUID) = TestEnv.metaSiteTpasCollector.collectProvisionedTpas(Seq(TpaInstance(tpaId, PROVISIONED)))
}

//using this simple impl just for simplicity - we dont want to write real db code for now
class MysqlUnacknowledgedEventsDao extends InMemoryUnacknowledgedEventsDao
