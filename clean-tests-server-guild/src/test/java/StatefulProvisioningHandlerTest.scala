import java.util.UUID

import com.wixpress.common.specs2.JMock
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

import scala.collection.mutable.{HashSet => MutableHashSet}

class StatefulProvisioningHandlerTest extends SpecificationWithJUnit with JMock {
  "StatefulEventGenerator" should {

    "notify of provisioned events" in new Context {
      checking {
        oneOf(eventNotifier).notify(TpaProvisionedEvent(tpaId))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(tpaId2))
      }

      provisioningHandler.handleEventsOf(Set(tpaId, tpaId2))
    }

    "save failed events" in new Context {
      checking {
        allowing(eventNotifier).notify(TpaProvisionedEvent(tpaId)) willThrow new RuntimeException("no service for you")
      }

      provisioningHandler.handleEventsOf(Set(tpaId))

      unacknowledgedEventsDao.getEventsWaitingForAck() must contain(exactly(tpaId))
    }

    "mark successfully notified events" in new Context {
      checking {
        allowing(eventNotifier).notify(TpaProvisionedEvent(tpaId))
      }

      provisioningHandler.handleEventsOf(Set(tpaId))

      unacknowledgedEventsDao.getEventsWaitingForAck() must beEmpty
    }

    "notify of previously failed events before notifying of the new ones" in new Context {
      val notAckedEventId = UUID.randomUUID()
      givenUnacknowledgedEvent(notAckedEventId)

      checking {
        oneOf(eventNotifier).notify(TpaProvisionedEvent(notAckedEventId))
        oneOf(eventNotifier).notify(TpaProvisionedEvent(tpaId))
      }

      provisioningHandler.handleEventsOf(Set(tpaId))

      unacknowledgedEventsDao.getEventsWaitingForAck() must beEmpty
    }
  }

  trait Context extends Scope {
    val eventNotifier = mock[EventNotifier]
    val unacknowledgedEventsDao = new InMemoryUnacknowledgedEventsDao
    val provisioningHandler = new StatefulProvisioningHandler(eventNotifier, unacknowledgedEventsDao, mock[BiEventGenerator])
    val tpaId = UUID.randomUUID
    val tpaId2 = UUID.randomUUID

    def givenUnacknowledgedEvent(notAckedEventId: UUID) = {
      unacknowledgedEventsDao.addEventsWaitingForAck(Set(notAckedEventId))
    }
  }
}

class InMemoryUnacknowledgedEventsDao() extends UnacknowledgedEventsDao {

  private val unacknowledgedIds = MutableHashSet[UUID]()

  override def markAcknowledged(id: UUID): Unit = unacknowledgedIds.remove(id)

  override def addEventsWaitingForAck(ids: Set[UUID]): Unit = ids foreach unacknowledgedIds.add

  override def getEventsWaitingForAck(): Set[UUID] = unacknowledgedIds.toSet
}

