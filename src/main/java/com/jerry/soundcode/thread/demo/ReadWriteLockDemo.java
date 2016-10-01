package com.jerry.soundcode.thread.demo;

import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Jerry Wang
 * @Email  jerry002@126.com
 * @date   2016年10月1日
 */
public class ReadWriteLockDemo {

	public static void main(String[] args) {
		final Queue queue = new Queue();
		for (int i = 0; i < 3; i++) {
			new Thread() {
				@Override
				public void run() {
					while(true) {
						queue.get();
					}
				}
			}.start();
			
			new Thread() {
				@Override
				public void run() {
					while(true) {
						queue.put(new Random().nextInt(10000));
					}
				}
			}.start();
		}
	}
}

class Queue {
	private Object data = null;
	ReadWriteLock lock = new ReentrantReadWriteLock();
	public void get() {
		lock.readLock().lock();
		try {
			System.out.println(Thread.currentThread().getName() + " be ready to read data :" + data);
		
			Thread.sleep((long) (Math.random() * 1000));
			
			System.out.println(Thread.currentThread().getName() + " have read data :  " + data);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.readLock().unlock();
		}
		
	}
	
	public void put(Object data) {
		lock.writeLock().lock();
		try {
			System.out.println(Thread.currentThread().getName() + " be ready to write data :" + data);
			
			Thread.sleep((long)(Math.random() * 1000));
		
			System.out.println(Thread.currentThread().getName()  + " have write data :  " + data);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.writeLock().unlock();
		}
	}
}