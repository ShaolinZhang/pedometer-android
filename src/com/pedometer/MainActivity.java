package com.pedometer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

	private Intent CalibrationIntent =new Intent();
	private static float StandardMax, StandardMin;
	private static MainActivity mainActivity;
	protected long stepNum = 0;
	private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private SensorListener sensorListener = new SensorListener();
    private TextView dis_StepNum = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		CalibrationIntent.setClass(this, Calibration.class);
		mainActivity = this;
		LoadConfig();
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME);
		dis_StepNum = (TextView)findViewById(R.id.stepnum);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(Menu.NONE,0,0,"关于 About");
		menu.add(Menu.NONE,1,0,"校准 Calibration");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case 0:
			setContentView(R.layout.about);
		case 1:
			startActivity(CalibrationIntent);
			break;
		case 3:
			break;
		}
		return false;
	}
	
	/*写文件，写入校正的标准值*/
	public static void WriteConfig(float StandardMax, float StandardMin)
	{
		MainActivity.StandardMax = StandardMax;
		MainActivity.StandardMin = StandardMin;
		try {
			FileOutputStream outStream;
			outStream = mainActivity.openFileOutput("config.ini", MODE_PRIVATE);
			outStream.write((String.valueOf(StandardMax)+":"+String.valueOf(StandardMin)).getBytes());
	        outStream.close(); 
		} 
		catch (FileNotFoundException e) {}
		catch (IOException e) {}
	}
	
	/*读文件，读取之前的标准值，如果没读到则代表初次运行，标准值设置为默认值并进入校正界面*/
	private void LoadConfig()
	{
		try {
			FileInputStream inStream;
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			inStream = this.openFileInput("config.ini");
			byte[] data = null;
			byte[] buffer = new byte[1024];
			int len = 0;
			while( (len = inStream.read(buffer))!= -1)
				outStream.write(buffer, 0, len);
			outStream.close();
			inStream.close();
			data = outStream.toByteArray();
			String DataRead = new String(data);
			String[] Figure = DataRead.split(":");
			StandardMax = Float.parseFloat(Figure[0]);
			StandardMin = Float.parseFloat(Figure[1]);
			Calibration.standardP = StandardMax;
			Calibration.standardN = StandardMin;		
		}
		catch (FileNotFoundException e) {
			StandardMax = Calibration.standardP;
			StandardMin = Calibration.standardN;
			AlertDialog.Builder dialog=new AlertDialog.Builder(this);  
            dialog.setTitle("欢迎使用").setMessage("初次使用，请先校正数值。")
            				   .setPositiveButton("确定",new OnClickListener(){
								@Override
								public void onClick(DialogInterface dialog,int which) {
									startActivity(CalibrationIntent);
								}
            }).create().show();
		}
		catch (Exception e) {}
	}
	
	/*加速度监听器，与校正界面完全相同，去除了排序绘制部分*/
	private class SensorListener implements SensorEventListener{
	    private float[] gravity=new float[3];
	    private float[] raw_acc=new float[3];
	    private float alpha = (float) 0.8;
	    private float acc, maximum, minimum;
	    private boolean positive = false, step = false;
    	@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		@Override
		public void onSensorChanged(SensorEvent event) {
			for(int i=0;i<3;i++)
			{
				gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];
				raw_acc[i] = event.values[i]-gravity[i];
			}
			for(int i=0;i<2;i++)
			for(int j=i+1;j<3;j++)
				if(Math.abs(raw_acc[i])>Math.abs(raw_acc[3-i-j]) && Math.abs(raw_acc[j])>Math.abs(raw_acc[3-i-j]) && (raw_acc[i]*raw_acc[j]<0))
					raw_acc[i]=-raw_acc[i];
			acc = (raw_acc[0]+raw_acc[1]+raw_acc[2])/3;
			if(positive && acc<0)
			{
				if(maximum>StandardMax && step) step = true;
				else step = false;
				positive = false; maximum = 0;
			}
			else if(!positive && acc>0)
			{
				if(minimum<StandardMin)	Step();
				else step = false;
				positive = true; minimum = 0;
			}
			else if(positive && acc>maximum)
				maximum = acc;//acc=acceleration?
			else if(!positive && acc<minimum)
				minimum = acc;
		}
	}
	
	/*检测到迈步后执行的操作*/
	private void Step()
	{
		stepNum++;
		dis_StepNum.setText(String.valueOf(stepNum));
	}
}
