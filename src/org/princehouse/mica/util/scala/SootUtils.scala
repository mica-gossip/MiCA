package org.princehouse.mica.util.scala
import soot.SootMethod
import soot.SootClass
import soot.Scene
import soot.PackManager
import soot.options.Options

object SootUtils {

  // wrap the soot singletons 
  def scene = Scene.v
  def options = Options.v
  def packManager = PackManager.v
    
  /**
   * A more scala-like implementation of getMethodByName
   */
  def getMethodByName(sc:SootClass, name:String) : Option[SootMethod] = {
    try {
      Some(sc.getMethodByName(name))
    } catch {
      case e:RuntimeException => None
    }
  }
  
  def forceResolveJavaClass(klass:Class[_], level:Int) : SootClass = {
		var className = klass.getCanonicalName()
		scene.forceResolve(className, level)
	}
	
  /**
   * Returns None if the supplied class is java.lang.Object.  Note that interfaces are classes, and are subclasses of Object
   */
  def getSuperclass(sc:SootClass) : Option[SootClass] = {
	if(sc.hasSuperclass) {
	  Some(sc.getSuperclass())
	} else {
	  None
	}
  }
  
  /**
   * Like getMethodByName, but searches superclasses until it finds an implementation
   */
  def getInheritedMethodByName(sc:SootClass, name:String) : Option[SootMethod] = {
    getMethodByName(sc,name) match  {
      case Some(method) => Some(method)
      case None => {
        getSuperclass(sc) match {
          case Some(superclass) => getInheritedMethodByName(superclass, name) 
          case None => None
        }
      }
    }
 }
}