package com.example.metro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

@SuppressLint("NewApi")
public class DrawView extends View {
    private Paint paint = new Paint();
    private int[] screen;
    private int[] size;
    
    public DrawView(Context context,int[] screen,int[] size) {
        super(context);
        paint.setColor(Color.WHITE);
        this.screen = screen;
        this.size = size;
        setBackgroundColor(Color.GRAY);
        setAlpha((float) 0.2);
    }

    @Override
    public void onDraw(Canvas canvas) {
	
            canvas.drawLine(0, 0, 0, screen[1], paint);
            canvas.drawLine(size[0], 0, size[0], screen[1], paint);
            canvas.drawLine(size[0]*2, 0, size[0]*2, screen[1], paint);
            canvas.drawLine(size[0]*3, 0, size[0]*3, screen[1], paint);
            
            canvas.drawLine(0, 0, screen[0], 0, paint);
            canvas.drawLine(0, size[1], screen[0], size[1], paint);
            canvas.drawLine(0, size[1]*2, screen[0], size[1]*2, paint);
            canvas.drawLine(0, size[1]*3, screen[0], size[1]*3, paint);
            canvas.drawLine(0, size[1]*4, screen[0], size[1]*4, paint);
    }

}
