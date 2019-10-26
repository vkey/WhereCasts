package com.krisdb.wearcasts.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CurvedClockView extends View  {

    private static final String QUOTE = "12:24 PM";
    private Path mArc;
    private Paint mPaintText;

    public CurvedClockView(Context context) {
        super(context);
    }

    public CurvedClockView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mArc = new Path();
        mArc.addCircle(180, 150, 140, Path.Direction.CW);

        mPaintText = new Paint();
        mPaintText.setColor(Color.WHITE);
        mPaintText.setTextSize(20f);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawTextOnPath(QUOTE, mArc, 600, 20, mPaintText);
        invalidate();
    }
}

