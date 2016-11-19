package com.pedometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/*自建View，用于绘制加速度传感器的波形图*/
public class CanvasView extends View {
	
	/*三个轴上加速度的对象各自的paint和path*/
	private Paint[] brush = { new Paint(), new Paint(), new Paint()};
    private Path [] path  = { new Path() , new Path() , new Path() };
    
    /*波形图边框及刻度的paint和path*/
    private Paint ScaleBrush = new Paint();
    private Path  ScalePath  = new Path ();
    
    /*自建View的三种初始化类型，要放到xml文件的话三个缺一不可*/
    public CanvasView(Context context) {
        super(context);
        initialize();
    }
    
    public CanvasView(Context context, AttributeSet attrs) {
    	super( context, attrs );
    	initialize();
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyle) {
    	super( context, attrs, defStyle );
    	initialize();
    }
    
    /*初始化画布对象*/
    private void initialize()
    {
    	/*初始化三个轴上加速度绘制的brush属性*/
    	for(int i=0;i<brush.length;i++)
    	{
    		brush[i].setAntiAlias(true);
    		brush[i].setStyle(Paint.Style.STROKE);
    		brush[i].setStrokeJoin(Paint.Join.ROUND);
    		/*线条宽度4像素*/
    		brush[i].setStrokeWidth(4);
    		
        }
		brush[0].setColor(Color.BLUE);  /*x轴加速度颜色设置为蓝色*/
		brush[1].setColor(Color.GREEN); /*y轴加速度颜色设置为绿色*/
		brush[2].setColor(Color.RED);   /*z轴加速度颜色设置为红色*/
		
		/*边框和刻度线条绘制的brush属性*/
		ScaleBrush.setAntiAlias(true);
    	ScaleBrush.setStyle(Paint.Style.STROKE);
    	ScaleBrush.setStrokeJoin(Paint.Join.ROUND);
    	
    	/*边框和刻度线条宽度2像素，颜色灰色*/
    	ScaleBrush.setStrokeWidth(2);
    	ScaleBrush.setColor(Color.GRAY);
    	
    }
    
    /*擦除函数，用于擦除所有线条*/
    public void erase()
    {
    	for(int i=0;i<brush.length;i++)
    		path[i].reset();
		postInvalidate();
    }
    
    /*绘制边框和刻度线条*/
    public void drawScale(int width,int height) /*width与height为View的长宽*/
    {
    	/*从(0，0)画到(0, height)*/
    	ScalePath.moveTo(0, 0);
		ScalePath.lineTo(0, height);
		
		/*从(width，0)画到(width, height)*/
		ScalePath.moveTo(width, 0);
		ScalePath.lineTo(width, height);
    	
		/*五条水平刻度线*/
		for(int i=0;i<=4;i++)
    	{
    		ScalePath.moveTo(0, i*height/4);
    		ScalePath.lineTo(width, i*height/4);
    	}
    	postInvalidate();
    }
    
    /*绘制函数，用于绘制三条加速度曲线*/
    /*Num为编号1-x轴，2-y轴，3-z轴*/
    /*pointX与pointY为绘制线条终点位置*/
    /*NewStarting为true表示移动到新起点，false表示从上一次绘制位置画到新的绘制位置*/
    public void draw(int Num, float pointX, float pointY, boolean NewStarting)
    {
    	if(NewStarting)
    		path[Num].moveTo(pointX, pointY);
    	else
    		path[Num].lineTo(pointX, pointY);
    	postInvalidate();
    }
    
    @Override
    /*更新画布*/
    public void onDraw(Canvas canvas) {
    	canvas.drawPath(ScalePath, ScaleBrush);
    	for(int i=0;i<brush.length;i++)
            canvas.drawPath(path[i], brush[i]);
    }
}
