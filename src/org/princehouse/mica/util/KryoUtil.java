package org.princehouse.mica.util;

import java.util.EnumMap;
import java.util.EnumSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

import de.javakaffee.kryoserializers.EnumMapSerializer;
import de.javakaffee.kryoserializers.EnumSetSerializer;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;

public class KryoUtil {
    public static Kryo defaultKryo() {
        return new Kryo();
        // return reflectionKryo();
    }

    public static Kryo reflectionKryo() {
        return new KryoReflectionFactorySupport() {

            @Override
            public Serializer<?> getDefaultSerializer(@SuppressWarnings("rawtypes") final Class clazz) {
                if (EnumSet.class.isAssignableFrom(clazz)) {
                    return new EnumSetSerializer();
                }
                if (EnumMap.class.isAssignableFrom(clazz)) {
                    return new EnumMapSerializer();
                }
                /*
                 * if ( SubListSerializers.canSerialize( clazz ) ) { return
                 * SubListSerializers.createFor( clazz ); } if (
                 * copyCollectionsForSerialization ) { if (
                 * Collection.class.isAssignableFrom( clazz ) ) { return new
                 * CopyForIterateCollectionSerializer(); } if (
                 * Map.class.isAssignableFrom( clazz ) ) { return new
                 * CopyForIterateMapSerializer(); } } if (
                 * Date.class.isAssignableFrom( type ) ) { return new
                 * DateSerializer( type ); } // see if the given class is a
                 * cglib proxy if ( CGLibProxySerializer.canSerialize( type ) )
                 * { // return the serializer registered for
                 * CGLibProxyMarker.class (see above) return getSerializer(
                 * CGLibProxySerializer.CGLibProxyMarker.class ); }
                 */
                return super.getDefaultSerializer(clazz);
            }

        };

    }
}
