/**
 * ==============================BEGIN_COPYRIGHT===============================
 * ===================NAVIOR CO.,LTD. PROPRIETARY INFORMATION==================
 * This software is supplied under the terms of a license agreement or
 * nondisclosure agreement with NAVIOR CO.,LTD. and may not be copied or
 * disclosed except in accordance with the terms of that agreement.
 * ==========Copyright (c) 2003 NAVIOR CO.,LTD. All Rights Reserved.===========
 * ===============================END_COPYRIGHT================================
 *
 * @author wangxiayang
 * @date 15/08/13
 */
package cn.navior.tool.rssi_stat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class WaveGraph extends View {

  private final static int BASE_X = 20;
  private final static int BASE_Y = 20;
  private final static int MARGIN_X = 50;
  private final static int RULER_LENGTH = 20;
  private final static int GAP = 21;
  private final static int RADIUS = 10;
  private int gap;  // gap between two points on the graph
  private int radius; // radius of a point
  private int amplifier;
  private int max_pixel_x;

  private ArrayList< RecordItem > records;

  WaveGraph( Context context, ArrayList< RecordItem > records, int max_pixel_x ) {
    super(context);

    gap = GAP;
    radius = RADIUS;
    amplifier = 50;

    this.records = records;
    this.max_pixel_x = max_pixel_x;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    gap = GAP;
    radius = RADIUS;

    if ( records.size() > 0 ) {
      Paint painter = new Paint();
      // find the boundary values
      int weakestValue = records.get( 0 ).getRssi();  // start from the first item
      int strongestValue = records.get( 0 ).getRssi();  // start from the first item
      for( int i = 0; i < records.size(); i++ ) {
        final int rssiValue = records.get( i ).getRssi();
        if( rssiValue < weakestValue ) {
          weakestValue = rssiValue;
        }
        if( rssiValue > strongestValue ) {
          strongestValue = rssiValue;
        }
      }
      // adjust the height
      this.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (strongestValue - weakestValue + 2) * amplifier));
      // adjust the gap and radius
      while (gap * records.size() + MARGIN_X + RULER_LENGTH >= max_pixel_x) {
        gap--;
      }  // too wide for the graph
      radius = (gap + 1) / 2 - 1;
      // draw the vertical rulers
      // the stronger ones lie upwards
      for (int i = 0; i < strongestValue - weakestValue + 1; i++) {
        canvas.drawLine(BASE_X, BASE_Y + i * amplifier, max_pixel_x - BASE_X, BASE_Y + i * amplifier, painter);
        canvas.drawText("" + (strongestValue - i), BASE_X + 0.0f, BASE_Y + i * amplifier + 0.0f, painter);
      }
      // draw the horizontal rulers
      // one separator between every 10 points
      for (int i = 0; i < records.size(); i += 10) {
        canvas.drawLine(BASE_X + RULER_LENGTH + MARGIN_X + i * gap, BASE_Y, BASE_X + RULER_LENGTH + MARGIN_X + i * gap, BASE_Y + (strongestValue - weakestValue + 1) * amplifier, painter);
      }
      // draw the points
      for (int i = 0; i < records.size(); i++) {
        canvas.drawCircle(BASE_X + RULER_LENGTH + MARGIN_X + i * gap, BASE_Y + (-records.get(i).getRssi() + strongestValue) * amplifier, radius, painter);
      }
    }
  }

}