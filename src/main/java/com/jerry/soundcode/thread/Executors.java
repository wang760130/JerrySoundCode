package com.jerry.soundcode.thread;


public class Executors {

	public Executors() {
		// TODO Auto-generated constructor stub
	}
	
	public static <T> Callable<T> callable(Runnable task, T result) {
		return null;
	}
	
	public static ThreadFactory defaultThreadFactory() {
		return new DefaultThreadFactory();
	}
	
	static class DefaultThreadFactory implements ThreadFactory {
		
		@Override
		public Thread newThread(Runnable r) {
			return null;
		}
		
	}
}
