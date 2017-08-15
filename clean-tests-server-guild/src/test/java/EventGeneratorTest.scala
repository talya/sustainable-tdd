import java.util.UUID

import com.wixpress.common.specs2.JMock
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class EventGeneratorTest extends SpecificationWithJUnit with JMock {
  "EventGenerator" should {
    "notify of provisioned events" in new Context {
      checking {
        oneOf(eventNotifier).notify(TpaProvisionedEvent(provisionedTpa1.id))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(provisionedTpa2.id))
      }

      eventGenerator.generateProvisionedEvents(Set(provisionedTpa1.id, provisionedTpa2.id))
    }
  }

  trait Context extends Scope {
    val eventNotifier = mock[EventNotifier]
    val eventGenerator = new TheEventGenerator()
    val provisionedTpa1 = TpaInstance(id = UUID.randomUUID, TpaInstance.PROVISIONED)
    val provisionedTpa2 = TpaInstance(id = UUID.randomUUID, TpaInstance.PROVISIONED)
  }
}
