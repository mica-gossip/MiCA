package org.princehouse.mica.example

import org.princehouse.mica.base.model.Protocol
import org.princehouse.mica.base.net.model.Address
import org.princehouse.mica.base.sugar.annotations.View
import org.princehouse.mica.base.BaseProtocol
import org.princehouse.mica.lib.abstractions.Overlay
import org.princehouse.mica.util.harness.ProtocolInstanceFactory
import org.princehouse.mica.util.harness.TestHarness
import scala.Math
import org.princehouse.mica.util.Distribution

/**
 * FindMin protocol written in Scala.
 *
 * Note: @View annotation syntactic sugar does not seem to work in Scala
 */
class ScalaMin(initvalue: Int, overlay: Overlay) extends BaseProtocol {

  var value = initvalue;

  override def getView: Distribution[Address] = overlay.getOverlay(getRuntimeState);

  override def update(other: Protocol) =
    other match {
      case that: ScalaMin =>
        that
        value = Math.min(value, that.value)
        that.value = value
      case _ => throw new RuntimeException
    }

}

/**
 * Main method to run this example
 */
object Main extends TestHarness with ProtocolInstanceFactory {
  def main(args: Array[String]) {
    runMain(args)
  }

  override def createProtocolInstance(nodeid: Int, addr: Address, overlay: Overlay): Protocol = {
    new ScalaMin(nodeid, overlay)
  }
}