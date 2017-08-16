import java.util.UUID

import com.wixpress.common.specs2.JMock
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class StatefulProvisioningHandlerTest extends SpecificationWithJUnit with JMock {
  "StatefulEventGenerator" should {

    "notify of provisioned events" in new Context {
      ignoring(unacknowledgedEventsDao)
      checking {
        oneOf(eventNotifier).notify(TpaProvisionedEvent(tpaId))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(tpaId2))
      }

      provisioningHandler.handleEventsOf(Set(tpaId, tpaId2))
    }

    "save failed events" in new Context {
      checking {
        allowing(eventNotifier).notify(TpaProvisionedEvent(tpaId)) willThrow new RuntimeException("no service for you")
        oneOf(unacknowledgedEventsDao).addEventsWaitingForAck(Set(tpaId))
      }

      provisioningHandler.handleEventsOf(Set(tpaId))
    }

    "mark successfully notified events" in new Context {
      checking {
        allowing(unacknowledgedEventsDao).addEventsWaitingForAck(Set(tpaId))
        allowing(eventNotifier).notify(TpaProvisionedEvent(tpaId))
        oneOf(unacknowledgedEventsDao).markAcknowledged(tpaId)
      }

      provisioningHandler.handleEventsOf(Set(tpaId))
    }

    "notify of previously failed events before notifying of the new ones" in new Context {
      val notAckedEventId = UUID.randomUUID()
      checking {
        allowing(unacknowledgedEventsDao).getEventsWaitingForAck() willReturn(Set(notAckedEventId))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(notAckedEventId))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(tpaId))
      }

      provisioningHandler.handleEventsOf(Set(tpaId))
    }
  }

  trait Context extends Scope {
    val eventNotifier = mock[EventNotifier]
    val unacknowledgedEventsDao = mock[UnacknowledgedEventsDao]
    val provisioningHandler = new StatefulProvisioningHandler(eventNotifier, unacknowledgedEventsDao)
    val tpaId = UUID.randomUUID
    val tpaId2 = UUID.randomUUID
  }
}
