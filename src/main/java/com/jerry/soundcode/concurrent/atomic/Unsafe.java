package com.jerry.soundcode.concurrent.atomic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import sun.reflect.Reflection;

public class Unsafe {

	private static final Unsafe theUnsafe;
	public static final int INVALID_FIELD_OFFSET = -1;

	private static native void registerNatives();

	public static Unsafe getUnsafe() {
		Class localClass = Reflection.getCallerClass(2);
		if (localClass.getClassLoader() != null)
			throw new SecurityException("Unsafe");
		return theUnsafe;
	}

	public native int getInt(Object paramObject, long paramLong);

	public native void putInt(Object paramObject, long paramLong, int paramInt);

	public native Object getObject(Object paramObject, long paramLong);

	public native void putObject(Object paramObject1, long paramLong, Object paramObject2);

	public native boolean getBoolean(Object paramObject, long paramLong);

	public native void putBoolean(Object paramObject, long paramLong, boolean paramBoolean);

	public native byte getByte(Object paramObject, long paramLong);

	public native void putByte(Object paramObject, long paramLong, byte paramByte);

	public native short getShort(Object paramObject, long paramLong);

	public native void putShort(Object paramObject, long paramLong,	short paramShort);

	public native char getChar(Object paramObject, long paramLong);

	public native void putChar(Object paramObject, long paramLong, char paramChar);

	public native long getLong(Object paramObject, long paramLong);

	public native void putLong(Object paramObject, long paramLong1,	long paramLong2);

	public native float getFloat(Object paramObject, long paramLong);

	public native void putFloat(Object paramObject, long paramLong, float paramFloat);

	public native double getDouble(Object paramObject, long paramLong);

	public native void putDouble(Object paramObject, long paramLong, double paramDouble);

	@Deprecated
	public int getInt(Object paramObject, int paramInt) {
		return getInt(paramObject, paramInt);
	}

	@Deprecated
	public void putInt(Object paramObject, int paramInt1, int paramInt2) {
		putInt(paramObject, paramInt1, paramInt2);
	}

	@Deprecated
	public Object getObject(Object paramObject, int paramInt) {
		return getObject(paramObject, paramInt);
	}

	@Deprecated
	public void putObject(Object paramObject1, int paramInt, Object paramObject2) {
		putObject(paramObject1, paramInt, paramObject2);
	}

	@Deprecated
	public boolean getBoolean(Object paramObject, int paramInt) {
		return getBoolean(paramObject, paramInt);
	}

	@Deprecated
	public void putBoolean(Object paramObject, int paramInt, boolean paramBoolean) {
		putBoolean(paramObject, paramInt, paramBoolean);
	}

	@Deprecated
	public byte getByte(Object paramObject, int paramInt) {
		return getByte(paramObject, paramInt);
	}

	@Deprecated
	public void putByte(Object paramObject, int paramInt, byte paramByte) {
		putByte(paramObject, paramInt, paramByte);
	}

	@Deprecated
	public short getShort(Object paramObject, int paramInt) {
		return getShort(paramObject, paramInt);
	}

	@Deprecated
	public void putShort(Object paramObject, int paramInt, short paramShort) {
		putShort(paramObject, paramInt, paramShort);
	}

	@Deprecated
	public char getChar(Object paramObject, int paramInt) {
		return getChar(paramObject, paramInt);
	}

	@Deprecated
	public void putChar(Object paramObject, int paramInt, char paramChar) {
		putChar(paramObject, paramInt, paramChar);
	}

	@Deprecated
	public long getLong(Object paramObject, int paramInt) {
		return getLong(paramObject, paramInt);
	}

	@Deprecated
	public void putLong(Object paramObject, int paramInt, long paramLong) {
		putLong(paramObject, paramInt, paramLong);
	}

	@Deprecated
	public float getFloat(Object paramObject, int paramInt) {
		return getFloat(paramObject, paramInt);
	}

	@Deprecated
	public void putFloat(Object paramObject, int paramInt, float paramFloat) {
		putFloat(paramObject, paramInt, paramFloat);
	}

	@Deprecated
	public double getDouble(Object paramObject, int paramInt) {
		return getDouble(paramObject, paramInt);
	}

	@Deprecated
	public void putDouble(Object paramObject, int paramInt, double paramDouble) {
		putDouble(paramObject, paramInt, paramDouble);
	}

	public native byte getByte(long paramLong);

	public native void putByte(long paramLong, byte paramByte);

	public native short getShort(long paramLong);

	public native void putShort(long paramLong, short paramShort);

	public native char getChar(long paramLong);

	public native void putChar(long paramLong, char paramChar);

	public native int getInt(long paramLong);

	public native void putInt(long paramLong, int paramInt);

	public native long getLong(long paramLong);

	public native void putLong(long paramLong1, long paramLong2);

	public native float getFloat(long paramLong);

	public native void putFloat(long paramLong, float paramFloat);

	public native double getDouble(long paramLong);

	public native void putDouble(long paramLong, double paramDouble);

	public native long getAddress(long paramLong);

	public native void putAddress(long paramLong1, long paramLong2);

	public native long allocateMemory(long paramLong);

	public native long reallocateMemory(long paramLong1, long paramLong2);

	public native void setMemory(long paramLong1, long paramLong2, byte paramByte);

	public native void copyMemory(Object paramObject1, long paramLong1, Object paramObject2, long paramLong2, long paramLong3);

	public void copyMemory(long paramLong1, long paramLong2, long paramLong3) {
		copyMemory(null, paramLong1, null, paramLong2, paramLong3);
	}

	public native void freeMemory(long paramLong);

	@Deprecated
	public int fieldOffset(Field paramField) {
		if (Modifier.isStatic(paramField.getModifiers())) {
			return (int) staticFieldOffset(paramField);
		}
		return (int) objectFieldOffset(paramField);
	}

	@Deprecated
	public Object staticFieldBase(Class paramClass) {
		Field[] arrayOfField = paramClass.getDeclaredFields();
		for (int i = 0; i < arrayOfField.length; ++i) {
			if (Modifier.isStatic(arrayOfField[i].getModifiers())) {
				return staticFieldBase(arrayOfField[i]);
			}
		}
		return null;
	}

	public native long staticFieldOffset(Field paramField);

	public native long objectFieldOffset(Field paramField);

	public native Object staticFieldBase(Field paramField);

	public native void ensureClassInitialized(Class paramClass);

	public native int arrayBaseOffset(Class paramClass);

	public native int arrayIndexScale(Class paramClass);

	public native int addressSize();

	public native int pageSize();

	public native Class defineClass(String paramString, byte[] paramArrayOfByte, int paramInt1, int paramInt2, ClassLoader paramClassLoader, ProtectionDomain paramProtectionDomain);

	public native Class defineClass(String paramString, byte[] paramArrayOfByte, int paramInt1, int paramInt2);

	public native Object allocateInstance(Class paramClass) throws InstantiationException;

	public native void monitorEnter(Object paramObject);

	public native void monitorExit(Object paramObject);

	public native boolean tryMonitorEnter(Object paramObject);

	public native void throwException(Throwable paramThrowable);

	public final native boolean compareAndSwapObject(Object paramObject1, long paramLong, Object paramObject2, Object paramObject3);

	public final native boolean compareAndSwapInt(Object paramObject, long paramLong, int paramInt1, int paramInt2);

	public final native boolean compareAndSwapLong(Object paramObject, long paramLong1, long paramLong2, long paramLong3);

	public native Object getObjectVolatile(Object paramObject, long paramLong);

	public native void putObjectVolatile(Object paramObject1, long paramLong, Object paramObject2);

	public native int getIntVolatile(Object paramObject, long paramLong);

	public native void putIntVolatile(Object paramObject, long paramLong, int paramInt);

	public native boolean getBooleanVolatile(Object paramObject, long paramLong);

	public native void putBooleanVolatile(Object paramObject, long paramLong, boolean paramBoolean);

	public native byte getByteVolatile(Object paramObject, long paramLong);

	public native void putByteVolatile(Object paramObject, long paramLong, byte paramByte);

	public native short getShortVolatile(Object paramObject, long paramLong);

	public native void putShortVolatile(Object paramObject, long paramLong, short paramShort);

	public native char getCharVolatile(Object paramObject, long paramLong);

	public native void putCharVolatile(Object paramObject, long paramLong, char paramChar);

	public native long getLongVolatile(Object paramObject, long paramLong);

	public native void putLongVolatile(Object paramObject, long paramLong1, long paramLong2);

	public native float getFloatVolatile(Object paramObject, long paramLong);

	public native void putFloatVolatile(Object paramObject, long paramLong, float paramFloat);

	public native double getDoubleVolatile(Object paramObject, long paramLong);

	public native void putDoubleVolatile(Object paramObject, long paramLong, double paramDouble);

	public native void putOrderedObject(Object paramObject1, long paramLong, Object paramObject2);

	public native void putOrderedInt(Object paramObject, long paramLong, int paramInt);

	public native void putOrderedLong(Object paramObject, long paramLong1, long paramLong2);

	public native void unpark(Object paramObject);

	public native void park(boolean paramBoolean, long paramLong);

	public native int getLoadAverage(double[] paramArrayOfDouble, int paramInt);

	static {
		registerNatives();

		theUnsafe = new Unsafe();
	}
}