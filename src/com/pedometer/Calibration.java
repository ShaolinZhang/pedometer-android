package com.pedometer;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.TextView;

/*校准界面 */
public class Calibration extends Activity{

	/*创建画布对象，绘制加速度曲线*/
	private CanvasView canvasView = null;
	
	/*设置加速度曲线画布上所能绘制的最大加速度*/
    private int Paint_maxValue = 10;
    
	/*文字对象，显示加速度峰值*/
	private TextView maxValue, minValue, standardMax, standardMin =null;
	
	/*设置是否分开显示三轴加速度*/
	private CheckBox disDetach = null;
	
	/*画布大小，用来获得绘制的相对位置*/
	private int ViewWidth, ViewHeight;
	
	/*计时器相关内容，用来等待窗体载入完成后获取画布尺寸并初始化*/
	private Handler handler;
	private Timer myTimer;
	
	/*加速度传感器*/
    private SensorManager sm = null;
    private Sensor sensor = null;
    private SensorListener sensorListener = new SensorListener();
    
    /*声音对象*/
    private SoundPool soundPool;
    
    /*声音编号*/
    private int loadId;
    
    /*设置小数的显示格式使布局看起来整齐*/
    private DecimalFormat format = new DecimalFormat("##0.0000000"); 
    
    /*标示是否有触摸事件，当为true时进行校正*/
    private boolean Touched = false;
    
    /*上一次波峰波谷的值*/
    private float maximum = 0, minimum = 0;
    
    /*用于判断上一次波幅是否为正常跑步波动的标准值*/
    /*（在屏幕上显示的时两倍的标准值，但为了防止波动，实际判断标准是平均波幅一半）*/
    public static float standardP = 1, standardN = -1;
    
    /*校正过程中记录近九次峰值的数组，用于取中位数作为判断用的标准值*/
    private int ArraySize=9;
    private float[] maxValueArray = new float[ArraySize];
    private float[] minValueArray = new float[ArraySize];
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calibration);
		/*加载View对象*/
		canvasView = (CanvasView)findViewById(R.id.MycanvasView);
		maxValue = (TextView)findViewById(R.id.maxvalue);
		minValue = (TextView)findViewById(R.id.minvalue);
		standardMax = (TextView)findViewById(R.id.standardmax);
		standardMin = (TextView)findViewById(R.id.standardmin);
		disDetach = (CheckBox)findViewById(R.id.displaydetach);
		standardMax.setText(format.format(standardP*2));
    	standardMin.setText(format.format(standardN*2));
    	
    	/*初始化标准值*/
    	for(int i=0;i<ArraySize;i++)
    	{
    		maxValueArray[i] = standardP*2;
    		minValueArray[i] = standardN*2;
    	}
    	
    	/*初始化声音对象*/
		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);  
		loadId = soundPool.load(this,R.raw.sound, 1);  
		
		/*初始化计时器*/
		handler = new Handler() {   
            @Override  
            public void handleMessage(Message msg) {   
                super.handleMessage(msg);    
                if(msg.what>0){  
                	/*获取画布的长宽，当不为零（加载完成）是进行初始化并取消计时器*/
            		ViewWidth = canvasView.getWidth();
            		ViewHeight = canvasView.getHeight();
            		if(ViewWidth!=0 && ViewHeight!=0)
            		{	
            			initialize();
            			myTimer.cancel();
            		}
                }
            }
        };
        myTimer = new Timer();
		myTimer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run() {
			 	Message msg = new Message();   
             	msg.what = 1;   
             	handler.sendMessage(msg);
			}
		}, 0, 100);
	}

	/*检测到退出键按下时，消除传感器监听，将校准到的标准值传回主程序，结束当前活动*/
	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {         
		if(keyCode == KeyEvent.KEYCODE_BACK){
			sm.unregisterListener(sensorListener, sensor);
			MainActivity.WriteConfig(standardP, standardN);
			this.finish();
		}
		return false;
	}

	/*初始化*/
	private void initialize() {
		/*初始化传感器，设置为加速度传感器*/
		sm = (SensorManager) getSystemService(SENSOR_SERVICE); 
		sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		/*注册传感器监听器*/
		sm.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME);
		
		/*画布绘制边框及刻度*/
		canvasView.drawScale(ViewWidth, ViewHeight);
	}
	
	/*触摸事件，修改Touched变量的值*/
	public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
        	Touched = true;
        	break;
        case MotionEvent.ACTION_UP:
        	Touched = false;
        	break;
        }
        return true;
	}
	
	/*传感器监听类*/
	private class SensorListener implements SensorEventListener{

		/*重力数组，用于动态计算重力并从加速度数据中消除*/
	    private float[] gravity=new float[3];
	    private float alpha = (float) 0.8; /*重力计算系数，好像等于t/(t+dT),t为时间，dT为时间导数*/
	    
	    /*原始三轴加速度值*/
	    private float[] raw_acc=new float[3];
	    
	    /*储存三轴以及综合加速度，用于绘制曲线。acc_N为指针，Storage为储存数*/
	    private int acc_N=0, Storage = 20;
	    private float[] x_acc = new float[Storage];
	    private float[] y_acc = new float[Storage];
	    private float[] z_acc = new float[Storage];
	    private float[] total_acc = new float[Storage];
	    
	    /*AccArrayFull为判断储存数组是否已满，若已满则开始覆盖储存*/
	    /*positive为记录上一次获得的加速度数据是否大于0，作为波峰和波谷曲线的分界*/
	    /*step用于记录上一次是否有波谷，如果有，在下一次遇到波峰时则认为迈了一步*/
	    private boolean AccArrayFull = false, positive = false, step = false;
	    
    	@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		@Override
		public void onSensorChanged(SensorEvent event) {
			for(int i=0;i<3;i++)
			{
				/*获取原始加速度数据，并计算重力值*/
				gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];
				raw_acc[i] = event.values[i]-gravity[i];
			}
			
			/*将原始数据存入用于绘制曲线的数据*/
			 x_acc[acc_N] = raw_acc[0]; y_acc[acc_N] = raw_acc[1]; z_acc[acc_N] = raw_acc[2];
			
			 /*判断如果波幅最大的两个轴上的加速度相反，则其中一个去相反数，以免三轴相加获取综合加速度时抵消掉*/
			 for(int i=0;i<2;i++)
				for(int j=i+1;j<3;j++)
					if(Math.abs(raw_acc[i])>Math.abs(raw_acc[3-i-j]) && Math.abs(raw_acc[j])>Math.abs(raw_acc[3-i-j]) && (raw_acc[i]*raw_acc[j]<0))
						raw_acc[i]=-raw_acc[i];
			
			 /*综合加速度等于三轴上加速度之和*/
			 total_acc[acc_N] = (raw_acc[0]+raw_acc[1]+raw_acc[2])/3;
			
			 /*判断如果上一次加速度为正而这一次为负，则上一段正加速度结束，获取上一段加速度中最大值作为波峰*/
			 if(positive && total_acc[acc_N]<0)
			{
				if(maximum!=0)
					maxValue.setText(format.format(maximum));
				
				/*如果有触摸事件（正在校正）且数值合理，不是轻微抖动，则将峰值放入计算标准值的数组*/
				if(Touched && maximum>0.3)
				{
					/*将最新数字加到数组开头*/
					for(int i=maxValueArray.length-1; i>0; i--)
					{	
						maxValueArray[i]=maxValueArray[i-1];
					}
					maxValueArray[0]=maximum;
					
					/*正加速度标准值等于数组中9次加速度数据的中位数的一半*/
					standardP=(float) (median(maxValueArray)*0.5);
					
					/*显示标准值*/
					standardMax.setText(format.format(standardP*2));
				}
				/*positive标为false表示进入负加速度段*/
				positive = false;
				
				/*如果当前最大值大于标准值并且之前有过波谷，判断为迈出一步，播放提示音*/
				if(maximum>standardP && step)	
					soundPool.play(loadId, 1, 1, 0, 0, 1);  
				else /*反之step为false重新开始判断波谷是否出现*/
					step = false;
				
				/*最大值清零，准备下一轮计算*/
				maximum = 0;
			}
			 /*负加速度段同理*/
			else if(!positive && total_acc[acc_N]>0)
			{
				if(minimum!=0)
					minValue.setText(format.format(minimum));
				if(Touched && minimum<-0.3)
				{
					for(int i=minValueArray.length-1; i>0; i--)
					{	
						minValueArray[i]=minValueArray[i-1];
					}
					minValueArray[0]=minimum;
					standardN=(float) (median(minValueArray)*0.5);
					standardMin.setText(format.format(standardN*2));
				}
				positive = true;
				if(minimum<standardN)	
					step = true;
				else
					step = false;
				minimum = 0;
			}
			 /*如果在正加速度段并且当前加速度大于记录到的最大值，最大值等于当前加速度*/
			else if(positive && total_acc[acc_N]>maximum)
				maximum = total_acc[acc_N];
			
			 /*负加速度段同理*/
			else if(!positive && total_acc[acc_N]<minimum)
				minimum = total_acc[acc_N];
			
			 /*将用于绘制曲线的数组指针加一，并判断如果数组以写到尾端，指针回到开头开始覆盖书写*/
			 if(++acc_N==Storage) { AccArrayFull = true; acc_N=0;}
			 
			 /*绘制曲线，传入是否分开绘制三轴加速度的bool值*/
			paint(disDetach.isChecked());
		}
		
		/*用于计算传入数组的中位数*/
		private float median(float[] data)
		{
			float[] a = new float[data.length];
			for(int i=0;i<a.length;i++)
				a[i] = data[i];
			float temp = 0;
	        for (int i = a.length - 1; i > 0; --i) {
	            for (int j = 0; j < i; ++j) {
	                if (a[j + 1] < a[j]) {
	                    temp = a[j];
	                    a[j] = a[j + 1];
	                    a[j + 1] = temp;
	                }
	            }
	        }
	        return a[a.length/2];	
		}
		
		/*用于绘制曲线*/
		private void paint(boolean detach)
		{
			/*每一个数据点之间的横向间隔*/
			float WidthPerDot;
			
			/*数值真实值与绘制点高度的比例*/
			float HeightRatio = ViewHeight/2;
			
			/*从左向右第几个点*/
			int x = 0;
			
			HeightRatio/=Paint_maxValue;
			if(!AccArrayFull) WidthPerDot = (float)ViewWidth / acc_N;
			else WidthPerDot = (float)ViewWidth / (Storage-1);
			
			/*初始化绘制，设置起点*/
			canvasView.erase();
			canvasView.draw(0, 0,ViewHeight/2,true);
			canvasView.draw(1, 0,ViewHeight/2,true);
			canvasView.draw(2, 0,ViewHeight/2,true);
			
			/*绘制*/
			/*如果储存数组是满的，则代表acc_N之前是覆盖上去的最新数据，之后是最之前的数据，所以先绘制后面的数据*/
			for(int i=acc_N; i<Storage && AccArrayFull;i++,x++)
			{
				if(detach){
					canvasView.draw(0, x*WidthPerDot,(x_acc[i]+Paint_maxValue)*HeightRatio,false);
					canvasView.draw(1, x*WidthPerDot,(y_acc[i]+Paint_maxValue)*HeightRatio,false);
					canvasView.draw(2, x*WidthPerDot,(z_acc[i]+Paint_maxValue)*HeightRatio,false);
				}
				else
					canvasView.draw(0, x*WidthPerDot,(total_acc[i]+Paint_maxValue)*HeightRatio,false);
			}
			
			/*acc_N之后的旧数据绘制完后继续从头开始绘制*/
			for(int i=0; i<acc_N ;i++,x++)
			{
				if(detach){
					canvasView.draw(0, x*WidthPerDot,(x_acc[i]+Paint_maxValue)*HeightRatio,false);
					canvasView.draw(1, x*WidthPerDot,(y_acc[i]+Paint_maxValue)*HeightRatio,false);
					canvasView.draw(2, x*WidthPerDot,(z_acc[i]+Paint_maxValue)*HeightRatio,false);
				}
				else
					canvasView.draw(0, x*WidthPerDot,(total_acc[i]+Paint_maxValue)*HeightRatio,false);
			}
		}

	}
}
