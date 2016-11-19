package com.pedometer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/*�Խ�View�����ڻ��Ƽ��ٶȴ������Ĳ���ͼ*/
public class CanvasView extends View {
	
	/*�������ϼ��ٶȵĶ�����Ե�paint��path*/
	private Paint[] brush = { new Paint(), new Paint(), new Paint()};
    private Path [] path  = { new Path() , new Path() , new Path() };
    
    /*����ͼ�߿򼰿̶ȵ�paint��path*/
    private Paint ScaleBrush = new Paint();
    private Path  ScalePath  = new Path ();
    
    /*�Խ�View�����ֳ�ʼ�����ͣ�Ҫ�ŵ�xml�ļ��Ļ�����ȱһ����*/
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
    
    /*��ʼ����������*/
    private void initialize()
    {
    	/*��ʼ���������ϼ��ٶȻ��Ƶ�brush����*/
    	for(int i=0;i<brush.length;i++)
    	{
    		brush[i].setAntiAlias(true);
    		brush[i].setStyle(Paint.Style.STROKE);
    		brush[i].setStrokeJoin(Paint.Join.ROUND);
    		/*�������4����*/
    		brush[i].setStrokeWidth(4);
    		
        }
		brush[0].setColor(Color.BLUE);  /*x����ٶ���ɫ����Ϊ��ɫ*/
		brush[1].setColor(Color.GREEN); /*y����ٶ���ɫ����Ϊ��ɫ*/
		brush[2].setColor(Color.RED);   /*z����ٶ���ɫ����Ϊ��ɫ*/
		
		/*�߿�Ϳ̶��������Ƶ�brush����*/
		ScaleBrush.setAntiAlias(true);
    	ScaleBrush.setStyle(Paint.Style.STROKE);
    	ScaleBrush.setStrokeJoin(Paint.Join.ROUND);
    	
    	/*�߿�Ϳ̶��������2���أ���ɫ��ɫ*/
    	ScaleBrush.setStrokeWidth(2);
    	ScaleBrush.setColor(Color.GRAY);
    	
    }
    
    /*�������������ڲ�����������*/
    public void erase()
    {
    	for(int i=0;i<brush.length;i++)
    		path[i].reset();
		postInvalidate();
    }
    
    /*���Ʊ߿�Ϳ̶�����*/
    public void drawScale(int width,int height) /*width��heightΪView�ĳ���*/
    {
    	/*��(0��0)����(0, height)*/
    	ScalePath.moveTo(0, 0);
		ScalePath.lineTo(0, height);
		
		/*��(width��0)����(width, height)*/
		ScalePath.moveTo(width, 0);
		ScalePath.lineTo(width, height);
    	
		/*����ˮƽ�̶���*/
		for(int i=0;i<=4;i++)
    	{
    		ScalePath.moveTo(0, i*height/4);
    		ScalePath.lineTo(width, i*height/4);
    	}
    	postInvalidate();
    }
    
    /*���ƺ��������ڻ����������ٶ�����*/
    /*NumΪ���1-x�ᣬ2-y�ᣬ3-z��*/
    /*pointX��pointYΪ���������յ�λ��*/
    /*NewStartingΪtrue��ʾ�ƶ�������㣬false��ʾ����һ�λ���λ�û����µĻ���λ��*/
    public void draw(int Num, float pointX, float pointY, boolean NewStarting)
    {
    	if(NewStarting)
    		path[Num].moveTo(pointX, pointY);
    	else
    		path[Num].lineTo(pointX, pointY);
    	postInvalidate();
    }
    
    @Override
    /*���»���*/
    public void onDraw(Canvas canvas) {
    	canvas.drawPath(ScalePath, ScaleBrush);
    	for(int i=0;i<brush.length;i++)
            canvas.drawPath(path[i], brush[i]);
    }
}
