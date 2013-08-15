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

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class DistributionGraph extends View {

  private final static int BASE_X = 20;
  private final static int BASE_Y = 20;
  private final static int MARGIN_X = 50;
  private final static int GAP = 20;
  private final static int WIDTH = 50;
  private int gap;
  private int width;
  private int amplifier;

  private int max_pixel_x;

  private HashMap< Integer, Integer > distribution;

  DistributionGraph( Context context, HashMap< Integer, Integer > distribution, int max_pixel_x ) {
    super(context);

    gap = 20;
    width = 50;
    amplifier = 20;

    this.distribution = distribution;
    this.max_pixel_x = max_pixel_x;
  }

  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    gap = GAP;
    width = WIDTH;
    // draw the graph
    TreeSet<Integer> keys = new TreeSet<Integer>( distribution.keySet() );
    if ( keys.size() > 0 ) {
      // prepare the attributes
      Paint painter = new Paint();
      int strongestValue = keys.pollLast();
      keys.add( strongestValue );
      int weakestValue = keys.pollFirst();
      keys.add( weakestValue );
      // adjust the height
      int maxAmount = 0;
      Iterator<Integer> iterator = keys.descendingIterator();
      while (iterator.hasNext()) {
        int rssiValue = iterator.next();
        if ( distribution.get(rssiValue) > maxAmount ) {
          maxAmount = distribution.get(rssiValue);
        }
      }
      this.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (maxAmount + 1) * amplifier));
      // adjust gap and width
      while ( ( gap + width ) * ( strongestValue - weakestValue + 1 ) + 2 * MARGIN_X >= max_pixel_x ) {
        gap /= 2;
        width /= 2;
      }
      // draw the ruler
      canvas.drawLine(BASE_X, BASE_Y, max_pixel_x - BASE_X, BASE_Y, painter);
      // from the strongest to weakest
      for (int i = 0; i < strongestValue - weakestValue + 1; i++) {
        canvas.drawText("" + (strongestValue - i), BASE_X + i * (width + gap) + MARGIN_X, BASE_Y, painter); // draw the marker
        Integer size = distribution.get( strongestValue - i );
        if ( size != null) {
          canvas.drawRect(BASE_X + MARGIN_X + i * (width + gap), BASE_Y, BASE_X + MARGIN_X + i * (width + gap) + width, BASE_Y + size * amplifier, painter);
        }
      }
    }
  }
}