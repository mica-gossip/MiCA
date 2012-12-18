package org.princehouse.mica.experiment.ecoop2013.layers;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;

import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaOptions;
import org.princehouse.mica.base.model.MicaRuntime;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sim.RestartEvent;
import org.princehouse.mica.base.sim.SimRuntime;
import org.princehouse.mica.base.sim.Simulator;
import org.princehouse.mica.base.sim.SimulatorEvent;
import org.princehouse.mica.example.TreeCountNodes;
import org.princehouse.mica.example.TreeLabelNodes;
import org.princehouse.mica.lib.MinAddressLeaderElection;
import org.princehouse.mica.lib.SpanningTreeOverlay;
import org.princehouse.mica.lib.abstractions.Merge;
import org.princehouse.mica.lib.abstractions.MergeCorrelated;
import org.princehouse.mica.lib.abstractions.MergeOperator;
import org.princehouse.mica.lib.abstractions.Overlay;
import org.princehouse.mica.util.Functional;
import org.princehouse.mica.util.Randomness;
import org.princehouse.mica.util.harness.TestHarness;

import fj.F;

/**
 * Tests leader election + spanning tree + counting + labeling
 * 
 * @author lonnie
 * 
 */
public class Ecoop2013Correlated extends TestHarness {
	public  int BEGIN_TRANSIENT_FAILURE;
	public  int END_TRANSIENT_FAILURE;
	public  int MEGA_DISRUPTION;

	
	public MergeOperator merge = null;

	public Ecoop2013Correlated() {
		super();
	}

	public void processOptions() {
		MicaOptions options = getOptions();
		options.stopAfter = 140;
		options.n = 100;
		options.roundLength = 100000;
		options.timeout = 1000;
		BEGIN_TRANSIENT_FAILURE = 40;
		END_TRANSIENT_FAILURE = 70;
		MEGA_DISRUPTION = 100;
		super.processOptions();
		
	}
	
	public void configure() {
		super.configure();
		merge = MergeCorrelated.operator;
		addTimerRounds(BEGIN_TRANSIENT_FAILURE, new TimerTask() {
			@Override
			public void run() {
				beginTransientFaults();
			}
		});
		
		
		addTimerRounds(END_TRANSIENT_FAILURE, new TimerTask() {
			@Override
			public void run() {
				endTransientFaults();
			}
		}); 
		
		addTimerRounds(MEGA_DISRUPTION, new TimerTask() {
			@Override
			public void run() {
				megaDisruption();
			}
		});
		
	}

	private void megaDisruption() {
		List<MicaRuntime> runtimes = MiCA.getRuntimeInterface().getRuntimes();
		Iterable<Address> addresses = Functional.map(runtimes,
				new F<MicaRuntime, Address>() {
					@Override
					public Address f(MicaRuntime rt) {
						return rt.getAddress();
					}
				});
		Set<Address> aset = Functional.set(addresses);
		Random rng = runtimes.get(0).getRandom();

		for (MicaRuntime rt : runtimes) {
			Merge p = (Merge) rt.getProtocolInstance();
			MinAddressLeaderElection leader = (MinAddressLeaderElection) ((Merge) p
					.getP1()).getP1();
			leader.setLeader(Randomness.choose(aset, rng));
		}
	}

	private List<MicaRuntime> fail = null;
	
	private void endTransientFaults() {
		for (MicaRuntime rt : fail) {
			SimulatorEvent e = new RestartEvent((SimRuntime)rt);
			e.t = Simulator.v().getClock();
			Simulator.v().schedule(e);
		}
	}
	
	private void beginTransientFaults() {
		List<MicaRuntime> runtimes = MiCA.getRuntimeInterface().getRuntimes();
		
		int tofail = runtimes.size()/10;
		
		System.out.printf("Failing %s of %s nodes\n", tofail, runtimes.size());
		
		fail = Functional.list();
		for(int i = 0; i < tofail+0; i++) {
			MicaRuntime rt = runtimes.get(i);
			if(rt.getAddress().toString().equals("n0")) continue;
			fail.add(runtimes.get(i));
		}
		
		for (MicaRuntime rt : fail) {
			System.out.printf("--> fail node %s\n", rt.getAddress());
			rt.stop();
		}
	}
	
	/**
	 * @param args
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) {
		TestHarness test = new Ecoop2013Correlated();
		test.runMain(args);
	}

	@Override
	public Protocol createProtocolInstance(int nodeId, Address address,
			Overlay overlay) {
		MinAddressLeaderElection leaderElection = new MinAddressLeaderElection(
				overlay);

		SpanningTreeOverlay tree = new SpanningTreeOverlay(leaderElection,
				overlay);

		TreeCountNodes counting = new TreeCountNodes(tree);
		TreeLabelNodes labeling = new TreeLabelNodes(counting);

		return merge.merge(merge.merge(leaderElection, labeling),
				merge.merge(tree, counting));
	}

}
