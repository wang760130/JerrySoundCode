package com.jerry.soundcode.thread.demo;

import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Exchanger可以在两个线程之间交换数据，只能是2个线程，他不支持更多的线程之间互换数据。
 * 当线程A调用Exchange对象的exchange()方法后，他会陷入阻塞状态，直到线程B也调用了exchange()方法，
 * 然后以线程安全的方式交换数据，之后线程A和B继续运行
 * @author Jerry Wang
 * @Email  jerry002@126.com
 * @date   2016年9月10日
 */
public class ExchangerDemo {

	public static void main(String[] args) {
		ExecutorService service = Executors.newCachedThreadPool();
		final Exchanger<String> exchanger = new Exchanger<String>();
		
		service.execute(new Runnable(){
			public void run() {
				try {				
					String data1 = "aaa";
					System.out.println("线程" + Thread.currentThread().getName() + "正在把数据" + data1 +"换出去");
					Thread.sleep((long)(Math.random() * 10000));
					String data2 = exchanger.exchange(data1);
					System.out.println("线程" + Thread.currentThread().getName() + "换回的数据为" + data2);
				} catch(Exception e){
					e.printStackTrace();
				}
			}	
		});
		
		service.execute(new Runnable(){
			public void run() {
				try {				
					String data1 = "bbb";
					System.out.println("线程" + Thread.currentThread().getName() + "正在把数据" + data1 +"换出去");
					Thread.sleep((long)(Math.random()*10000));					
					String data2 = exchanger.exchange(data1);
					System.out.println("线程" + Thread.currentThread().getName() + "换回的数据为" + data2);
				} catch(Exception e){
					e.printStackTrace();
				}				
			}	
		});	
	}
}
