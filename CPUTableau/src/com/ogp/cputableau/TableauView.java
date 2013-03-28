package com.ogp.cputableau;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;


class TableauView extends View 
{
    private Bitmap mBitmap;

    
    public TableauView(Context context) 
    {
        super(context);

        final int W = 200;
        final int H = 200; 

        mBitmap = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565);
    }

    
    @Override 
    protected void onDraw(Canvas canvas) 
    {
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }
}
