package org.princehouse.mica.experiment.dilation;

import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.util.Functional;

/**
 * Large experiment on degree-60 random graph
 * 
 * @author lonnie
 * 
 */
public class RunDilationExperiment3 extends DilationExperiment {

	@Override
	public MicaOptions defaultOptions() {
		MicaOptions options = super.defaultOptions();
		options.n = 500;
		options.implementation = "sim";
		options.graphType = "random";
		options.rdegree = 60;
		options.timeout = 3000;
		options.roundLength = 5000;
		options.stopAfter = 60;
		options.logsDisable = Functional.list(new String[]{
				"state","view","rate","select","merge","error"
		});
		return options;
	}
	
	@Override
	public void setExperimentOptions() {
		direction = Protocol.Direction.PUSHPULL;
	}
	
	public static void main(String[] args) {
		new RunDilationExperiment3().runMain(args);
	}

}
