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

/*У׼���� */
public class Calibration extends Activity{

	/*�����������󣬻��Ƽ��ٶ�����*/
	private CanvasView canvasView = null;
	
	/*���ü��ٶ����߻��������ܻ��Ƶ������ٶ�*/
    private int Paint_maxValue = 10;
    
	/*���ֶ�����ʾ���ٶȷ�ֵ*/
	private TextView maxValue, minValue, standardMax, standardMin =null;
	
	/*�����Ƿ�ֿ���ʾ������ٶ�*/
	private CheckBox disDetach = null;
	
	/*������С��������û��Ƶ����λ��*/
	private int ViewWidth, ViewHeight;
	
	/*��ʱ��������ݣ������ȴ�����������ɺ��ȡ�����ߴ粢��ʼ��*/
	private Handler handler;
	private Timer myTimer;
	
	/*���ٶȴ�����*/
    private SensorManager sm = null;
    private Sensor sensor = null;
    private SensorListener sensorListener = new SensorListener();
    
    /*��������*/
    private SoundPool soundPool;
    
    /*�������*/
    private int loadId;
    
    /*����С������ʾ��ʽʹ���ֿ���������*/
    private DecimalFormat format = new DecimalFormat("##0.0000000"); 
    
    /*��ʾ�Ƿ��д����¼�����Ϊtrueʱ����У��*/
    private boolean Touched = false;
    
    /*��һ�β��岨�ȵ�ֵ*/
    private float maximum = 0, minimum = 0;
    
    /*�����ж���һ�β����Ƿ�Ϊ�����ܲ������ı�׼ֵ*/
    /*������Ļ����ʾ��ʱ�����ı�׼ֵ����Ϊ�˷�ֹ������ʵ���жϱ�׼��ƽ������һ�룩*/
    public static float standardP = 1, standardN = -1;
    
    /*У�������м�¼���Ŵη�ֵ�����飬����ȡ��λ����Ϊ�ж��õı�׼ֵ*/
    private int ArraySize=9;
    private float[] maxValueArray = new float[ArraySize];
    private float[] minValueArray = new float[ArraySize];
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calibration);
		/*����View����*/
		canvasView = (CanvasView)findViewById(R.id.MycanvasView);
		maxValue = (TextView)findViewById(R.id.maxvalue);
		minValue = (TextView)findViewById(R.id.minvalue);
		standardMax = (TextView)findViewById(R.id.standardmax);
		standardMin = (TextView)findViewById(R.id.standardmin);
		disDetach = (CheckBox)findViewById(R.id.displaydetach);
		standardMax.setText(format.format(standardP*2));
    	standardMin.setText(format.format(standardN*2));
    	
    	/*��ʼ����׼ֵ*/
    	for(int i=0;i<ArraySize;i++)
    	{
    		maxValueArray[i] = standardP*2;
    		minValueArray[i] = standardN*2;
    	}
    	
    	/*��ʼ����������*/
		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);  
		loadId = soundPool.load(this,R.raw.sound, 1);  
		
		/*��ʼ����ʱ��*/
		handler = new Handler() {   
            @Override  
            public void handleMessage(Message msg) {   
                super.handleMessage(msg);    
                if(msg.what>0){  
                	/*��ȡ�����ĳ�������Ϊ�㣨������ɣ��ǽ��г�ʼ����ȡ����ʱ��*/
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

	/*��⵽�˳�������ʱ��������������������У׼���ı�׼ֵ���������򣬽�����ǰ�*/
	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {         
		if(keyCode == KeyEvent.KEYCODE_BACK){
			sm.unregisterListener(sensorListener, sensor);
			MainActivity.WriteConfig(standardP, standardN);
			this.finish();
		}
		return false;
	}

	/*��ʼ��*/
	private void initialize() {
		/*��ʼ��������������Ϊ���ٶȴ�����*/
		sm = (SensorManager) getSystemService(SENSOR_SERVICE); 
		sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		/*ע�ᴫ����������*/
		sm.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME);
		
		/*�������Ʊ߿򼰿̶�*/
		canvasView.drawScale(ViewWidth, ViewHeight);
	}
	
	/*�����¼����޸�Touched������ֵ*/
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
	
	/*������������*/
	private class SensorListener implements SensorEventListener{

		/*�������飬���ڶ�̬�����������Ӽ��ٶ�����������*/
	    private float[] gravity=new float[3];
	    private float alpha = (float) 0.8; /*��������ϵ�����������t/(t+dT),tΪʱ�䣬dTΪʱ�䵼��*/
	    
	    /*ԭʼ������ٶ�ֵ*/
	    private float[] raw_acc=new float[3];
	    
	    /*���������Լ��ۺϼ��ٶȣ����ڻ������ߡ�acc_NΪָ�룬StorageΪ������*/
	    private int acc_N=0, Storage = 20;
	    private float[] x_acc = new float[Storage];
	    private float[] y_acc = new float[Storage];
	    private float[] z_acc = new float[Storage];
	    private float[] total_acc = new float[Storage];
	    
	    /*AccArrayFullΪ�жϴ��������Ƿ���������������ʼ���Ǵ���*/
	    /*positiveΪ��¼��һ�λ�õļ��ٶ������Ƿ����0����Ϊ����Ͳ������ߵķֽ�*/
	    /*step���ڼ�¼��һ���Ƿ��в��ȣ�����У�����һ����������ʱ����Ϊ����һ��*/
	    private boolean AccArrayFull = false, positive = false, step = false;
	    
    	@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		@Override
		public void onSensorChanged(SensorEvent event) {
			for(int i=0;i<3;i++)
			{
				/*��ȡԭʼ���ٶ����ݣ�����������ֵ*/
				gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];
				raw_acc[i] = event.values[i]-gravity[i];
			}
			
			/*��ԭʼ���ݴ������ڻ������ߵ�����*/
			 x_acc[acc_N] = raw_acc[0]; y_acc[acc_N] = raw_acc[1]; z_acc[acc_N] = raw_acc[2];
			
			 /*�ж�������������������ϵļ��ٶ��෴��������һ��ȥ�෴��������������ӻ�ȡ�ۺϼ��ٶ�ʱ������*/
			 for(int i=0;i<2;i++)
				for(int j=i+1;j<3;j++)
					if(Math.abs(raw_acc[i])>Math.abs(raw_acc[3-i-j]) && Math.abs(raw_acc[j])>Math.abs(raw_acc[3-i-j]) && (raw_acc[i]*raw_acc[j]<0))
						raw_acc[i]=-raw_acc[i];
			
			 /*�ۺϼ��ٶȵ��������ϼ��ٶ�֮��*/
			 total_acc[acc_N] = (raw_acc[0]+raw_acc[1]+raw_acc[2])/3;
			
			 /*�ж������һ�μ��ٶ�Ϊ������һ��Ϊ��������һ�������ٶȽ�������ȡ��һ�μ��ٶ������ֵ��Ϊ����*/
			 if(positive && total_acc[acc_N]<0)
			{
				if(maximum!=0)
					maxValue.setText(format.format(maximum));
				
				/*����д����¼�������У��������ֵ����������΢�������򽫷�ֵ��������׼ֵ������*/
				if(Touched && maximum>0.3)
				{
					/*���������ּӵ����鿪ͷ*/
					for(int i=maxValueArray.length-1; i>0; i--)
					{	
						maxValueArray[i]=maxValueArray[i-1];
					}
					maxValueArray[0]=maximum;
					
					/*�����ٶȱ�׼ֵ����������9�μ��ٶ����ݵ���λ����һ��*/
					standardP=(float) (median(maxValueArray)*0.5);
					
					/*��ʾ��׼ֵ*/
					standardMax.setText(format.format(standardP*2));
				}
				/*positive��Ϊfalse��ʾ���븺���ٶȶ�*/
				positive = false;
				
				/*�����ǰ���ֵ���ڱ�׼ֵ����֮ǰ�й����ȣ��ж�Ϊ����һ����������ʾ��*/
				if(maximum>standardP && step)	
					soundPool.play(loadId, 1, 1, 0, 0, 1);  
				else /*��֮stepΪfalse���¿�ʼ�жϲ����Ƿ����*/
					step = false;
				
				/*���ֵ���㣬׼����һ�ּ���*/
				maximum = 0;
			}
			 /*�����ٶȶ�ͬ��*/
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
			 /*����������ٶȶβ��ҵ�ǰ���ٶȴ��ڼ�¼�������ֵ�����ֵ���ڵ�ǰ���ٶ�*/
			else if(positive && total_acc[acc_N]>maximum)
				maximum = total_acc[acc_N];
			
			 /*�����ٶȶ�ͬ��*/
			else if(!positive && total_acc[acc_N]<minimum)
				minimum = total_acc[acc_N];
			
			 /*�����ڻ������ߵ�����ָ���һ�����ж����������д��β�ˣ�ָ��ص���ͷ��ʼ������д*/
			 if(++acc_N==Storage) { AccArrayFull = true; acc_N=0;}
			 
			 /*�������ߣ������Ƿ�ֿ�����������ٶȵ�boolֵ*/
			paint(disDetach.isChecked());
		}
		
		/*���ڼ��㴫���������λ��*/
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
		
		/*���ڻ�������*/
		private void paint(boolean detach)
		{
			/*ÿһ�����ݵ�֮��ĺ�����*/
			float WidthPerDot;
			
			/*��ֵ��ʵֵ����Ƶ�߶ȵı���*/
			float HeightRatio = ViewHeight/2;
			
			/*�������ҵڼ�����*/
			int x = 0;
			
			HeightRatio/=Paint_maxValue;
			if(!AccArrayFull) WidthPerDot = (float)ViewWidth / acc_N;
			else WidthPerDot = (float)ViewWidth / (Storage-1);
			
			/*��ʼ�����ƣ��������*/
			canvasView.erase();
			canvasView.draw(0, 0,ViewHeight/2,true);
			canvasView.draw(1, 0,ViewHeight/2,true);
			canvasView.draw(2, 0,ViewHeight/2,true);
			
			/*����*/
			/*����������������ģ������acc_N֮ǰ�Ǹ�����ȥ���������ݣ�֮������֮ǰ�����ݣ������Ȼ��ƺ��������*/
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
			
			/*acc_N֮��ľ����ݻ�����������ͷ��ʼ����*/
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
