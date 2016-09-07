package com.jerry.soundcode.timer;


/**
 * 创建定时器传统方式
 * @author Jerry Wang
 * @Email  jerry002@126.com
 * @date   2016年9月7日
 */
public class TraditionalTimer {
	
	public static void main(String[] args) {
		
		new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
				System.out.println("bombing");
			}
		}, 10000, 3000);
		
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
