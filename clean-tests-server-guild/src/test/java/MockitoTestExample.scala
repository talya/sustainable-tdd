import java.util.UUID

import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class MockitoTestExample extends SpecificationWithJUnit with Mockito {

  "MetaSiteTpaCollector" should {

    "return only provisioned tpa and generate events for them" in new Context {
      metaSiteTpaCollector.collectProvisionedTpas(Seq(provisionedTpa, templateTpa)) must contain(exactly(provisionedTpa.id))
      there was one(eventGenerator).generateProvisionedEvent(provisionedTpa.id)
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
    tpas.foreach(tpa => eventGenerator.generateProvisionedEvent(tpa.id))
    tpas.collect { case tpa if tpa.state == TpaInstance.PROVISIONED => tpa.id }
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


