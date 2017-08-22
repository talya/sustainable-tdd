import java.util.UUID

import com.wixpress.common.specs2.JMock
import core.{InterestingTpaEventNotifier, TpaProvisionedEvent, TpaTypeProviderFacade}
import org.specs2.mutable.SpecificationWithJUnit

class SomeImplOfEventsNotifierTest extends SpecificationWithJUnit with JMock {

  "notify of provisioned events only if tpa is interesting" should {
    val tpaId = UUID.randomUUID
    val tpaId2 = UUID.randomUUID

    val tpaTypeProviderFacade = mock[TpaTypeProviderFacade]
    val someImplOfEventNotifier = new InterestingTpaEventNotifier(tpaTypeProviderFacade)

    checking {
      allowing(tpaTypeProviderFacade).tpaIsInteresting(tpaId) willReturn (true)
      allowing(tpaTypeProviderFacade).tpaIsInteresting(tpaId2) willReturn (false)

      //and the assertion we would have when we actually implemented notifying to some event bus...
    }

    someImplOfEventNotifier.notify(TpaProvisionedEvent(tpaId))
  }
}
