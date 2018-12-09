package com.ucm.tfg.cinema.SampleAppMenu;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * This class configures the Sample App Menu layout
 */
public class SampleAppMenuView extends LinearLayout
{

    private int horizontalClipping = 0;


    public SampleAppMenuView(Context context)
    {
        super(context);
    }


    public SampleAppMenuView(Context context, AttributeSet attribute)
    {
        super(context, attribute);
    }


    @Override
    public void onDraw(Canvas canvas)
    {
        canvas.clipRect(0, 0, horizontalClipping, canvas.getHeight());
        super.onDraw(canvas);
    }


    public void setHorizontalClipping(int hClipping)
    {
        horizontalClipping = hClipping;
        invalidate();
    }

}