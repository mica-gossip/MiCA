package org.princehouse.mica.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class Serialization {

	public static byte[] serializeJava(Serializable obj) {
		ByteOutputStream buffer = new ByteOutputStream();
		try {
			new ObjectOutputStream(buffer).writeObject(obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return buffer.getBytes();
	}

	public static Serializable deserializeJava(byte[] data) {
		try {
			return (Serializable) new ObjectInputStream(
					new ByteArrayInputStream(data)).readObject();
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] serializeKryo(Serializable obj) {
		return serializeKryo(obj, getKryo());
	}
	
	public static byte[] serializeKryo(Serializable obj, Kryo k) {

		ByteOutputStream buffer = new ByteOutputStream();
		Output output = new Output(buffer);
		try {
			k.writeClassAndObject(output, obj);
		} catch (KryoException ke) {
			throw ke;
		}
		output.close();
		return buffer.getBytes();
	}

	public static Serializable deserializeKryo(byte[] data) {
		return deserializeKryo(data, getKryo());
	}
	
	public static Serializable deserializeKryo(byte[] data, Kryo k) {
		try {
			Serializable obj = (Serializable) k.readClassAndObject(
					new Input(new ByteArrayInputStream(data)));
			return obj;
		} catch (KryoException ke) {
			throw ke;
		}
	}

	public static byte[] serializeDefault(Serializable obj) {
		return serializeJava(obj); //fixme
	}

	public static Serializable deserializeDefault(byte[] data) {
		return deserializeJava(data); // fixme
	}

	private static ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>();

	private static Kryo getKryo() {
		if (kryo.get() == null) {
			Kryo k = KryoUtil.defaultKryo();
			kryo.set(k);
			return k;
		} else {
			return kryo.get();
		}
	}

}
