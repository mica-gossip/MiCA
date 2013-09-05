package org.princehouse.mica.test.analysis
import org.princehouse.mica.base.c1.C1Compiler
import org.princehouse.mica.base.model.MiCA
import org.princehouse.mica.base.model.MicaRuntime
import org.princehouse.mica.base.model.Protocol
import org.princehouse.mica.base.net.model.Address
import org.princehouse.mica.base.sim.SimAddress
import org.princehouse.mica.base.sim.Simulator
import org.princehouse.mica.util.Distribution

object TestC1 {

  def main(args: Array[String]): Unit = {
    
    // Instantiate a protocol and set up its runtime environment (need to guarantee getRuntimeState is going to return something)
    val dist = new Distribution[Address]
    dist.put(new SimAddress("1"), 1)
    dist.put(new SimAddress("2"), 1)
    dist.put(new SimAddress("3"), 1)
    dist.put(new SimAddress("4"), 1)
    dist.ipnormalize()

    val p = new C1TestProtocol()
    
    val sim = new Simulator
    MiCA setRuntimeInterface sim
    
    val rt: MicaRuntime = sim.addRuntime(new SimAddress("0"), 0,
      1000, 1000, 500);

    sim.getRuntimeContextManager().setNativeRuntime(rt)
    val rts = p.getRuntimeState
    
    testCompiler(p)
    sim.getRuntimeContextManager.clear
  }

  def testCompiler(p:Protocol) = {
    // Now run it through the C1 compiler
    val compiler = new C1Compiler
    val agent = compiler compile p
  }
}

