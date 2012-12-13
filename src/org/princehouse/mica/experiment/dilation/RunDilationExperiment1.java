package org.princehouse.mica.experiment.dilation;

import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.util.Functional;

/**
 * Small, 20-node experiment with most of the logging enabled
 * 
 * @author lonnie
 * 
 */
public class RunDilationExperiment1 extends DilationExperiment {

	@Override
	public MicaOptions defaultOptions() {
		MicaOptions options = super.defaultOptions();
		options.n = 20;
		options.implementation = "simple";
		options.graphType = "complete";
		options.timeout = 2000;
		options.roundLength = 2000;
		options.stopAfter = 20;
		options.logsDisable = Functional.list(new String[]{"view"});
		//options.logsDisable = Functional.list(new String[]{
		//		"state view rate select merge error"
		//});
		return options;
	}
	
	@Override
	public void setExperimentOptions() {
		direction = Protocol.Direction.PUSHPULL;
	}
	
	public static void main(String[] args) {
		new RunDilationExperiment1().runMain(args);
	}

}
