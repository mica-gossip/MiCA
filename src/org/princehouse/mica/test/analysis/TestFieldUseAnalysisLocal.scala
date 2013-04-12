package org.princehouse.mica.test.analysis

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import org.princehouse.mica.test.analysis.C1TestProtocol
import org.princehouse.mica.util.scala.SootUtils
import soot.options.Options
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.Local
import soot.PackManager
import soot.Scene
import soot.SootClass
import soot.SootMethod
import org.princehouse.mica.util.scala.SootViz

object TestFieldUseAnalysisLocal {

  def main(args: Array[String]): scala.Unit = {

    val protocolClass = classOf[C1TestProtocol]
    Scene.v().loadClassAndSupport(protocolClass.getName())
        val c = SootUtils.forceResolveJavaClass(protocolClass, SootClass.BODIES)
    c.setApplicationClass()
    Scene.v().loadNecessaryClasses()

    val entryMethod: SootMethod = c.getMethodByName("update")
    entryMethod.retrieveActiveBody()
    Scene.v().setEntryPoints(List(entryMethod))
   
    println("analyzing local used fields of entry method")
    for(field <- SootUtils.getUsedFields(entryMethod)) {
      println("  field: " + field)
    }
    
    // ------------------ analyze field usage
    
    
    // org.princehouse.mica.test.analysis

    //val sootArgs = Array[String]("-v","-w","-f","jimple","-dump-body","jb")
    //val sootArgs = Array[String]("-w", "-f", "jimple")
    // note: -dump-cfg ALL prints out a lotta spam...  view with dotview script

    //    PackManager.v().getPack("jtp").add(new
    //Transform("jpt.myanalysistagger",
    //MyAnalysisTagger.instance()));

    // SootUtils.packManager.getPack("jap").add(new C1MayUseFieldAnalysis)

    //val sootArgs = Array[String]("-w", "-f", "jimple")
    /*
    val sootArgs = Array[String]("-f", "jimple")
    SootUtils.options.parse(sootArgs)
    val sclass = SootUtils.forceResolveJavaClass(pclass, SootClass.BODIES)
    val Some(smethod) = SootUtils.getInheritedMethodByName(sclass, "update")
    val mclass = smethod.getDeclaringClass()
    println("Declaring class:" + mclass)
    mclass.setApplicationClass()
    SootUtils.scene.loadNecessaryClasses()
    SootUtils.scene.setEntryPoints(List(smethod))
    try {
      SootUtils.packManager.runPacks()
    } catch {
      case e: RuntimeException =>
        e.printStackTrace()
    }
    println("Done") */

    /*    var sclass = SootUtils.forceResolveJavaClass(pclass, SootClass.BODIES)
    val Some(smethod) = SootUtils.getInheritedMethodByName(sclass, "update")

    sclass = SootUtils.scene.loadClassAndSupport(smethod.getDeclaringClass.getName)
    sclass.setApplicationClass()
    val body = smethod.retrieveActiveBody()

    // Load the control flow graph
    val cfg: ExceptionalUnitGraph = new ExceptionalUnitGraph(body)
    SootViz.exportSootDirectedGraphToDot(cfg, "debug/cfg.dot")
*/
    // Get the PDG.  For info on this PDG implementation, see:
    // http://www.sable.mcgill.ca/soot/doc/soot/toolkits/graph/pdg/HashMutablePDG.html
    //   if (false) {
    //     val pdg = new HashMutablePDG(cfg)
    //     SootViz.exportSootDirectedGraphToDot(pdg, "debug/pdg.dot")
    //   }

    //pdg.constructPDG()
    // Initial flow info
    //val entryData = new UnitData()
    //entryData.addPath(new soot.jimple.ThisRef(smethod.getDeclaringClass().getType()), new Location("A"))
    //entryData.addPath(new soot.jimple.ParameterRef(smethod.getParameterType(0), 0), new Location("B"))

    //val flow = new TestDataFlow(cfg, entryData)
    //flow.go
  }
}


