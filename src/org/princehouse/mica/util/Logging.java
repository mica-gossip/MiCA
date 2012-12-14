package org.princehouse.mica.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Contains subclasses for custom log events
 * 
 * Note: The "current" preferences for logging are stored in
 * LogFlag.currentLogMask
 * 
 * @author lonnie
 * 
 */
public class Logging {

	public static class SelectEvent {
		public Address selected = null;
		transient public Distribution<Address> view = null;
	};

	public static class StateEvent {
		public Object state;
		public Distribution<Address> view;
		public String stateType;
	};

	private static GsonBuilder gsonBuilder = null;
	private static Gson gson = null;

	public static synchronized Gson getGson() {
		if (gson == null) {
			gson = getGsonBuilder().create();
		}
		return gson;
	}

	public static GsonBuilder getGsonBuilder() {
		if (gsonBuilder == null) {
			gsonBuilder = new GsonBuilder();
			initGsonBuilder();
		}
		return gsonBuilder;
	}

	/**
	 * Any custom support for json-serialization of various classes should
	 * happen here
	 */
	private static void initGsonBuilder() {

		// Custom serialization for addresses
		JsonSerializer<Address> addressSerializer = new JsonSerializer<Address>() {
			@Override
			public JsonElement serialize(Address src, Type typeOfSrc,
					JsonSerializationContext context) {
				return new JsonPrimitive(src.toString());
			}
		};
		getGsonBuilder().registerTypeHierarchyAdapter(Address.class,
				addressSerializer);

		JsonSerializer<Throwable> throwableSerializer = new JsonSerializer<Throwable>() {
			@Override
			public JsonElement serialize(Throwable throwable, Type typeOfSrc,
					JsonSerializationContext context) {
				String stacktrace = Exceptions.stackTraceToString(throwable);
				return new JsonPrimitive(stacktrace);
			}
		};
		getGsonBuilder().registerTypeHierarchyAdapter(Throwable.class,
				throwableSerializer);

		getGsonBuilder().registerTypeAdapterFactory(new TypeAdapterFactory() {

			private HashMap<Class<?>, TypeAdapter<?>> typeAdapterCache = new HashMap<Class<?>, TypeAdapter<?>>();
			private HashMap<TypeToken<?>, TypeAdapter<?>> originalAdapterCache = new HashMap<TypeToken<?>, TypeAdapter<?>>();

			@SuppressWarnings("unchecked")
			@Override
			public <T> TypeAdapter<T> create(final Gson gson,
					final TypeToken<T> type) {
				Class<?> rawType = type.getRawType();
				try {
					rawType.asSubclass(Protocol.class);
				} catch (ClassCastException e) {
					return null; // not a subclass of protocol
				}

				synchronized (typeAdapterCache) {
					if (typeAdapterCache.containsKey(rawType)) {
						return (TypeAdapter<T>) typeAdapterCache.get(rawType);
					}
				}

				final TypeAdapterFactory factory = this;

				TypeAdapter<T> ta = new TypeAdapter<T>() {

					@Override
					public T read(JsonReader arg0) throws IOException {
						throw new RuntimeException(); // not implemented
					}

					@Override
					public void write(JsonWriter writer, T src)
							throws IOException {
						JsonObject obj = new JsonObject();
						obj.addProperty("stateType", src.getClass()
								.getSimpleName());

						TypeAdapter<T> originalProtAdapter = null;
						synchronized (originalAdapterCache) {
							if (originalAdapterCache.containsKey(type)) {
								originalProtAdapter = (TypeAdapter<T>) originalAdapterCache
										.get(type);
							} else {
								originalProtAdapter = gson.getDelegateAdapter(
										factory, type);
								originalAdapterCache.put(type,
										originalProtAdapter);
							}
						}
						obj.add("state", originalProtAdapter.toJsonTree(src));

						if (LogFlag.view.test()) {
							obj.add("view",
									gson.toJsonTree(((Protocol) src).getView()));
						}
						gson.toJson(obj, writer);
					}

				};

				synchronized (typeAdapterCache) {
					typeAdapterCache.put(rawType, ta);
				}
				return ta;
			}
		});

	}

}
