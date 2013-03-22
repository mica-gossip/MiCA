package org.princehouse.mica.test.analysis
import java.io._
import soot._
import java.util.Arrays
import collection.JavaConversions._
import soot.jimple._
import soot.util._
import org.princehouse.mica.util.scala.SootUtils

object TestProxyGenerator {

  val j = Jimple.v
  
  def main(args: Array[String]): scala.Unit = {
    val p = new C1TestProtocol
    var targetClass = SootUtils.forceResolveJavaClass(p.getClass, SootClass.BODIES)
        
    var proxyFields:Set[SootField] = (targetClass.getFields().toSet[SootField]).filter(!SootUtils.isStatic(_))
    
    
    val updateMethod = targetClass.getMethod("void update(org.princehouse.mica.base.model.Protocol)")
    val thisRef = j.newThisRef(targetClass.getType()) 
    val refactorTargets = Map[SootMethod,Set[Value]](updateMethod -> Set(thisRef))
    
    
    val gen = new ProxyGenerator(targetClass, proxyFields, refactorTargets)
    
    val proxyClass = gen.createProxyClass
    
    SootUtils.writeSootClass(proxyClass)
    
  }

}