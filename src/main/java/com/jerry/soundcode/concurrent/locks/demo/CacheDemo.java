package com.jerry.soundcode.concurrent.locks.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 模拟一个缓存器
 * @author Jerry Wang
 * @Email  jerry002@126.com
 * @date   2016年9月10日
 */
public class CacheDemo {

	private Map<String, Object> cache = new HashMap<String, Object>();

	/**
	 * 使用 synchronized 进行互斥
	 * @param key
	 * @return
	 */
	public synchronized Object getData(String key) {
		Object value = cache.get(key);
		if(value == null) {
			// 实际是从数据库中取
			value = "xxxxxx"; 
		}
		return value;
	}
	
	/**
	 * 使用读写锁
	 * 
	 * @param key
	 * @return
	 */
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	public Object get(String id) {
		Object value = null;
		// 首先开启读锁，从缓存中去取
		lock.readLock().lock();
		try {
			value = cache.get(id);
			 // 如果缓存中没有释放读锁，上写锁
			if (value == null) {
				lock.readLock().unlock();
				lock.writeLock().lock();
				try {
					if (value == null) {
						// 此时可以去数据库中查找，这里简单的模拟一下
						value = "aaa"; 
					}
				} finally {
					// 释放写锁
					lock.writeLock().unlock(); 
				}
				// 然后再上读锁
				lock.readLock().lock(); 
			}
		} finally {
			// 最后释放读锁
			lock.readLock().unlock(); 
		}
		return value;
	}
}
