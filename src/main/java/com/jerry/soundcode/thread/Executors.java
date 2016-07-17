package com.jerry.soundcode.thread;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jerry.soundcode.concurrent.collection.LinkedBlockingQueue;
import com.jerry.soundcode.concurrent.collection.SynchronousQueue;
import com.jerry.soundcode.list.Collection;
import com.jerry.soundcode.list.List;


public class Executors {

	public static ExecutorService newFixedThreadPool(int nThreads) {
		return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public static ExecutorService newFixedThreadPool(int nThread, ThreadFactory threadFactory) {
		return new ThreadPoolExecutor(nThread, nThread, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
	}
	
	public static ExecutorService newSingleThreadExecutor() {
		return new FinalizableDelegatedExecutorServie(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()));
	}
	
	public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
		return new FinalizableDelegatedExecutorServie(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory));
	}
	
	public static ExecutorService newCachedThreadPool() {
		return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}
	
	public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
		return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory);
	}
	
	public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
		return new DelegatedScheduledExecutorService(new ScheduledThreadPoolExecutor(1));
	}
	
	public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
		return new DelegatedScheduledExecutorService(new ScheduledThreadPoolExecutor(1, threadFactory));
	}
	
	public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
		return new ScheduledThreadPoolExecutor(corePoolSize);
	}
	
	public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
		return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
	}
	
	public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
		if(executor == null) {
			throw new NullPointerException();
		}
		return new DelegatedExecutorService(executor);
	}
	
	public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService executor) {
		if(executor == null) {
			throw new NullPointerException();
		}
		return new DelegatedScheduledExecutorService(executor);
	}
	
	public static ThreadFactory defaultThreadFactory() {
		return new DefaultThreadFactory();
	}
	
	public static ThreadFactory privilegedThreadFactory() {
		return new PrivilegedThreadFactory();
	}
	
	public static <T> Callable<T> callable(Runnable task, T result) {
		if(task == null) {
			throw new NullPointerException();
		}
		return new RunnableAdapter<T>(task, result);
	}
	
	public static Callable<Object> callable(Runnable task) {
		if(task == null) {
			throw new NullPointerException();
		}
		return new RunnableAdapter<Object>(task, null);
	}
	
	public static Callable<Object> callable(final PrivilegedAction<?> action) {
		if(action == null) {
			throw new NullPointerException();
		}
		return new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				return action.run();
			}
			
		};
	}
	
	public static Callable<Object> callable(final PrivilegedExceptionAction<?> action) {
		if(action == null) {
			throw new NullPointerException();
		}
		return new Callable<Object>() {
			
			@Override
			public Object call() throws Exception {
				return action.run();
			}
		};
	}
	
	public static <T> Callable<T> privilegedCallable(Callable<T> callable) {
		if(callable == null) {
			throw new NullPointerException();
		}
		return new PrivilegedCallable<T>(callable);
	}
	
	public static <T> Callable<T> privilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
		if(callable == null) {
			throw new NullPointerException();
		}
		return new PrivilegedCallableUsingCurrentClassLoader<T>(callable);
	}
	
	static final class RunnableAdapter<T> implements Callable<T> {
		final Runnable task;
		final T result;
		
		RunnableAdapter(Runnable task, T result) {
			this.task = task;
			this.result = result;
		}
		
		@Override
		public T call() throws Exception {
			task.run();
			return result;
		}
	}
	
	static final class PrivilegedCallable<T> implements Callable<T> {

		private final AccessControlContext acc;
		private final Callable<T> task;
		private T result;
		private Exception exception;
		
		PrivilegedCallable(Callable<T> task) {
			this.task = task;
			this.acc = AccessController.getContext();
		}
		
		@Override
		public T call() throws Exception {
			AccessController.doPrivileged(new PrivilegedAction<T>() {

				@Override
				public T run() {
					try {
						result = task.call();
					} catch (Exception e) {
						exception = e;
					}
					return null;
				}
			
			}, acc);
			
			if(exception != null) {
				throw exception;
			} else {
				return result;
			}
		}
	}
	
	static final class PrivilegedCallableUsingCurrentClassLoader<T> implements Callable<T> {
		private final ClassLoader ccl;
		private final AccessControlContext acc;
		private final Callable<T> task;
		private T result;
		private Exception exception;
		
		PrivilegedCallableUsingCurrentClassLoader(Callable<T> task) {
			this.task = task;
			this.ccl = Thread.currentThread().getContextClassLoader();
			this.acc = AccessController.getContext();
			acc.checkPermission(new RuntimePermission("getContextClassLoader"));
			acc.checkPermission(new RuntimePermission("setContextClassLoader"));
		}
		
		@Override
		public T call() throws Exception {
			
			AccessController.doPrivileged(new PrivilegedAction<T>() {

				@Override
				public T run() {
					Thread t = Thread.currentThread();
					
					try {
						ClassLoader cl = t.getContextClassLoader();
						if(ccl == cl) {
							result = task.call();
						} else {
							t.setContextClassLoader(ccl);
							try {
								result = task.call();
							} finally {
								t.setContextClassLoader(cl);
							}
						}
					} catch (Exception ex) {
						exception = ex;
					}
					return null;
				}
			}, acc);
			
			if(exception != null) {
				throw exception;
			} else {
				return result;
			}
		}
	}
	
	static class PrivilegedThreadFactory extends DefaultThreadFactory {
		
		private final ClassLoader ccl;
		private final AccessControlContext acc;
		
		PrivilegedThreadFactory() {
			super();
			this.ccl = Thread.currentThread().getContextClassLoader();
			this.acc = AccessController.getContext();
			acc.checkPermission(new RuntimePermission("setContextClassLoader"));
		}
		
		public Thread newThread(final Runnable r) {
			return super.newThread(new Runnable() {

				@Override
				public void run() {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {

						@Override
						public Object run() {
							Thread.currentThread().setContextClassLoader(ccl);
							r.run();
							return null;
						}
					}, acc);
				}
				
			});
		}
	}
	
	static class DefaultThreadFactory implements ThreadFactory {
		
		@Override
		public Thread newThread(Runnable r) {
			return null;
		}
		
	}
	
	static class DelegatedExecutorService extends AbstractExecutorService {

		private final ExecutorService e;
		
		public DelegatedExecutorService(ExecutorService executor) {
			e = executor;
		}

		@Override
		public void execute(Runnable command) {
			e.execute(command);
		}
		
		@Override
		public void shutdown() {
			e.shutdown();
		}
		
		@Override
		public List<Runnable> shutdownNow() {
			return e.shutdownNow();
		}
		
		@Override
		public boolean isShutdown() {
			return e.isShutdown();
		}
		
		@Override
		public boolean isTerminated() {
			return e.isTerminated();
		}
		
		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit)
				throws InterruptedException {
			return e.awaitTermination(timeout, unit);
		}
		
		public Future<?> submit(Runnable task) {
			return e.submit(task);
		} 
		
		public <T> Future<T> submit(Callable<T> task) {
			return e.submit(task);
		}
		
		public <T> Future<T> submit(Runnable task, T result) {
			return e.submit(task, result);
		}
		
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
			return e.invokeAll(tasks);
		}
		
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
			return e.invokeAll(tasks, timeout, unit);
		}
		
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
			return e.invokeAny(tasks);
		}
		
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return e.invokeAny(tasks, timeout, unit);
		}
	}	
	
	static class FinalizableDelegatedExecutorServie extends DelegatedExecutorService {

		public FinalizableDelegatedExecutorServie(
				ExecutorService executor) {
			super(executor);
		}

		protected void finalize() {
			super.shutdown();
		}
	}
	
	static class DelegatedScheduledExecutorService extends DelegatedExecutorService implements ScheduledExecutorService {
		
		private final ScheduledExecutorService e;
		
		public DelegatedScheduledExecutorService(ScheduledExecutorService executor) {
			super(executor);
			e = executor;
		}
		
		@Override
		public ScheduledFuture<?> schedule(Runnable command, long delay,TimeUnit unit) {
			return e.schedule(command, delay, unit);
		}

		@Override
		public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
			return e.schedule(callable, delay, unit);
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,	int initialDelay, long period, TimeUnit unit) {
			return e.scheduleAtFixedRate(command, initialDelay, period, unit);
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,long initialDelay, long delay, TimeUnit unit) {
			return e.scheduleWithFixedDelay(command, initialDelay, delay, unit);
		}
	}
	
	 private Executors() {}
}
