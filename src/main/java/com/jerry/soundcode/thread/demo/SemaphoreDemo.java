package com.jerry.soundcode.thread.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Semaphore同步工具
 * @author Jerry Wang
 * @Email  jerry002@126.com
 * @date   2016年9月10日
 */
public class SemaphoreDemo {

	public static void main(String[] args) {
		
		final int count = 3;
		ExecutorService service =  Executors.newCachedThreadPool();
		final Semaphore semaphore = new Semaphore(count);
		
		for(int i = 0; i < 10; i++) {
			Runnable runable = new Runnable() {

				@Override
				public void run() {
					try {
						semaphore.acquire();
						System.out.println("线程" + Thread.currentThread().getName() + "进入，" + "当前已有" + (count - semaphore.availablePermits() + "个并发"));
						
						Thread.sleep(1000);
						System.out.println("线程" + Thread.currentThread().getName() + "即将离开");
						
						semaphore.release();
						System.out.println("线程" + Thread.currentThread().getName() + "进入，" +	"当前已有" + (count - semaphore.availablePermits() + "个并发"));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			service.execute(runable);
		}
	}

}
