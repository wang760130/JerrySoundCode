package com.jerry.soundcode.thread;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jerry.soundcode.concurrent.locks.AbstractQueuedSynchronizer;

public class FutureTask<V> implements RunnableFuture<V> {
	
	private final Sync sync;
	
	public FutureTask(Callable<V> callable) {
		if(callable == null) {
			throw new NullPointerException();
		}
		sync = new Sync(callable);
	}
	
	public FutureTask(Runnable runnable, V result) {
		sync = new Sync(Executors.callable(runnable, result));
	}
	
	@Override
	public boolean isCancelled() {
		return sync.innerIsCancelled();
	}
	
	@Override
	public boolean isDone() {
		return sync.innerIsDoine();
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return sync.innerCancel(mayInterruptIfRunning);
	}


	@Override
	public V get() throws InterruptedException, ExecutionException {
		return sync.innerGet();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return sync.innerGet(unit.toNanos(timeout));
	}
	
	protected void done() {
		
	}
	
	protected void set(V v) {
		sync.innerSet(v);
	} 

	protected void setException(Exception e) {
		sync.innerSetException(e);
	}
	
	@Override
	public void run() {
		sync.innerRun();
	}


	private final class Sync extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 1L;
		
		private static final int RUNNING = 1;
		private static final int RAN = 2;
		private static final int CANCELLED = 4;
		
		private final Callable<V> callable;
		private V result;
		private Exception exception;
		private volatile Thread runner;
		
		Sync(Callable<V> callable) {
			this.callable = callable;
		}
		
		private boolean ranOrCanelled(int state) {
			return (state & (RAN | CANCELLED)) != 0;
		}
		
		protected int tryAcquireShared(int ignore) {
			return innerIsDoine() ? 1 : -1;
		}
		
		protected boolean tryReleaseShared(int ignore) {
			runner = null;
			return true;
		}
		
		boolean innerIsCancelled() {
			return getState() == CANCELLED;
		}

		private boolean innerIsDoine() {
			return ranOrCanelled(getState()) && runner == null;
		}
		
		V innerGet() throws InterruptedException, ExecutionException {
			acquireSharedInterruptibly(0);
			if(getState() == CANCELLED) {
				throw new CancellationException();
			}
			
			if(exception != null) {
//				throw new ExecutionException(exception);
			}
			return result;
		}
		
		V innerGet(long nanosTimeout) throws InterruptedException, ExecutionException, TimeoutException {
            if (!tryAcquireSharedNanos(0, nanosTimeout))
                throw new TimeoutException();
            if (getState() == CANCELLED)
                throw new CancellationException();
//            if (exception != null)
//                throw new ExecutionException(exception);
            return result;
        }
		
		void innerSet(V v) {
			for(;;) {
				int s = getState();
				if(s == RAN) {
					return ;
				}
				
				if(s == CANCELLED) {
					releaseShared(0);
					return ;
				}
				
				if(compareAndSetState(s, RAN)) {
					result = v;
					releaseShared(0);
					done();
					return ;
				}
			}
		}
		
		void innerSetException(Exception e) {
			for(;;) {
				int s = getState();
				if(s == RAN) {
					return ;
				}
				if(s == CANCELLED) {
					releaseShared(0);
					return ;
				}
				
				if(compareAndSetState(s, RAN)) {
					exception = e;
					result = null;
					releaseShared(0);
					done() ;
					return ;
				}
			}
		}
		
		boolean innerCancel(boolean mayInterruptIfRunning) {
			for(;;) {
				int s = getState();
				if(ranOrCanelled(s)) {
					return false;
				}
				if(compareAndSetState(s, CANCELLED)) {
					break;
				}
			}
			
			if(mayInterruptIfRunning) {
				Thread r = runner;
				if(r != null) {
					r.interrupt();
				}
			}
			releaseShared(0);
			done();
			return true;
		}

		void innerRun() {
			if(!compareAndSetState(0, RUNNING)) {
				return ;
			}
			
			try {
				runner = Thread.currentThread();
				if(getState() == RUNNING) {
					innerSet(callable.call());
				} else {
					releaseShared(0);
				}
			} catch (Exception ex) {
				innerSetException(ex);
			}
		}
		
	}
}
