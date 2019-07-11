package com.tvd12.ezyfox.binding.impl;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.tvd12.ezyfox.binding.EzyReader;
import com.tvd12.ezyfox.binding.EzyUnmarshaller;
import com.tvd12.ezyfox.binding.EzyUnwrapper;
import com.tvd12.ezyfox.binding.reader.EzyByteReader;
import com.tvd12.ezyfox.binding.reader.EzyCharacterReader;
import com.tvd12.ezyfox.binding.reader.EzyConcurrentHashMapReader;
import com.tvd12.ezyfox.binding.reader.EzyDefaultReader;
import com.tvd12.ezyfox.binding.reader.EzyDoubleReader;
import com.tvd12.ezyfox.binding.reader.EzyFloatReader;
import com.tvd12.ezyfox.binding.reader.EzyIntegerReader;
import com.tvd12.ezyfox.binding.reader.EzyListReader;
import com.tvd12.ezyfox.binding.reader.EzyLongReader;
import com.tvd12.ezyfox.binding.reader.EzyMapReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveBooleanArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveByteArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveCharArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveDoubleArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveFloatArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveIntArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveLongArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyPrimitiveShortArrayReader;
import com.tvd12.ezyfox.binding.reader.EzySetReader;
import com.tvd12.ezyfox.binding.reader.EzyShortReader;
import com.tvd12.ezyfox.binding.reader.EzyStringArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyTreeMapReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperBooleanArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperByteArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperCharacterArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperDoubleArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperFloatArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperIntegerArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperLongArrayReader;
import com.tvd12.ezyfox.binding.reader.EzyWrapperShortArrayReader;
import com.tvd12.ezyfox.entity.EzyArray;
import com.tvd12.ezyfox.entity.EzyObject;
import com.tvd12.ezyfox.io.EzyMaps;
import com.tvd12.ezyfox.reflect.EzyTypes;
import com.tvd12.ezyfox.util.EzyCollectionFactory;
import com.tvd12.ezyfox.util.EzyEntityBuilders;
import com.tvd12.ezyfox.util.EzyMapFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class EzySimpleUnmarshaller
		extends EzyEntityBuilders
		implements EzyUnmarshaller {

	protected final Map<Class, EzyReader> readersByType;
	protected final Map<Class, EzyReader> readersByObjectType;
	protected Map<Class, EzyUnwrapper> unwrappersByObjectType;
	
	protected final EzyMapFactory mapFactory = new EzyMapFactory();
	protected final EzyCollectionFactory collectionFactory = new EzyCollectionFactory();
	
	public EzySimpleUnmarshaller() {
		this.readersByObjectType = defaultReaders();
		this.readersByType = defaultReadersByType();
		this.unwrappersByObjectType  = new ConcurrentHashMap<>();
	}
	
	public void addReader(EzyReader reader) {
		readersByType.put(reader.getClass(), reader);
		Class<?> objectType = reader.getObjectType();
		if(objectType != null)
			readersByObjectType.put(objectType, reader);
	}
	
	public void addReaders(Iterable<EzyReader> readers) {
		readers.forEach(this::addReader);
	}
	
	public void addReader(Class type, EzyReader reader) {
		readersByObjectType.put(type, reader);
		readersByType.put(reader.getClass(), reader);
	}
	
	public void addReaders(Map<Class, EzyReader> readers) {
		readers.keySet().forEach(key -> addReader(key, readers.get(key)));
	}
	
	public void addUnwrapper(Class type, EzyUnwrapper unwrapper) {
		unwrappersByObjectType.put(type, unwrapper);
	}
	
	public void addUnwrappers(Map<Class, EzyUnwrapper> unwrappers) {
		unwrappers.keySet().forEach(key -> addUnwrapper(key, unwrappers.get(key)));
	}
	
	@Override
	public boolean containsUnwrapper(Class objectType) {
		boolean answer = unwrappersByObjectType.containsKey(objectType);
		return answer;
	}
	
	@Override
	public void unwrap(Object value, Object output) {
		EzyUnwrapper unwrapper = unwrappersByObjectType.get(output.getClass());
		if(unwrapper != null)
			unwrapper.unwrap(this, value, output);;
	}
	
	@Override
	public <T> T unmarshal(Object value, Class<T> outType) {
		if(value == null)
			return null;
		EzyReader reader = EzyMaps.getValue(readersByObjectType, outType);
		if(reader != null)
			return (T) reader.read(this, value);
		if(outType.isArray())
			return (T) readArray(value, outType.getComponentType());
		if(outType.isEnum())
			return (T) Enum.valueOf((Class<Enum>)outType, value.toString());
		throw new IllegalArgumentException("has no reader for " + outType);
	}
	
	@Override
	public <K, V> Map<K, V> unmarshalMap(
			Object value, Class mapType, Class<K> keyType, Class<V> valueType) {
		Map map = mapFactory.newMap(mapType);
		EzyObject object = (EzyObject)value;
		for(Object key : object.keySet())
			map.put(unmarshal(key, keyType), unmarshal((Object)object.get(key), valueType));
		return map;
	}
	
	@Override
	public <T> Collection<T> unmarshalCollection(
			Object value, Class collectionType, Class<T> itemType) {
		if(value instanceof Collection)
			return unmarshalCollection(((Collection)value).iterator(), collectionType, itemType);
		return unmarshalCollection(((EzyArray)value).iterator(), collectionType, itemType);
	}
	
	private <T> Collection<T> unmarshalCollection(
			Iterator iterator, Class collectionType, Class<T> itemType) {
		Collection<T> collection = collectionFactory.newCollection(collectionType);
		while(iterator.hasNext())
			collection.add(unmarshal(iterator.next(), itemType));
		return collection;
	}
	
	@Override
	public <T> T unmarshal(Class<? extends EzyReader> readerClass, Object value) {
		if(value == null)
			return null;
		if(readersByType.containsKey(readerClass))
			return (T) readersByType.get(readerClass).read(this, value);
		throw new IllegalArgumentException("can't unmarshal value " + 
			value + ", " + readerClass.getName() + " is not reader class");
	}
	
	private Map<Class, EzyReader> defaultReadersByType() {
		Map<Class, EzyReader> map = new ConcurrentHashMap<>();
		readersByObjectType.values().forEach(w -> map.put(w.getClass(), w));
		return map;
	}
	
	private Map<Class, EzyReader> defaultReaders() {
		Map<Class, EzyReader> map = new ConcurrentHashMap<>();
		Set<Class> normalTypes = EzyTypes.NON_ARRAY_TYPES;
		for(Class normalType : normalTypes)
			map.put(normalType, EzyDefaultReader.getInstance());
		map.put(byte.class, EzyByteReader.getInstance());
		map.put(char.class, EzyCharacterReader.getInstance());
		map.put(double.class, EzyDoubleReader.getInstance());
		map.put(float.class, EzyFloatReader.getInstance());
		map.put(int.class, EzyIntegerReader.getInstance());
		map.put(long.class, EzyLongReader.getInstance());
		map.put(short.class, EzyShortReader.getInstance());
		map.put(Byte.class, EzyByteReader.getInstance());
		map.put(Character.class, EzyCharacterReader.getInstance());
		map.put(Double.class, EzyDoubleReader.getInstance());
		map.put(Float.class, EzyFloatReader.getInstance());
		map.put(Integer.class, EzyIntegerReader.getInstance());
		map.put(Long.class, EzyLongReader.getInstance());
		map.put(Short.class, EzyShortReader.getInstance());
		
		map.put(boolean[].class, EzyPrimitiveBooleanArrayReader.getInstance());
		map.put(byte[].class, EzyPrimitiveByteArrayReader.getInstance());
		map.put(char[].class, EzyPrimitiveCharArrayReader.getInstance());
		map.put(double[].class, EzyPrimitiveDoubleArrayReader.getInstance());
		map.put(float[].class, EzyPrimitiveFloatArrayReader.getInstance());
		map.put(int[].class, EzyPrimitiveIntArrayReader.getInstance());
		map.put(long[].class, EzyPrimitiveLongArrayReader.getInstance());
		map.put(short[].class, EzyPrimitiveShortArrayReader.getInstance());
		map.put(Boolean[].class, EzyWrapperBooleanArrayReader.getInstance());
		map.put(Byte[].class, EzyWrapperByteArrayReader.getInstance());
		map.put(Character[].class, EzyWrapperCharacterArrayReader.getInstance());
		map.put(Double[].class, EzyWrapperDoubleArrayReader.getInstance());
		map.put(Float[].class, EzyWrapperFloatArrayReader.getInstance());
		map.put(Integer[].class, EzyWrapperIntegerArrayReader.getInstance());
		map.put(Long[].class, EzyWrapperLongArrayReader.getInstance());
		map.put(Short[].class, EzyWrapperShortArrayReader.getInstance());
		map.put(String[].class, EzyStringArrayReader.getInstance());
		
		map.put(Date.class, EzyDefaultReader.getInstance());
		map.put(Class.class, EzyDefaultReader.getInstance());
		map.put(LocalDate.class, EzyDefaultReader.getInstance());
		map.put(LocalDateTime.class, EzyDefaultReader.getInstance());
		map.put(EzyArray.class, EzyDefaultReader.getInstance());
		map.put(EzyObject.class, EzyDefaultReader.getInstance());
		map.put(Map.class, EzyMapReader.getInstance());
		map.put(HashMap.class, EzyMapReader.getInstance());
		map.put(TreeMap.class, EzyTreeMapReader.getInstance());
		map.put(ConcurrentHashMap.class, EzyConcurrentHashMapReader.getInstance());
		map.put(List.class, EzyListReader.getInstance());
		map.put(Set.class, EzySetReader.getInstance());
		map.put(ArrayList.class, EzyListReader.getInstance());
		map.put(HashSet.class, EzySetReader.getInstance());
		map.put(Collection.class, EzyListReader.getInstance());
		return map;
	}
	
	private Object readArray(Object array, Class componentType) {
		if(array instanceof EzyArray)
			return readArrayByArray((EzyArray) array, componentType);
		else if(array instanceof Collection)
			return readArrayByCollection((Collection) array, componentType);
		return array;
	}
	
	private Object readArrayByArray(EzyArray array, Class componentType) {
		Object[] answer = (Object[]) Array.newInstance(componentType, array.size());
		for(int i = 0 ; i < array.size() ; ++i) 
			answer[i] = unmarshal((Object)array.get(i), componentType);
		return answer;
	}
	
	private Object readArrayByCollection(Collection collection, Class componentType) {
		Object[] answer = (Object[]) Array.newInstance(componentType, collection.size());
		int index = 0;
		for(Object item : collection) 
			answer[index ++] = unmarshal(item, componentType);
		return answer;
	}

}
