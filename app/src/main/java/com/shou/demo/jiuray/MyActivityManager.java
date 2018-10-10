package com.shou.demo.jiuray;

import android.app.Activity;
import android.app.Application;

import java.util.LinkedList;
import java.util.List;

public class MyActivityManager extends Application {
	
	private List<Activity> activityList = new LinkedList<Activity>();
	private static MyActivityManager manager ;
	
//	private MyActivityManager(){}
	
	@Override
	public void onCreate() {
		if(manager != null){
			manager = new MyActivityManager();
		}
		super.onCreate();
	}
	
//	public static MyActivityManager getInstance(){
//		if(manager != null){
//			manager = new MyActivityManager();
//		}
//		return manager ;
//	}
	
	public void addActivity(Activity activity){
		activityList.add(activity);
	}
	
	public void exit(){
		for(Activity activity : activityList){
			activity.finish();
		}
		
		System.exit(0);
	}

}
