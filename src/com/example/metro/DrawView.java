package com.example.metro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

@SuppressLint({ "NewApi", "ViewConstructor" })
public class DrawView extends View {
    private Paint paint = new Paint();
    private int[] screen;
    private int[] size;
    private int[] itemCount;
    
    public DrawView(Context context,int[]... data) {
        super(context);
        paint.setColor(Color.WHITE);
        this.screen = data[0];
        this.size = data[1];
        this.itemCount = data[2];
        setBackgroundColor(Color.DKGRAY);
        setAlpha((float) 0.2);
    }

    @Override
    public void onDraw(Canvas canvas) {
	
    	for (int i = 0; i < itemCount[0]; i++) 
    		canvas.drawLine(size[0]*i, 0, size[0]*i, screen[1], paint);
    	
    	
    	for (int i = 0; i < itemCount[1]; i++)
    		canvas.drawLine(0, size[1]*i, screen[0], size[1]*i, paint);
    }

}
