package org.princehouse.mica.base.sim;

import fj.F;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimerTask;
import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.exceptions.MicaException;
import org.princehouse.mica.base.model.Compiler;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaRuntime;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.model.RuntimeContextManager;
import org.princehouse.mica.base.model.RuntimeInterface;
import org.princehouse.mica.base.net.dummy.DummyAddress;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Functional;

/**
 * Single-threaded MiCA emulator.
 *
 * @author lonnie
 */
public class Simulator implements RuntimeInterface {

  private long clock = 0; // current clock

  public long getClock() {
    return clock;
  }

  @Override
  public void reset() {
    stop();
    clock = 0;
    eventQueue.clear();
    addressBindings.clear();
    lockHolders.clear();
    lockWaitQueues.clear();
    unlockedQueue.clear();
    running = false;
  }

  public void setClock(long clock) {
    this.clock = clock;
  }

  private LinkedList<SimulatorEvent> eventQueue = new LinkedList<SimulatorEvent>();

  private Map<Address, SimRuntime> addressBindings = Functional.map();

  // maps lock_address -> lock_holder_address
  //
  // lock_holder will never be none (keys will just be removed)
  // yes, we could track this with a set, but this lets us sanity-check the
  // property
  // that only a lock-holder should be able to release a lock
  private Map<Address, Address> lockHolders = new HashMap<Address, Address>();

  private Map<Address, List<SimulatorEvent>> lockWaitQueues = Functional.map();

  private List<Address> unlockedQueue = Functional.list();

  public void bind(Address address, SimRuntime rt, int starttime) {
    addressBindings.put(address, rt);
    markUnlocked(address);
    new SimRound(address, this, starttime);
  }

  public void unbind(SimRuntime rt) {
    addressBindings.remove(rt.getAddress());
    Address a = rt.getAddress();

    // if anyone is holding our lock, clear the entry so that other waiters
    // will get cleared up
    if (lockHolders.containsKey(a)) {
      unlock(a, lockHolders.get(a));
    }

    // if we're holding any locks, release them
    for (Map.Entry<Address, Address> me : lockHolders.entrySet()) {
      if (me.getValue().equals(a)) {
        unlock(me.getKey(), a);
      }
    }
  }

  /**
   * Returns false if lock failed, true if successful otherwise Fails if lock already held by
   * someone else, or if the address is not bound
   *
   * @param lock
   * @return
   */
  public static boolean SPAM = false;

  protected boolean lock(Address lock, Address requestor) {
    if (!addressBindings.containsKey(lock)) {
      return false;
    }
    if (lockHolders.containsKey(lock)) {
      return false;
    } else {
      lockHolders.put(lock, requestor);
      if (SPAM) {
        System.err.printf("%s lock(%s)\n", requestor, lock);
      }
      return true;
    }
  }

  // lock is being released at time t
  protected void unlock(Address lock, Address requestor) {
    if (SPAM) {
      System.err.printf("%s unlock(%s)\n", requestor, lock);
    }

    if (lockHolders.containsKey(lock)) {
      Address holder = lockHolders.get(lock);
      if (!requestor.equals(holder)) {
        throw new RuntimeException("tried to unlock an address locked by someone else");
      }
      lockHolders.remove(lock);
      markUnlocked(lock);
    } else {
      throw new RuntimeException("tried to unlock an already-unlocked address");
    }
  }

  protected void markUnlocked(Address a) {
    unlockedQueue.add(a);
  }

  @Override
  public Compiler getDefaultCompiler() {
    return new FakeCompiler();
  }

  /*
   * if(lockWaitQueues.containsKey(a)) { List<SimulatorEvent> lockq =
   * lockWaitQueues.get(a); while(lockq.size() > 0) { SimulatorEvent triggered
   * = lockq.remove(0); if(triggered.isCancelled()) continue;
   * triggered.execute(this); return; } } locked.remove(a); }
   */

  protected void addLockWaiter(Address a, SimulatorEvent acquisitionCallback) {
    if (!lockWaitQueues.containsKey(a)) {
      lockWaitQueues.put(a, Functional.<SimulatorEvent>list());
    }
    acquisitionCallback.t = getClock();
    schedule(acquisitionCallback, lockWaitQueues.get(a));
  }

  public void schedule(SimulatorEvent e) {
    schedule(e, eventQueue);
  }

  public void scheduleRelative(SimulatorEvent e, long offset) {
    assert (offset >= 0);
    e.t = getClock() + offset;
    schedule(e, eventQueue);
  }

  public SimulatorEvent pollEventQueue(List<SimulatorEvent> queue) {
    if (queue == null) {
      return null;
    }

    while (queue.size() > 0) {
      SimulatorEvent e = queue.remove(0);
      if (e.isCancelled()) {
        // SimRuntime.debug.printf("   (cancelled)@%d   %s\n", e.t,
        // e.toString());
        continue;
      } else {
        return e;
      }
    }
    return null;
  }

  public void schedule(SimulatorEvent e, List<SimulatorEvent> queue) {
    assert (e != null);
    assert (e.t >= getClock());

    // this is a very common case --- insert an event that happens
    // immediately
    if (queue.size() == 0 || queue.get(0).t >= e.t) {
      queue.add(0, e);
      return;
    }

    // backwards O(n) linked list insert, running on the assumption that the
    // inserted event will likely be scheduled
    // later than existing events
    for (ListIterator<SimulatorEvent> it = queue.listIterator(queue.size()); it.hasPrevious(); ) {
      if (it.previous().t <= e.t) {
        it.next();
        it.add(e);
        break;
      }
    }
  }

  @SuppressWarnings("unused")
  private boolean queueIsCorrectlySorted(List<SimulatorEvent> queue) {
    // sanity check --- O(n), use sparingly
    long t = 0;
    for (SimulatorEvent e : queue) {
      if (e.t < t) {
        return false;
      }
      t = e.t;
    }
    return true;
  }

  private boolean running = false;

  private SimulatorEvent getNextEvent() {
    while (unlockedQueue.size() > 0) {
      Address a = unlockedQueue.remove(0);
      SimulatorEvent callback = pollEventQueue(lockWaitQueues.get(a));
      if (callback != null) {
        callback.t = getClock();
        return callback;
      }
    }
    return pollEventQueue(eventQueue);
  }

  @Override
  public void run() {
    // run the simulation
    for (MicaRuntime rt : getRuntimesSim()) {
      rt.start();
    }

    running = true;
    setClock(0L);

    StopWatch simtimer = new StopWatch();
    simtimer.reset();

    long round = 0;
    int roundSize = MiCA.getOptions().roundLength;

    // write options message to the first runtime
    MicaRuntime arbitraryRuntime = addressBindings.values().iterator().next();
    arbitraryRuntime.logJson(LogFlag.init, "mica-options", MiCA.getOptions());

    while (running) {
      long curRound = (getClock() / roundSize) + 1;
      if (curRound != round) {
        String stopsfx;
        if (MiCA.getOptions().stopAfter > 0) {
          stopsfx = String.format(" of %s", (int) MiCA.getOptions().stopAfter);
        } else {
          stopsfx = "";
        }

        SimRuntime.debug.printf("(%s) round %d%s\n", MiCA.getOptions().expname, curRound, stopsfx);
        round = curRound;
      }
      SimulatorEvent e = getNextEvent();
      if (e == null) {
        break;
      }

      long clock = getClock();

      // String msg = String.format("@%d -> %d execute %s", clock, e.t,
      // e.toString());
      // rt.logJson("debug-event", msg);
      // SimRuntime.debug.println(msg);

      assert (e.t >= clock);
      setClock(e.t);

      Address src = e.getSrc();
      if (src != null) {
        SimRuntime rt = getRuntime(src);
        if (rt == null) {
          // ignore event; runtime is dead
          continue;
        }
      }

      try {
        e.execute(this);
      } catch (AbortRound ex) {
        if (e instanceof SimRound.RoundEvent) {
          e.abortRound(this);
        }
      } catch (FatalErrorHalt ex) {
        if (e instanceof SimRound.RoundEvent) {
          e.abortRound(this);
        }
        if (src != null) {
          killRuntime(getRuntime(src));
        }
      } catch (MicaException ex) {
        // dead code
        ex.printStackTrace();
      }
    }
    running = false;

    double sfac = ((double) getClock()) / ((double) simtimer.elapsed() + 1);
    SimRuntime.debug.printf("Simulator stopped @%d; speed-up factor of %f\n", getClock(), sfac);
  }

  protected void stopRuntime(SimRuntime rt) {
    unbind(rt);
  }

  // Simulator is a singleton...
  private static Simulator singleton = null;

  public static Simulator v() {
    if (singleton == null) {
      singleton = new Simulator();
    }
    return singleton;
  }

  protected SimRuntime getRuntime(Address a) {
    SimRuntime rt = addressBindings.get(a);
    return rt;
  }

  public Protocol getReceiver(SimConnection sc) {
    throw new UnsupportedOperationException();
  }

  public void killRuntime(SimRuntime rt) {
    rt.stop();
  }

  public Protocol getSender(SimConnection sc) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MicaRuntime addRuntime(Address address, long randomSeed, int roundLength, int startTime,
      int lockTimeout) {
    SimRuntime rt = new SimRuntime(address);
    rt.setRandomSeed(randomSeed);
    rt.setRoundLength(roundLength);
    rt.setLockWaitTimeout(lockTimeout);
    bind(address, rt, startTime);
    return rt;
  }

  public void restart(SimRuntime rt) {
    bind(rt.getAddress(), rt, 0);
  }

  @Override
  public void scheduleTask(long delay, TimerTask task) {
    this.scheduleRelative(new TimerEvent(null, task), delay);
  }

  @Override
  public void stop() {
    running = false;
  }

  /**
   * Default node name is options.expname + i
   */
  @Override
  public F<Integer, Address> getAddressFunc() {
    return new F<Integer, Address>() {
      @Override
      public Address f(Integer i) {
        return new DummyAddress(String.format("%s%d", MiCA.getOptions().expname, i));
      }
    };
  }

  @Override
  public void logJson(Object flags, Address origin, String eventType, Object obj) {
    getRuntimeContextManager().getNativeRuntime().logJson(flags, origin, eventType, obj);
  }

  private RuntimeContextManager runtimeContextManager = new RuntimeContextManager();

  @Override
  public RuntimeContextManager getRuntimeContextManager() {
    return runtimeContextManager;
  }

  private Collection<SimRuntime> getRuntimesSim() {
    return addressBindings.values();
  }

  @Override
  public List<MicaRuntime> getRuntimes() {
    List<MicaRuntime> temp = Functional.list();
    for (MicaRuntime rt : getRuntimesSim()) {
      temp.add(rt);
    }
    return temp;
  }

  /**
   * Who currently has the lock? Returns null if no lock holder
   *
   * @param lock
   * @return
   */
  public Address getLockHolder(Address lock) {
    return lockHolders.get(lock);
  }

}
