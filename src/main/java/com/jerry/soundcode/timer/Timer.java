package com.jerry.soundcode.timer;

import java.util.Arrays;

import com.jerry.soundcode.concurrent.atomic.AtomicInteger;

public class Timer {

	private final TaskQueue queue = new TaskQueue();
	
	private final TimerThread thread = new TimerThread(queue);
	
	private final Object threadReaper = new Object() {
		protected void finalize() throws Throwable {
			synchronized (queue) {
				thread.newTasksMayBeScheduled = false;
				queue.notify();
			}
		}
	};
	
	private final static AtomicInteger nextSerialNumber = new AtomicInteger(0);
	// TODO
}

class TimerThread extends Thread {
	
	boolean newTasksMayBeScheduled = true;
	
	private TaskQueue queue;
	
	TimerThread(TaskQueue queue) {
		this.queue = queue;
	}
	
	public void run() {
		try {
			mainLoop();
		} finally {
			synchronized (queue) {
				newTasksMayBeScheduled = false;
				queue.clear();
			}
		}
		
	}

	private void mainLoop() {
		while(true) {
			try {
				TimerTask task = null;
				boolean taskFired = false;
				synchronized (queue) {
					while(queue.isEmpty() && newTasksMayBeScheduled) {
						queue.wait();
						if(queue.isEmpty()) {
							break;
						}
						
						long currentTime, executionTime;
						task = queue.getMin();
						synchronized (task.lock) {
							if(task.state == TimerTask.CANCELLED) {
								queue.removeMin();
								continue;
							}
							
							currentTime = System.currentTimeMillis();
							executionTime = task.nextExecutionTime;
							
							if(taskFired = (executionTime <= currentTime)) {
								if(task.period == 0) {
									queue.removeMin();
									task.state = TimerTask.EXECUTED;
								} else {
									queue.rescheduleMin(task.period < 0 ? currentTime - task.period : executionTime + task.period);
								}
							}
						}
						
						if(!taskFired) {
							queue.wait(executionTime - currentTime); 
						}
					}
					
				}
				
				if(taskFired) {
					task.run();
				}
			} catch(InterruptedException e) {}
		}
	}
	
}

class TaskQueue {
	
	private TimerTask[] queue = new TimerTask[128];
	
	private int size = 0;
	
	int size() {
		return size;
	}
	
	void add(TimerTask task) {
		if(size + 1 == queue.length) {
			queue = Arrays.copyOf(queue, 2 * queue.length);
		}
		queue[++size] = task;
		fixUp(size);
	}
	
	TimerTask getMin() {
		return queue[1];
	}
	
	TimerTask get(int i) {
		return queue[i];
	} 
	
	void removeMin() {
		queue[1] = queue[size];
		queue[size --] = null;
		fixDown(1);
	}
	
	void quickRemove(int i) {
		assert i <= size;
		
		queue[i] = queue[size];
		queue[size --] = null;
	}

	void rescheduleMin(long newTime) {
		queue[1].nextExecutionTime = newTime;
		fixDown(1);
	}
	
	boolean isEmpty() {
		return size == 0;
	}
	
	void clear() {
		for(int i = 0; i <= size; i++) {
			queue[i] = null;
		}
		size = 0;
	}
	
	private void fixUp(int k) {
		while(k > 1) {
			int j = k >> 1;
			if(queue[j].nextExecutionTime <= queue[k].nextExecutionTime) {
				break;
			}
			TimerTask tmp = queue[j];
			queue[j] = queue[k];
			queue[k] = tmp;
			k = j; 
		}
	}
	
	private void fixDown(int k) {
		int j;
		while((j = k << 1) <= size && j > 0) {
			if(j < size && queue[j].nextExecutionTime > queue[j + 1].nextExecutionTime) {
				j ++;
			} 
			if(queue[k].nextExecutionTime <= queue[j].nextExecutionTime) {
				break;
			}
			TimerTask tmp = queue[j];
			queue[j] = queue[k];
			queue[k] = tmp;
			k = j;
		}
	}
	
	void heapify() {
		for(int i = size / 2; i >= 1; i--) {
			fixDown(i);
		}
	}
	
}