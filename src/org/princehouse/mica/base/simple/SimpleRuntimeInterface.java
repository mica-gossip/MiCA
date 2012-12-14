package org.princehouse.mica.base.simple;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.model.RuntimeContextManager;
import org.princehouse.mica.base.model.RuntimeInterface;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

import fj.F;

public class SimpleRuntimeInterface extends RuntimeContextManager implements
		RuntimeInterface {
	private List<Runtime> runtimes = Functional.list();

	private Map<Runtime, Integer> startTimes = Functional.map();

	@Override
	public void reset() {
		stop();
		runtimes.clear();
		startTimes.clear();
		running = false;
	}

	@Override
	public Runtime addRuntime(Address address,
			long randomSeed, int roundLength, int startTime, int lockTimeout) {

		// fixme this pattern gives no way for Native Runtime to be set when protocol is initialized
		// (protocol constructor executes before this runtime exists!)
		// FIXME: rewrite to call setProtocolInstance after the call to addRuntime returns a runtime
		Runtime rt = new SimpleRuntime(address);
		startTimes.put(rt, startTime);
		rt.setRandomSeed(randomSeed);
		rt.setRoundLength(roundLength);
		rt.setLockWaitTimeout(lockTimeout);
		runtimes.add(rt);
		return rt;
	}

	private boolean running = false;

	@Override
	public void run() {
		Runtime arbitraryRuntime = runtimes.get(0);
		arbitraryRuntime.logJson(LogFlag.init, "mica-options",
				MiCA.getOptions());

		// wait for interrupt
		running = true;
		// start runtimes

		// sort runtimes by start time, ascending
		Collections.sort(runtimes, new Comparator<Runtime>() {
			@Override
			public int compare(Runtime r1, Runtime r2) {
				return startTimes.get(r1).compareTo(startTimes.get(r2));
			}
		});

		int t0 = 0;
		for (Runtime rt : runtimes) {
			int t1 = startTimes.get(rt);
			try {
				Thread.sleep(t1 - t0);
				System.out.printf("[sleep %s ms, start next runtime]\n", t1
						- t0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			t0 = t1;
			getRuntimeContextManager().setNativeRuntime(rt);
			rt.start();
			getRuntimeContextManager().clear();
		}

		try {
			try {
				while (running) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {

			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		stop(); // clean up
	}

	@Override
	public void stop() {
		running = false;
		for (Runtime rt : runtimes) {
			rt.stop();
		}
	}

	@Override
	public void scheduleTask(long delay, TimerTask task) {
		Timer timer = new Timer(true);
		timer.schedule(task, delay);
	}

	@Override
	public F<Integer, Address> getAddressFunc() {
		return null;
	}

	@Override
	public void logJson(Object flags, Address address, String eventType,
			Object obj) {
		getRuntimeContextManager().getNativeRuntime().logJson(flags, address,
				eventType, obj);
	}

	private RuntimeContextManager runtimeContextManager = new RuntimeContextManager();

	@Override
	public RuntimeContextManager getRuntimeContextManager() {
		return runtimeContextManager;
	}

}
