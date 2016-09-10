package com.jerry.soundcode.thread.demo;

import java.util.Random;

/**
 * ThreadLocal 现实线程范围内的数据共享
 * @author Jerry Wang
 * @Email  jerry002@126.com
 * @date   2016年9月10日
 */
public class ThreadLocalDemo {
private static ThreadLocal<Integer> threadData = new ThreadLocal<Integer>();
	
	public static void main(String[] args) {
		for(int i = 0; i < 2; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					int data = new Random().nextInt();
					System.out.println(Thread.currentThread().getName() + " has put data : " + data);
					threadData.set(data);
					MyThreadScopeData.getThreadInstance().setName("name " + data);
					MyThreadScopeData.getThreadInstance().setAge(data);
					new A().get();
					new B().get();
				}
			}).start();
		}
	}
	
	static class A {
		public void  get() {
			int data = threadData.get();
			System.out.println("A from " + Thread.currentThread().getName() + " has put data : " + data);
			MyThreadScopeData myThreadScopeData = MyThreadScopeData.getThreadInstance();
			System.out.println("A from " + Thread.currentThread().getName()
					+ " getMyData: " + myThreadScopeData.getName()  + "," 
					+ myThreadScopeData.getAge());
		}
	}
	
	static class B {
		public void  get() {
			int data = threadData.get();
			System.out.println("B from " + Thread.currentThread().getName() + " has put data : " + data);
			MyThreadScopeData myThreadScopeData = MyThreadScopeData.getThreadInstance();
			System.out.println("A from " + Thread.currentThread().getName()
					+ " getMyData: " + myThreadScopeData.getName()  + "," 
					+ myThreadScopeData.getAge());
		}
	}
}

class MyThreadScopeData{
	
	private static ThreadLocal<MyThreadScopeData> map = new ThreadLocal<MyThreadScopeData>();
	
	private MyThreadScopeData(){}
	
	public synchronized static MyThreadScopeData getThreadInstance() {
		MyThreadScopeData instance = map.get();
		if(null == instance) {
			instance = new MyThreadScopeData();
			map.set(instance);
		}
		return instance;
	}
	
	private String name;
	
	private int age;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getAge() {
		return age;
	}
	
	public void setAge(int age) {
		this.age = age;
	}
}

