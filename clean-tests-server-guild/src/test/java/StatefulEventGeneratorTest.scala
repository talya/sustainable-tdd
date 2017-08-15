import java.util.UUID

import com.wixpress.common.specs2.JMock
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class StatefulEventGeneratorTest extends SpecificationWithJUnit with JMock {
  "StatefulEventGenerator" should {

    "notify of provisioned events" in new Context {
      ignoring(eventsStateDao)
      checking {
        oneOf(eventNotifier).notify(TpaProvisionedEvent(provisionedTpa1.id))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(provisionedTpa2.id))
      }

      eventGenerator.generateProvisionedEvents(Set(provisionedTpa1.id, provisionedTpa2.id))
    }

    "save failed events" in new Context {
      checking {
        allowing(eventNotifier).notify(TpaProvisionedEvent(provisionedTpa1.id)) willThrow new RuntimeException("no service for you")
        oneOf(eventsStateDao).addAll(Set(provisionedTpa1.id))
      }

      eventGenerator.generateProvisionedEvents(Set(provisionedTpa1.id))
    }
  }

  trait Context extends Scope {
    val eventNotifier = mock[EventNotifier]
    val eventsStateDao = mock[EventsStateDao]
    val eventGenerator = new StatefulEventGenerator(eventNotifier, eventsStateDao)
    val provisionedTpa1 = TpaInstance(id = UUID.randomUUID, TpaInstance.PROVISIONED)
    val provisionedTpa2 = TpaInstance(id = UUID.randomUUID, TpaInstance.PROVISIONED)
  }
}
