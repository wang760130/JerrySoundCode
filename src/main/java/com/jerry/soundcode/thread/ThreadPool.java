package com.jerry.soundcode.thread;

import java.util.LinkedList;

/**
 * Java线程池工具类
 * 模拟线程池
 * @author Jerry Wang
 * @Email  jerry002@126.com
 * @date   2016年9月8日
 */
public class ThreadPool extends ThreadGroup {
	
	// 线程池是否关闭 
	private boolean isClosed = false;
	
	// 工作队列
	private LinkedList<java.lang.Runnable> workQueue;
	
	// 线程池的id
	private static int threadPoolID = 1;
	
	public ThreadPool(int poolSize) {
		// poolSize 表示线程池中的工作线程的数量

		// 指定ThreadGroup的名称
		super(threadPoolID + "");
		// 继承到的方法，设置是否守护线程池
		setDaemon(true);
		// 创建工作队列
		workQueue = new LinkedList<java.lang.Runnable>();
		
		// 创建并启动工作线程,线程池数量是多少就创建多少个工作线程
		for(int i = 0; i < poolSize; i++) {
			new WorkThread(i).start();
		}
	}
	
	/**
	 * 向工作队列中加入一个新任务,由工作线程去执行该任务
	 * @param task
	 */
	public synchronized void execute(java.lang.Runnable task) {
		if(isClosed) {
			throw new IllegalStateException();
		}
		
		if(task != null) {
			// 向队列中加入一个任务
			workQueue.add(task);
			// 唤醒一个正在getTask()方法中待任务的工作线程
			notify();
		}
	}
	
	private synchronized java.lang.Runnable getTask(int threadid) throws InterruptedException {
		while(workQueue.size() == 0) {
			if(isClosed) {
				return null;
			}
			// 如果工作队列中没有任务,就等待任务
			wait();
		}
		// 反回队列中第一个元素,并从队列中删除
		return (java.lang.Runnable) workQueue.removeFirst();
	}

	/**
	 * 关闭线程池
	 */
	public synchronized void closePool() {
		if(!isClosed) {
			// 等待工作线程执行完毕
			waitFinish();
			isClosed = true;
			// 清空工作队列
			workQueue.clear();
			// 中断线程池中的所有的工作线程,此方法继承自ThreadGroup类
			interrupt();
		}
	}
	
	/**
	 * 等待工作线程把所有任务执行完毕
	 */
	private void waitFinish() {
		synchronized (this) {
			isClosed = true;
			// 唤醒所有还在getTask()方法中等待任务的工作线程
			notifyAll();
		}
		
		// activeCount() 返回该线程组中活动线程的估计值。
		java.lang.Thread[] threads = new java.lang.Thread[activeCount()];
		// enumerate()方法继承自ThreadGroup类，根据活动线程的估计值获得线程组中当前所有活动的工作线程
		int count = enumerate(threads);
		for(int i = 0; i < count; i++) {
			// 等待所有工作线程结束
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}

	/**
	 * 内部类,工作线程,负责从工作队列中取出任务,并执行
	 * @author Jerry Wang
	 * @Email  jerry002@126.com
	 * @date   2016年9月8日
	 */
	private class WorkThread extends java.lang.Thread {
		private int id;
		
		public WorkThread(int id) {
			// 父类构造方法,将线程加入到当前ThreadPool线程组中
			super(ThreadPool.this, id + "");
			this.id = id;
		}

		@Override
		public void run() {
			while(!isInterrupted()) {
				java.lang.Runnable task = null;
				try {
					task = getTask(id);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
				
				// 如果getTask()返回null或者线程执行getTask()时被中断，则结束此线程
				if(task == null) {
					return ;
				}
				
				try {
					// 运行任务
					task.run();
				} catch(Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
	
	private static java.lang.Runnable createTask(final int taskID) {
		return new java.lang.Runnable() {
			public void run() {
				System.out.println("Task" + taskID + "开始");
				System.out.println("Task" + taskID + "结束");
			}
		};
	}
	
	public static void main(String[] args) throws InterruptedException {
		// 创建一个有个3工作线程的线程池
		ThreadPool threadPool = new ThreadPool(3); 
		// 休眠500毫秒,以便让线程池中的工作线程全部运行
		java.lang.Thread.sleep(500); 
		// 运行任务
		for (int i = 0; i <=5 ; i++) { 
			// 创建6个任务
			threadPool.execute(createTask(i));
		}
		// 等待所有任务执行完毕
		threadPool.waitFinish(); 
		// 关闭线程池
		threadPool.closePool(); 
	}
}
