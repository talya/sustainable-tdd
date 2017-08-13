import java.util.UUID

import com.wixpress.common.specs2.JMock
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class JMockTestExample extends SpecificationWithJUnit with JMock {

  "MetaSiteTpaCollector" should {

    "return only provisioned tpa and generate events for them" in new Context {
      checking {
        oneOf(eventGenerator).generateProvisionedEvent(provisionedTpa.id)
      }
      metaSiteTpaCollector.collectProvisionedTpas(Seq(provisionedTpa, templateTpa)) must contain(exactly(provisionedTpa.id))
    }
  }

  trait Context extends Scope {
    val eventGenerator = mock[EventGenerator]
    val metaSiteTpaCollector = new MetaSiteTpaCollector(eventGenerator)

    val provisionedTpa = TpaInstance(id = UUID.randomUUID, TpaInstance.PROVISIONED)
    val templateTpa = TpaInstance(id = UUID.randomUUID, TpaInstance.TEMPLATE)
  }

}

class MetaSiteTpaCollector(eventGenerator: EventGenerator) {

  def collectProvisionedTpas(tpas: Seq[TpaInstance]) = {
    tpas.collect { case tpa if tpa.state == TpaInstance.PROVISIONED => {
      eventGenerator.generateProvisionedEvent(tpa.id)
      tpa.id
    }}
  }
}

trait EventGenerator {
  def generateProvisionedEvent(i: UUID): Unit
}

case class TpaInstance(id: UUID, state: String)
object TpaInstance {
  val PROVISIONED = "provisioned"
  val TEMPLATE = "template"
}


