import java.util.UUID

import com.wixpress.common.specs2.JMock
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class StatefulEventGeneratorTest extends SpecificationWithJUnit with JMock {
  "StatefulEventGenerator" should {

    "notify of provisioned events" in new Context {
      ignoring(eventsStateDao)
      checking {
        oneOf(eventNotifier).notify(TpaProvisionedEvent(provisionedTpaId1))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(provisionedTpaId2))
      }

      eventGenerator.generateProvisionedEvents(Set(provisionedTpaId1, provisionedTpaId2))
    }

    "save failed events" in new Context {
      checking {
        allowing(eventNotifier).notify(TpaProvisionedEvent(provisionedTpaId1)) willThrow new RuntimeException("no service for you")
        oneOf(eventsStateDao).addEventsWaitingForAck(Set(provisionedTpaId1))
      }

      eventGenerator.generateProvisionedEvents(Set(provisionedTpaId1))
    }

    "mark successfully notified events" in new Context {
      checking {
        allowing(eventsStateDao).addEventsWaitingForAck(Set(provisionedTpaId1))
        allowing(eventNotifier).notify(TpaProvisionedEvent(provisionedTpaId1))
        oneOf(eventsStateDao).markSuccessful(provisionedTpaId1)
      }

      eventGenerator.generateProvisionedEvents(Set(provisionedTpaId1))
    }

    "notify of previously failed events before continuing" in new Context {
      val eventIdNotAcked = UUID.randomUUID()
      checking {
        allowing(eventsStateDao).eventsNotAcked() willReturn(Set(eventIdNotAcked))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(eventIdNotAcked))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(provisionedTpaId1))
      }

      eventGenerator.generateProvisionedEvents(Set(provisionedTpaId1))
    }
  }

  trait Context extends Scope {
    val eventNotifier = mock[EventNotifier]
    val eventsStateDao = mock[EventsStateDao]
    val eventGenerator = new StatefulEventGenerator(eventNotifier, eventsStateDao)
    val provisionedTpaId1 = UUID.randomUUID
    val provisionedTpaId2 = UUID.randomUUID
  }
}
