package org.princehouse.mica.test.analysis
import java.io._
import soot._
import java.util.Arrays
import collection.JavaConversions._
import soot.jimple._
import soot.util._
import soot.jimple.spark.ondemand.pautil.SootUtil
import org.princehouse.mica.util.scala.SootUtils


class IdentifyRefactorTargets(method:SootMethod, initialTargets:Set[Value]) {
	val targets:(Set[Value],Boolean) = analyze

	def analyze:(Set[Value],Boolean) = {
	  (initialTargets,false)
	}
}