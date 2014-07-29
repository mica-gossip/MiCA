package org.princehouse.mica.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.princehouse.mica.base.model.MiCA;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class Serialization {

    public static byte[] serializeJava(Serializable obj) {
    	// Note: ByteOutputStream is specific to Oracle's JDK. Will not work with OpenJDK.
    	// FIXME: Replace with something better supported
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
            return (Serializable) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] serializeKryo(Serializable obj) {
        return serializeKryo(obj, getKryo());
    }

    public static byte[] serializeKryo(Serializable obj, Kryo k) {      
    	// Note: ByteOutputStream is specific to Oracle's JDK. Will not work with OpenJDK.
    	// FIXME: Replace with something better supported
        ByteOutputStream buffer = new ByteOutputStream();
        Output output = new Output(buffer);
        try {
            k.writeClassAndObject(output, obj);
        } catch (KryoException ke) {
            throw ke;
        }
        output.close();
        byte[] bytes = buffer.getBytes();
        assert (bytes != null);
        return bytes;
       
    }

    public static Serializable deserializeKryo(byte[] data) {
        return deserializeKryo(data, getKryo());
    }

    public static Serializable deserializeKryo(byte[] data, Kryo k) {

        try {
            Serializable obj = (Serializable) k.readClassAndObject(new Input(new ByteArrayInputStream(data)));
            return obj;
        } catch (KryoException ke) {
            throw ke;
        }
    }

    public static byte[] serializeDefault(Serializable obj) {
        String sOpt = MiCA.getOptions().serializer;
        if (sOpt.equals("java")) {
            return serializeJava(obj);
        } else if (sOpt.equals("kryo")) {
            return serializeKryo(obj);
        } else {
            throw new RuntimeException("unrecognized default serializer option: " + sOpt);
        }
    }

    public static Serializable deserializeDefault(byte[] data) {
        String sOpt = MiCA.getOptions().serializer;
        if (sOpt.equals("java")) {
            return deserializeJava(data); // fixme
        } else if (sOpt.equals("kryo")) {
            return deserializeKryo(data);
        } else {
            throw new RuntimeException("unrecognized default serializer option: " + sOpt);
        }

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
