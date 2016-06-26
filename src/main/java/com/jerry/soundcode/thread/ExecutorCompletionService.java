package com.jerry.soundcode.thread;

import java.util.concurrent.*;

public class ExecutorCompletionService<V> implements CompletionService<V> {
	
	private final Executor executor;
	private final AbstractExecutorService aes;
	private final BlockingQueue<Future<V>> competionQueue;
//	
	private class QueueingFuture extends FutureTask<Void> {
		public QueueingFuture(RunnableFuture<V> task) {
			super(task, null);
			this.task = task;
		}
		protected void done() {
			competionQueue.add(task);
		}
		private final Future<V> task;
	} 
	
	private RunnableFuture<V> newTaskFor(Callable<V> task) {
		if(aes == null) {
			return new FutureTask<V> ((java.util.concurrent.Callable<V>) task); 
		} /*else {
			return aes.newTaskFor((java.util.concurrent.Callable<T>) task);
		}*/
		return null;
	}
	
	private RunnableFuture<V> newTaskFor(Runnable task, V result) {
		if(aes == null) {
			return new FutureTask<V>((java.lang.Runnable) task, result);
		} /*else {
			return aes.newTaskFor(task, result);
		}*/
		return null;
	}
	
	public ExecutorCompletionService(Executor executor) {
		if(executor == null) {
			throw new NullPointerException();
		}
		this.executor = executor;
		this.aes = (executor instanceof AbstractExecutorService) ? (AbstractExecutorService) executor : null;
		this.competionQueue = new LinkedBlockingQueue<Future<V>>();
	}
	
	public ExecutorCompletionService(Executor exrcutor, BlockingQueue<Future<V>> completionQueue) {
		if(exrcutor == null || completionQueue == null) {
			throw new NullPointerException();
		}
		
		this.executor = exrcutor;
		this.aes = (executor instanceof AbstractExecutorService) ? (AbstractExecutorService) executor : null;
		this.competionQueue = new LinkedBlockingQueue<Future<V>>();
	}
	
	@Override
	public Future<V> submit(Callable<V> task) {
		if(task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<V> f = newTaskFor(task);
		executor.execute((Runnable) new QueueingFuture(f));
		return f;
	}

	@Override
	public Future<V> submit(Runnable task, V result) {
		if(task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<V> f = newTaskFor(task, result);
		executor.execute((Runnable) new QueueingFuture(f));
		return f;
	}

	@Override
	public Future<V> task() throws InterruptedException {
		return competionQueue.take();
	}

	@Override
	public Future<V> poll() {
		return competionQueue.poll();
	}

	@Override
	public Future<V> poll(long timeout, TimeUnit unit)
			throws InterruptedException {
		return competionQueue.poll(timeout, unit);
	} 

}
