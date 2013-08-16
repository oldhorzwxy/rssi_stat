package cn.navior.tool.rssi_stat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.samsung.android.sdk.bt.gatt.BluetoothGatt;
import com.samsung.android.sdk.bt.gatt.BluetoothGattAdapter;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

  // constants defined by wangxiayang
  private static final int REQUEST_ENABLE_BT = 36287;
  private static final int MAX_PIXEL_X = 1080;  // samsung galaxy s4's configuration
  private static final int MAX_PIXEL_Y = 1920;  // samsung galaxy s4's configuration
  // components
  private TextView searchingStatus;
  private EditText distanceInput;
  private EditText clockInput;
  private Button stopButton;
  private Button startButton;
  private Button enableClockButton;
  private Button quitButton;
  private Button clearButton;
  private Spinner spinner;
  private TotalTable totalTable;
  // status
  private boolean clockEnabled;
  // tools
  private Thread clock;
  private PrintWriter statWriter;
  private Handler handler;
  private HashMap< String, ArrayList< RecordItem > > recordMap;
  private HashMap< String, HashMap< Integer, Integer > > rssiMap;
  private HashMap< String, StatTable > tableMap;
  private HashMap< String, Integer > averageMap;
  private List< String > deviceList;
  private List< String > spinnerItemList;
  private int searchid;
  // fields about local Bluetooth device model
  private BluetoothAdapter mBluetoothAdapter; // local Bluetooth device model
  private BluetoothGatt mBluetoothGatt = null;    // local Bluetooth BLE device model
  private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {
    @Override
    public void onScanResult(final android.bluetooth.BluetoothDevice device, final int rssi, byte[] scanRecord) {
      // only deal with certain device
      if ( deviceList.contains( device.getName() )) {
        // initialize the record
        RecordItem item = new RecordItem(device.getAddress());
        item.setRssi(rssi);  // replace the short value into an integer
        item.setName(device.getName());
        item.setDistance(Integer.parseInt(distanceInput.getEditableText().toString()));
        SimpleDateFormat tempDate = new SimpleDateFormat("kk-mm-ss-SS", Locale.ENGLISH);
        String datetime = tempDate.format(new java.util.Date());
        item.setDatetime(datetime);
        // put into HashMap
        recordMap.get( device.getName() ).add( item );
        if( rssiMap.get( device.getName() ).containsKey( rssi ) ) {
          rssiMap.get( device.getName() ).put( rssi, rssiMap.get( device.getName() ).get( rssi ) + 1 );
        }
        else {
          rssiMap.get( device.getName() ).put( rssi, 1 );
        }
      }
    }
  };  // message handler
  private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      if (profile == BluetoothGattAdapter.GATT) {
        mBluetoothGatt = (BluetoothGatt) proxy;
        mBluetoothGatt.registerApp(mGattCallbacks);
      }
    }

    @Override
    public void onServiceDisconnected(int profile) {
      if (profile == BluetoothGattAdapter.GATT) {
        if (mBluetoothGatt != null)
          mBluetoothGatt.unregisterApp();

        mBluetoothGatt = null;
      }
    }
  };  // device model builder

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // initialize the fields
    searchid = 0;
    searchingStatus = (TextView) findViewById(R.id.searching_status);
    distanceInput = (EditText) findViewById(R.id.input_distence);
    clockInput = (EditText) findViewById(R.id.input_clock);
    stopButton = (Button) findViewById(R.id.searching_stop);
    startButton = (Button) findViewById(R.id.searching_start);
    enableClockButton = (Button) findViewById(R.id.searching_enable_clock);
    quitButton = (Button) findViewById(R.id.searching_quit);
    clockEnabled = false;
    handler = new Handler();
    spinner = ( Spinner ) findViewById( R.id.spinner );
    recordMap = new HashMap<String, ArrayList<RecordItem>>();
    rssiMap = new HashMap<String, HashMap<Integer, Integer>>();
    deviceList = new ArrayList<String>();
    tableMap = new HashMap<String, StatTable>();
    averageMap = new HashMap<String, Integer>();
    clearButton = ( Button )findViewById( R.id.searching_clear );
    spinnerItemList = new ArrayList<String>();
    totalTable = new TotalTable( this, averageMap );

    spinnerItemList.add( "total" );

    // initialize the maps
    for( int i = 22; i <= 31; i++ ) {
      if( i == 28 ) {
        continue;
      }
      String s = "876543" + i;
      deviceList.add( s );
      tableMap.put( s, new StatTable( this ) );
      recordMap.put( s, new ArrayList<RecordItem>() );
      rssiMap.put( s, new HashMap<Integer, Integer>() );
      spinnerItemList.add( s );
      averageMap.put( s, 0 );
    }



    // build up local Bluetooth device model
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();  // get the Bluetooth adapter for this device
    if (mBluetoothGatt == null) {
      BluetoothGattAdapter.getProfileProxy(this, mProfileServiceListener, BluetoothGattAdapter.GATT);
    }

    // bind the action listeners
    // stop-searching button
    stopButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        // create the time string
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.ENGLISH);
        String datetime = tempDate.format(new java.util.Date());
        // close statistic writer
        statWriter.write("stop time,"+ datetime);
        statWriter.close();
        if (clock != null) {
          clock.interrupt();
          clock = null;
        }
        onStopScan();

        // enable the buttons
        stopButton.setEnabled(false);  // only disable stop button
        startButton.setEnabled(true);
        enableClockButton.setEnabled(true);
        distanceInput.setEnabled(true);
        if (clockEnabled) {
          clockInput.setEnabled(true);
        }

        // check if there is a clock thread running
        searchingStatus.setText("Discovery has finished.");
      }
    });
    stopButton.setEnabled(false);  // disable the stop button until discovery starts
    // start-searching button
    startButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        // record the statistic results into a file
        // create directory
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "rssi_stat");
        if (!directory.exists()) {
          directory.mkdir();
        }

        // create the time string
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.ENGLISH);
        String datetime = tempDate.format(new java.util.Date());

        // create the file
        File recordFile = new File(directory.getAbsolutePath() + "/" + datetime + ".txt");
        if (recordFile.exists()) {
          recordFile.delete();
        }

        // open writer
        try {
          statWriter = new PrintWriter(recordFile);
          statWriter.write("Device name," + mBluetoothAdapter.getName() + "\n");
          statWriter.write("Device address," + mBluetoothAdapter.getAddress() + "\n");
          statWriter.write("Discovered device distance," + distanceInput.getEditableText().toString());
          statWriter.write("clock," + clockInput.getEditableText().toString());
          statWriter.write("start," + datetime + "\n");
          statWriter.write("name,id,ave,2pk,ave2pk,str3,ave3,middle\n");
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }

        onStartScan();
      }
    });
    // enable-clock button
    enableClockButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // if clock has been enabled, disable it
        if (clockEnabled) {
          clockEnabled = false;
          enableClockButton.setText("enable clock");
          clockInput.setEnabled(false);
        } else {
          clockEnabled = true;
          enableClockButton.setText("disable clock");
          clockInput.setEnabled(true);
        }
      }
    });
    clearButton.setOnClickListener( new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Iterator< StatTable > iterator = tableMap.values().iterator();
        while( iterator.hasNext() ) {
          iterator.next().clearRows();
        }
      }
    });
    // quit-searching button
    quitButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        MainActivity.this.finish();
      }
    });

    ArrayAdapter< String > spinnerAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item, spinnerItemList );
    spinner.setAdapter( spinnerAdapter );
    spinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {

        LinearLayout board = (LinearLayout) findViewById(R.id.searching_graph_board);
        if( board.getChildCount() != 0 ) {
          ( ( LinearLayout )( board.getChildAt(0) ) ).removeAllViews();
        }
        board.removeAllViews();

        String item = ( String )spinner.getItemAtPosition( pos );
        if( item.equals( "total" ) ) {
          board.addView( totalTable );
        }
        else {
          board.addView( getGraphBoard( recordMap.get( item ), rssiMap.get( item ), tableMap.get( item ) ) );
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {
        //TODO
      }
    });
  }

  private LinearLayout getGraphBoard( ArrayList< RecordItem > records, HashMap< Integer, Integer > distribution, StatTable table ) {
    LinearLayout result = new LinearLayout( this );
    result.setOrientation( LinearLayout.VERTICAL );
    result.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
    //WaveGraph waveGraph = new WaveGraph( this, records, MAX_PIXEL_X );
    //result.addView( waveGraph );
    //DistributionGraph distributionGraph = new DistributionGraph( this, distribution, MAX_PIXEL_X );
    //result.addView( distributionGraph );
    result.addView( table );
    return result;
  }

  @Override
  protected void onResume() {
    super.onResume();
    // check Bluetooth status, notify user to turn it on if it's not
    // repeat requesting if user refused to open Bluetooth. It's ugly now but no matter.
    while (!mBluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    BluetoothGattAdapter.closeProfileProxy(BluetoothGattAdapter.GATT, mBluetoothGatt);
  }

  /**
   * logic on starting scanning
   */
  private void onStartScan() {
    searchid++;
    // clear the storage
    for( int i = 0; i < deviceList.size(); i++ ) {
      String name = deviceList.get( i );
      recordMap.get( name ).clear();
      rssiMap.get( name ).clear();
      tableMap.get( name ).postInvalidate();
    }
    // check input validity
    if (!checkInput()) {
      // notify user if invalid
      Toast.makeText(MainActivity.this, "sth wrong in your input", Toast.LENGTH_SHORT).show();
      return;
    }
    // start only if distance and device name are set
    mBluetoothGatt.startScan();
    // if clock is enabled, start clock
    if (clockEnabled) {
      clock = new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(Integer.parseInt(clockInput.getEditableText().toString()));
            handler.post(new Runnable() {
              @Override
              public void run() {
                // stop scan if time is up
                onStopScan();
              }
            });
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      };
      clock.start();

    }
    // present the distance and name on status bar
    searchingStatus.setText("on discovering");
    // disable the buttons and input area
    stopButton.setEnabled(true);  // only enable stop button
    startButton.setEnabled(false);
    enableClockButton.setEnabled(false);
    distanceInput.setEnabled(false);
    clockInput.setEnabled(false);
  }

  /**
   * check input validity
   *
   * @return
   */
  private boolean checkInput() {
    // check distance input
    if (distanceInput.getEditableText() == null
        || distanceInput.getEditableText().toString().equals("")) {
      return false;
    } else {
      try {
        Integer.parseInt(distanceInput.getEditableText().toString());
      } catch (NumberFormatException e) {
        return false;
      }
    }
    // check clock input
    if (clockEnabled) {
      if (clockInput.getEditableText() == null
          || clockInput.getEditableText().toString().equals("")) {
        return false;
      } else {
        try {
          Integer.parseInt(clockInput.getEditableText().toString());
        } catch (NumberFormatException e) {
          return false;
        }
      }
    }

    // all pass
    return true;
  }

  /**
   * logic on stop scanning
   */
  private void onStopScan() {
    mBluetoothGatt.stopScan();
    Iterator< String > iterator = recordMap.keySet().iterator();
    System.out.println( recordMap.keySet().size() );
    while( iterator.hasNext() ) {
      String name = iterator.next();
      doStat( rssiMap.get( name ), tableMap.get( name ), name, averageMap, searchid );
    }
    if( clock != null ) {
      onStartScan();
    }
  }

  private void doStat( HashMap< Integer, Integer > distribution, StatTable table, String name, HashMap< String, Integer > averageMap, int searchid ) {
    // get first half
    /*Iterator< ArrayList< RecordItem > > iterator = recordsOnGraph.values().iterator();
    int size = 0;
    while( iterator.hasNext() ) {
      size += iterator.next().size();
    }
    HashMap< Integer, ArrayList< RecordItem > > half = new HashMap<Integer, ArrayList<RecordItem> >();
    TreeSet<Integer> keys2 = new TreeSet<Integer>(recordsOnGraph.keySet());
    int max = keys2.pollLast();
    int halfValue = 0;
    while( half.values().size() + recordsOnGraph.get( max ).size() <= size / 2 ) {
      half.put( max, recordsOnGraph.get( max ) );
      halfValue += recordsOnGraph.get( max ).size();
      if( keys2.size() == 0 ) {
        break;
      }
      max = keys2.pollLast();
    }
    recordsOnGraph = half;*/

    // get first and last value
    TreeSet<Integer> keys = new TreeSet<Integer>(distribution.keySet());
    if (keys.size() >= 2) {
      final int firstValue = keys.pollFirst();
      keys.add(firstValue);
      final int lastValue = keys.pollLast();
      keys.add(lastValue);
      // get distribution array
      ArrayList<Integer> sizeArray = new ArrayList<Integer>(lastValue - firstValue + 3);  // two items bigger than the value field
      for (int i = firstValue - 1; i <= lastValue + 1; i++) {
        if (distribution.containsKey( i )) {
          sizeArray.add( distribution.get( i ) );
        } else {
          sizeArray.add(0); // if it's not in the distribution, it's size should be zero
        }
      }
      // get total record size
      //final int recordNum = tempRecords.size();
      // get overall average
      ArrayList<Integer> rssiList = new ArrayList<Integer>();
      Iterator< Integer > distIterator = distribution.keySet().iterator();
      while( distIterator.hasNext() ) {
        int rssi = distIterator.next();
        for( int i = 0; i < distribution.get( rssi ); i++ ) {
          rssiList.add( rssi );
        }
      }
      final int average = (int) MyMathematicalMachine.getArithmeticAverage(rssiList);
      // get the standard derivation
      //double sd = MyMathematicalMachine.getStandardDeviation(rssiList);
      //sd = ((int) (sd * 100)) / 100.0;
      // get two peaks
      int firstPeak = 0;  // the weakest peak
      for (int i = 1; i <= lastValue - firstValue + 1; i++) {
        if (sizeArray.get(i) >= sizeArray.get(i - 1)
            && sizeArray.get(i) > sizeArray.get(i + 1)) {
          firstPeak = firstValue + (i - 1);
          break;
        }
      }
      int lastPeak = 0;  // the strongest peak
      for (int i = lastValue - firstValue + 1; i >= 1; i--) {
        if (sizeArray.get(i) >= sizeArray.get(i + 1)
              && sizeArray.get(i) > sizeArray.get(i - 1)) {
          lastPeak = firstValue + (i - 1);  // yes, it's right, using firstValue to compute lastPeak
          break;
        }
      }
      // there is a special condition: the peaks group into one
      if (firstPeak > lastPeak) {
        int temp = firstPeak;
        firstPeak = lastPeak;
        lastPeak = temp;
      }
      // get the third peak
      int thirdPeak = lastPeak; // the third peak will be no later than last peak
      if (firstPeak != lastPeak) {
        for (int i = firstPeak - firstValue + 2; i <= lastPeak - firstValue; i++) {
          if (sizeArray.get(i) >= sizeArray.get(i - 1)
              && sizeArray.get(i) > sizeArray.get(i + 1)) {
            thirdPeak = firstValue + (i - 1);
            break;
          }
        }
      }
      // get first three strongest
      int firstStrongest = 0;
      int secondStrongest = 0;
      int thirdStrongest = 0;
      int firstSIndex = 0;
      int secondSIndex = 0;
      int thirdSIndex = 0;
      for (int i = 1; i <= lastValue - firstValue + 1; i++) {
        // if found the strongest
        if (sizeArray.get(i) >= firstStrongest) {
          thirdStrongest = secondStrongest;
          secondStrongest = firstStrongest;
          firstStrongest = sizeArray.get(i);

          thirdSIndex = secondSIndex;
          secondSIndex = firstSIndex;
          firstSIndex = i;
        }
        // if found the second
        else if (sizeArray.get(i) >= secondStrongest) {
          thirdStrongest = secondStrongest;
          secondStrongest = sizeArray.get(i);

          thirdSIndex = secondSIndex;
          secondSIndex = i;
        }
        // if found the third
        else if (sizeArray.get(i) >= thirdStrongest) {
          thirdStrongest = sizeArray.get(i);
          thirdSIndex = i;
        }
      }
      firstStrongest = firstValue + firstSIndex - 1;
      secondStrongest = firstValue + secondSIndex - 1;
      thirdStrongest = firstValue + thirdSIndex - 1;
      // get average for first two peaks
      int ave2 = (lastPeak + thirdPeak) / 2;
      // get average for two strongest of the three most
      int ave3 = 0;
      if( firstStrongest <= secondStrongest && firstStrongest <= thirdStrongest ) {
        ave3 = ( secondStrongest + thirdStrongest ) / 2;
      }
      else if( secondStrongest <= firstStrongest && secondStrongest <= thirdStrongest ) {
        ave3 = ( firstStrongest + thirdStrongest ) / 2;
      }
      else {
        ave3 = ( firstStrongest + secondStrongest ) / 2;
      }
      // get the middle of the keys
      Iterator< Integer > iterator = keys.descendingIterator();
      int middle = 0;
      if( keys.size() % 2 != 0 ) {
        for( int i = 0; i < keys.size() / 2; i++ ) {
          iterator.next();
        }
        middle = iterator.next();
      }
      else {
        for( int i = 0; i < keys.size() / 2 - 1; i++ ) {
          iterator.next();
        }
        middle = ( iterator.next() + iterator.next() ) / 2;
      }
      // add the data into the table
      StatTableRow row = new StatTableRow( table.getContext() );
      row.addBlock( searchid + "" );
      row.addBlock( average + "" );
      row.addBlock( lastPeak + ";" + thirdPeak + "" );
      row.addBlock( ave2 + "" );
      row.addBlock( firstStrongest + ";" + secondStrongest + ";" + thirdStrongest + "" );
      row.addBlock( ave3 + "" );
      row.addBlock( middle + "" );

      table.addView( row );

      // do total stat
      averageMap.put( name, ( averageMap.get( name ) * ( searchid - 1 ) + average ) / searchid );
      totalTable.updateTable( averageMap );

      statWriter.write( name + "," + searchid + "," + average + "," + lastPeak + ";" + thirdPeak + "," + ave2 + "," + firstStrongest + ";" + secondStrongest
          + ";" + thirdStrongest + "," + ave3 + "," + middle + "\n" );
    }

    /*resultStat.post( new Runnable() {
      @Override
      public void run() {
        TableRow row = new TableRow( SingleSearchingActivity.this );
        TextView t = new TextView( SingleSearchingActivity.this );
        t.setText( "RSSI    " );
        row.addView( t );
        t = new TextView( SingleSearchingActivity.this );
        t.setText( "count   " );
        row.addView( t );
        t = new TextView( SingleSearchingActivity.this );
        t.setText( "percentage" );
        row.addView( t );
        resultStat.addView( row );
      }
    } );

    resultStat.post( new Runnable() {
      @Override
      public void run() {
        TableRow row = new TableRow( SingleSearchingActivity.this );
        TextView t = new TextView( SingleSearchingActivity.this );
        t.setText( "" + average );
        row.addView(t);
        t = new TextView( SingleSearchingActivity.this );
        t.setText( "" + recordNum );
        row.addView(t);
        t = new TextView( SingleSearchingActivity.this );
        t.setText( "1" );
        row.addView( t );
        resultStat.addView( row );
      }
    } );

    // from strongest to weakest
    int times = keys.size();
    for( int i = 0 ; i < times; i++ ) {
      final int rssiValue = keys.pollLast();
      final int count = recordsOnGraph.get( rssiValue ).size();
      final double percentage = ( count + 0.0 ) / recordNum;
      resultStat.post( new Runnable() {
        @Override
        public void run() {
          TableRow row = new TableRow( SingleSearchingActivity.this );
          TextView t = new TextView( SingleSearchingActivity.this );
          t.setText( "" + rssiValue );
          row.addView( t );
          t = new TextView( SingleSearchingActivity.this );
          t.setText( "" + count );
          row.addView( t );
          t = new TextView( SingleSearchingActivity.this );
          t.setText( "" + ( int )( percentage * 100 ) + "%" );
          row.addView( t );
          resultStat.addView( row );
        }
      } );
    }*/
  }

  class StatTable extends TableLayout {

    StatTableRow titleBar;

    StatTable(Context context) {
      super(context);
      // set title bar
      titleBar = new StatTableRow(context);
      titleBar.addBlock("id\t\t");
      titleBar.addBlock("ave\t\t");
      titleBar.addBlock("2pk\t\t\t\t\t\t");
      titleBar.addBlock("ave2pk\t\t");
      titleBar.addBlock("3str\t\t\t\t\t\t");
      titleBar.addBlock("ave3str\t\t");
      titleBar.addBlock("middle\t\t");
      addView(titleBar);
    }

    void clearRows() {
      removeAllViews();
      addView( titleBar );
    }
  }

  class StatTableRow extends TableRow {
    StatTableRow( Context context ) {
      super( context );
    }

    private void addBlock( String s ) {
      TextView t = new TextView( getContext());
      t.setText(s);
      addView(t);
    }
  }

  class TotalTable extends TableLayout {
    StatTableRow titleBar;

    TotalTable( Context context, HashMap< String, Integer > aveMap ) {
      super( context );

      titleBar = new StatTableRow( context );
      titleBar.addBlock( "name" );
      titleBar.addBlock( "average" );

      TreeSet< String > keySet = new TreeSet<String>( aveMap.keySet() );
      while( keySet.size() > 0 ) {
        String name = keySet.pollLast();
        addRow( name, aveMap.get( name ) );
      }
    }

    void clearRows() {
      removeAllViews();
      addView( titleBar );
    }

    void addRow( String name, int average ) {
      StatTableRow row = new StatTableRow( getContext() );
      row.addBlock( name );
      row.addBlock( average + "" );
      addView( row );
    }

    void updateTable( HashMap< String, Integer > aveMap ) {
      clearRows();
      TreeSet< String > keySet = new TreeSet<String>( aveMap.keySet() );
      while( keySet.size() > 0 ) {
        String name = keySet.pollLast();
        addRow( name + "\t\t\t\t", aveMap.get( name ) );
      }
    }
  }
}
