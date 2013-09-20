package org.princehouse.mica.base.simple;

import static org.princehouse.mica.base.RuntimeErrorCondition.ACTIVE_GOSSIP_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.BIND_ADDRESS_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.GOSSIP_IO_ERROR;
import static org.princehouse.mica.base.RuntimeErrorCondition.INITIATOR_LOCK_TIMEOUT;
import static org.princehouse.mica.base.RuntimeErrorCondition.NULL_SELECT;
import static org.princehouse.mica.base.RuntimeErrorCondition.OPEN_CONNECTION_FAIL;
import static org.princehouse.mica.base.RuntimeErrorCondition.POSTUDPATE_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.PREUDPATE_EXCEPTION;
import static org.princehouse.mica.base.RuntimeErrorCondition.SELF_GOSSIP;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.RuntimeErrorCondition;
import org.princehouse.mica.base.exceptions.AbortRound;
import org.princehouse.mica.base.exceptions.FatalErrorHalt;
import org.princehouse.mica.base.model.CommunicationPatternAgent;
import org.princehouse.mica.base.model.MiCA;
import org.princehouse.mica.base.model.MicaRuntime;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.AcceptConnectionHandler;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.model.Connection;
import org.princehouse.mica.base.sim.StopWatch;
import org.princehouse.mica.util.Logging.SelectEvent;

/**
 * Basic Runtime implementation.
 * 
 * Nothing fancy: It just serializes and exchanges complete node state.
 * 
 */
public class SimpleRuntime extends MicaRuntime implements
		AcceptConnectionHandler {

	public static final boolean DEBUG_NETWORKING = false;
	public static Random rng = new Random();

	private ReentrantLock lock = new ReentrantLock();

	public SimpleRuntime(Address address) {
		super();
		setAddress(address);
	}

	@Override
	public void setAddress(Address address) {
		Address current = getAddress();
		if (current != null && !current.equals(address)) {
			throw new RuntimeException(
					"previous address non-null; cannot change the address of an existing runtime");
		}
		super.setAddress(address);
	}

	/**
	 * Entry point for SimpleRuntime. Starts a protocol in a new thread.
	 * 
	 * @param pinstance
	 *            Local protocol instance
	 * @param address
	 *            Local address
	 * @param daemon
	 *            Launch thread as a daemon
	 * @param intervalMS
	 *            Milliseconds to sleep between each gossip initiation
	 * @param randomSeed
	 *            Random seed to use for this runtime
	 * @return New Runtime instance
	 */
	public static MicaRuntime launch(final MicaRuntime rt,
			final Protocol pinstance, final boolean daemon,
			final int intervalMS, final long randomSeed, int lockWaitTimeoutMS) {
		rt.setProtocolInstance(pinstance);
		rt.setInterval(intervalMS);
		rt.setLockWaitTimeout(lockWaitTimeoutMS);
		rt.setRandom(new Random(randomSeed));
		((SimpleRuntime) rt).launchThread(daemon);
		return rt;
	}

	public void launchThread(final boolean daemon) {
		final SimpleRuntime rt = this;

		Thread t = new Thread() {
			public void run() {
				try {
					rt.run();
				} catch (InterruptedException e) {
					rt.stop();
				}
			}
		};
		t.setDaemon(daemon);
		t.start();
	}

	/**
	 * Entry point for SimpleRuntime. Starts a protocol in a new thread. (Calls
	 * through to launch(), with the daemon flag true)
	 * 
	 * @param pinstance
	 *            Local protocol instance
	 * @param address
	 *            Local address
	 * @param intervalMS
	 *            milliseconds to sleep between each gossip initiation
	 * @param randomSeed
	 *            random seed to be used for this runtime
	 * @return New Runtime instance
	 */
	public static MicaRuntime launchDaemon(SimpleRuntime rt,
			final Protocol pinstance, final Address address, int intervalMS,
			long randomSeed, int lockWaitTimeoutMS) {
		return launch(rt, pinstance, true, intervalMS, randomSeed,
				lockWaitTimeoutMS);
	}

	private Protocol pinstance;

	@Override
	public void acceptConnection(Address recipient, Connection connection)
			throws IOException, FatalErrorHalt, AbortRound {

		CommunicationPatternAgent pattern = MiCA.getCompiler().compile(
				getProtocolInstance());

		try {
			if (lock.tryLock(getLockWaitTimeout(), TimeUnit.MILLISECONDS)) {
				boolean locked = true;
				try {
					if (!running) {
						logJson(LogFlag.error, "mica-error-internal",
								"acceptConnection called on a stopped runtime");
						connection.close();
						return;
					}
					Serializable m1 = receiveObject(pattern, connection);
					if (m1 == null) {
						debug.printf("message is null!!!\n");
					}
					Serializable m2 = pattern.f2(this, m1);
					lock.unlock();
					locked = false;
					sendObject(pattern, connection, "m2", m2);
				} finally {
					if (locked) {
						lock.unlock();
					}
				}
			} else {
				if (!running) {
					logJson(LogFlag.error, "mica-error-internal",
							"acceptConnection called on a stopped runtime + lock failed");
					connection.close();
					return;
				}
				// failed to acquire lock; timeout
				logJson(LogFlag.error, "mica-error-accept-connection"); // sim-ok
				debug.printf(
						"%s accept: failed to acquire lock (timeout)\n", this);
				connection.close();
			}
		} catch (InterruptedException e) {
			handleError(RuntimeErrorCondition.INTERRUPTED, e);
		}
	}

	private boolean running = true;

	@Override
	public void run() throws InterruptedException {

		final Address address = getAddress();
		super.run();

		MiCA.getRuntimeInterface().getRuntimeContextManager()
				.setNativeRuntime(this);
		logState("initial"); // sim-ok
		MiCA.getRuntimeInterface().getRuntimeContextManager().clear();

		try {
			address.bind(this);
		} catch (IOException e1) {
			try {
				handleError(BIND_ADDRESS_EXCEPTION, e1);
			} catch (FatalErrorHalt e) {
				return;
			} catch (AbortRound e) {
				return; // same as fatal in this case
			}
		}

		long lastElapsedMS = 0L;

		double rate = 1.0;

		MiCA.getRuntimeInterface().getRuntimeContextManager()
				.setNativeRuntime(this);
		try {
			rate = getProtocolInstance().getRate();
		} catch (Throwable t) {
			try {
				handleError(RuntimeErrorCondition.RATE_EXCEPTION, t);
			} catch (FatalErrorHalt e) {
				this.stop();
				return;
			} catch (AbortRound e) {
				// ignore
			}
		} finally {
			MiCA.getRuntimeInterface().getRuntimeContextManager().clear();
		}

		int intervalMS = getInterval();
		int intervalLength = 0;
		StopWatch stopwatch = new StopWatch();

		try {
			// Main gossip loop
			while (running) {
				try {
					Connection connection = null;
					stopwatch.reset();
					Address partner = null;

					MiCA.getRuntimeInterface().getRuntimeContextManager()
							.setNativeRuntime(this);
					logJson(LogFlag.rate, "mica-rate", rate); // sim-ok
					MiCA.getRuntimeInterface().getRuntimeContextManager()
							.clear();

					intervalLength = (int) (((double) intervalMS) / rate);
					if (intervalLength <= 0) {
						System.err
								.printf("%s error: Rate * intervalMS <= 0.  Resetting to default.\n",
										this);
						intervalLength = intervalMS;
					}

					long sleepTime = intervalLength - lastElapsedMS;

					if (sleepTime < 0) {
						sleepTime = 0;
						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.setNativeRuntime(this);
						logJson(LogFlag.user, "notable-event-late"); // sim-ok
						MiCA.getRuntimeInterface().getRuntimeContextManager()
								.clear();
					}
					Thread.sleep(sleepTime);

					if (!running)
						break;

					if (lock.tryLock(getLockWaitTimeout(),
							TimeUnit.MILLISECONDS)) {

						CommunicationPatternAgent pattern = MiCA.getCompiler()
								.compile(getProtocolInstance());

						try {
							if (!running) {
								// recv thread may have shutdown while it
								// held
								// the lock.
								// now that we have it, test for this
								lock.unlock();
								break;
							}

							SelectEvent se = null;

							Protocol p = getProtocolInstance();
							MiCA.getRuntimeInterface()
									.getRuntimeContextManager()
									.setNativeRuntime(this);
							try {
								se = new SelectEvent();
								se.selected = p.getView().sample(
										p.getRuntimeState().getRandom());
								if (se.selected.equals(p.getAddress())) {
									se.selected = null;
								}
							} catch (Throwable e) {
								handleError(
										RuntimeErrorCondition.SELECT_EXCEPTION,
										e);
							} finally {
								MiCA.getRuntimeInterface()
										.getRuntimeContextManager().clear();
							}

							partner = se.selected;

							logJson(LogFlag.select, "mica-select", se); // sim-ok

							MiCA.getRuntimeInterface()
									.getRuntimeContextManager()
									.setNativeRuntime(this);
							try {
								// preUpdate is called even if partner is
								// invalid
								// (null or self address)
								getProtocolInstance().preUpdate(partner);
							} catch (Throwable t) {
								handleError(PREUDPATE_EXCEPTION, t);
							} finally {
								MiCA.getRuntimeInterface()
										.getRuntimeContextManager().clear();
							}

							MiCA.getRuntimeInterface()
									.getRuntimeContextManager()
									.setNativeRuntime(this);
							logState("preupdate"); // sim-ok
							MiCA.getRuntimeInterface()
									.getRuntimeContextManager().clear();

							if (getAddress().equals(partner)) {
								handleError(SELF_GOSSIP, null);
							} else if (partner == null) {
								handleError(NULL_SELECT, null);
							}

							try {
								connection = partner.openConnection();
							} catch (Exception ce) {
								handleError(OPEN_CONNECTION_FAIL, ce);
							}

							if (connection == null) {
								continue;
							}

							try {
								Serializable m1 = pattern.f1(this);
								sendObject(pattern, connection, "m1", m1);
								Serializable m2 = receiveObject(pattern,
										connection);
								pattern.f3(this, m2);
							} catch (AbortRound ar) {
								throw ar;
							} catch (FatalErrorHalt feh) {
								throw feh;
							} catch (Throwable t) {
								// May be a serialization problem
								handleError(ACTIVE_GOSSIP_EXCEPTION, t);
							} finally {
								try {
									connection.close();
								} catch (IOException e) {
									handleError(
											RuntimeErrorCondition.CLOSE_CONNECTION_EXCEPTION,
											e);
								}
							}

							MiCA.getRuntimeInterface()
									.getRuntimeContextManager()
									.setNativeRuntime(this);
							logState("gossip-initiator"); // sim-ok
							MiCA.getRuntimeInterface()
									.getRuntimeContextManager().clear();

							MiCA.getRuntimeInterface()
									.getRuntimeContextManager()
									.setNativeRuntime(this);
							try {
								getProtocolInstance().postUpdate();
								logState("postupdate"); // sim-ok
							} catch (Throwable t) {
								handleError(POSTUDPATE_EXCEPTION, t);
							} finally {
								MiCA.getRuntimeInterface()
										.getRuntimeContextManager().clear();
							}

							getRuntimeState().incrementRound();

							MiCA.getRuntimeInterface()
									.getRuntimeContextManager()
									.setNativeRuntime(this);
							try {
								rate = getProtocolInstance().getRate();
							} catch (Throwable t) {
								try {
									handleError(
											RuntimeErrorCondition.RATE_EXCEPTION,
											t);
								} catch (FatalErrorHalt e) {
									this.stop();
									return;
								} catch (AbortRound e) {
									// ignore
								}
							} finally {
								MiCA.getRuntimeInterface()
										.getRuntimeContextManager().clear();
							}
						} finally {
							lock.unlock();
						}
					} else {
						// failed to acquire lock within time limit; gossip
						handleError(INITIATOR_LOCK_TIMEOUT, null);
					}

					double sec = ((double) stopwatch.elapsed()) / 1000.0;
					MicaRuntime.debug.printf("%s -> %s, elapsed time %g s\n",
							this, partner, sec);
				} catch (AbortRound ar) {
					if (lock.isLocked() && lock.isHeldByCurrentThread()) {
						lock.unlock();
					// ... do nothing, and on to the next round...
					}
                    // FIXME: ideally how long should we sleep to recover from deadlock? 
                    int backoff = rng.nextInt(intervalLength);
					Thread.sleep(backoff);
				}
			} // end while
		} catch (FatalErrorHalt e) {
			stop();
			// fatalErrorHalt should have already shut down everything
		} // end while(running) loop
	}

	/*
	 * @Override protected void fatalErrorHalt(RuntimeErrorCondition condition)
	 * throws FatalErrorHalt { stop(); // passively signal that it's time to
	 * shut down try { getAddress().unbind();// try to unbind the listener }
	 * catch (NotBoundException e1) { } try { lock.unlock(); } catch
	 * (IllegalMonitorStateException e) { } throw new FatalErrorHalt(); }
	 */

	@Override
	public String toString() {
		return String.format("<rt %s>", getAddress());
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public Protocol getProtocolInstance() {
		return pinstance;
	}

	@Override
	public void setProtocolInstance(Protocol pinstance) {
		this.pinstance = pinstance;
	}

	@Override
	public ReentrantLock getProtocolInstanceLock() {
		return lock;
	}

	@Override
	public void start() {
		launchThread(true); // launch in a new thread
	}

	private <T extends Serializable> void sendObject(
			CommunicationPatternAgent agent, Connection connection,
			String logMessageName, T obj) throws FatalErrorHalt, AbortRound {

		byte[] data = null;

		data = agent.serialize(obj);
		logJson(LogFlag.serialization,
				"mica-serialize-bytes-" + logMessageName, data.length);

		byte[] lengthBytes = ByteBuffer.allocate(4).putInt(data.length).array();

		try {
			connection.getOutputStream().write(lengthBytes);
			connection.getOutputStream().write(data);
		} catch (SocketException se) {
            // FIXME: check that handleError is aborting on GOSSIP_IO_ERROR
			//handleError(GOSSIP_IO_ERROR, se);
			handleError(GOSSIP_IO_ERROR, new AbortRound());
		} catch (IOException e) {
            // FIXME: check that handleError is aborting on GOSSIP_IO_ERROR
			//handleError(GOSSIP_IO_ERROR, e);
			handleError(GOSSIP_IO_ERROR, new AbortRound());
		}

	}

	/*
	 * private void dumpDataBuffer(byte[] data) {
	 * debug.println(bytesToHex(data,data.length)); }
	 * 
	 * public static String bytesToHex(byte[] bytes, int stop) { final char[]
	 * hexArray =
	 * {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'}; char[]
	 * hexChars = new char[bytes.length * 2]; int v; for ( int j = 0; j < stop;
	 * j++ ) { v = bytes[j] & 0xFF; hexChars[j * 2] = hexArray[v >>> 4];
	 * hexChars[j * 2 + 1] = hexArray[v & 0x0F]; } return new String(hexChars);
	 * }
	 */

	private <T extends Serializable> T receiveObject(
			CommunicationPatternAgent agent, Connection connection)
			throws FatalErrorHalt, AbortRound {

		try {
			InputStream is = connection.getInputStream();
			byte[] lengthBytes = new byte[4];
			int offset_hdr = 0;
			while (offset_hdr < 4) {
			   int _bytesRead = is.read(lengthBytes, offset_hdr, 4 - offset_hdr);
			   if (_bytesRead <= 0) {
			    throw new AbortRound();
			   }
			   offset_hdr += _bytesRead;
			}

			int length = (lengthBytes[0] << 24) | (lengthBytes[1] << 16)
					| (lengthBytes[2] << 8) | (lengthBytes[3]);

			byte[] data = new byte[length];

			int offset = 0;
			int bytesRead = 0;
			while (offset < length) {
				bytesRead = is.read(data, offset, length - offset);
				if (bytesRead < 0) {
					debug.printf("ERROR when trying to get bytes from peer");
					//break;
			    	throw new AbortRound();
				}
				offset += bytesRead;
			}

			if (offset != length) {
				debug.printf("ERROR read %d bytes, expected %d\n", bytesRead,
						length);
				throw new RuntimeException(String.format(
						"EXPECTED %d bytes, read %d, ERROR\n", length,
						bytesRead));
			}

			return agent.<T>deserialize(data);

		} catch (SocketException e) {
            // FIXME check that handlError is aborting round on GOSSIP_IO_ERROR
			//handleError(GOSSIP_IO_ERROR, e);
			handleError(GOSSIP_IO_ERROR, new AbortRound());
		} catch (IOException e) {
            // FIXME check that handlError is aborting round on GOSSIP_IO_ERROR
			handleError(GOSSIP_IO_ERROR, new AbortRound());
			//handleError(GOSSIP_IO_ERROR, e);
		}
		return null; // unreachable
	}

}
