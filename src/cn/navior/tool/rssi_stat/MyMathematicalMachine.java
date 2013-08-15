package cn.navior.tool.rssi_stat;

import java.util.ArrayList;
import java.util.Iterator;

public class MyMathematicalMachine {
	
	public static double getArithmeticAverage( ArrayList< Integer > list ){
		double result = 0.0;
		// get int out of list
		Iterator< Integer > iterator = list.iterator();
		while( iterator.hasNext() ){
			int item = iterator.next().intValue();
			result += item;
		}
		// calculate average
		result = result / list.size();
		return result;
	}
	
	public static double getDoubleArithmeticAverage( ArrayList< Double > list ){
		double result = 0.0;
		// get double out of list
		Iterator< Double > iterator = list.iterator();
		while( iterator.hasNext() ){
			double item = iterator.next().doubleValue();
			result += item;
		}
		// calculate average
		result = result / list.size();
		return result;
	}
	
	/**
	 * Warning: if list.size() == 0, it'll meet DividedByZero Exception.
	 * @param list
	 * @return
	 */
	public static double getStandardDeviation( ArrayList< Integer > list ){
		// get average of the values
		double average = getArithmeticAverage( list );
		double result = 0.0;
		// get int out of list
		Iterator< Integer > iterator = list.iterator();
		while( iterator.hasNext() ){
			int item = iterator.next();
			result += ( item - average ) * ( item - average );
		}
		// warning: divided by zero
		result = result / ( list.size() - 1 );
		result = Math.sqrt( result );
		return result;
	}
	
	/**
	 * Calculate the geometrical average value of a list of integer values.
	 * @param list
	 * @return
	 */
	public static double getIntegerGeometricalAverage( ArrayList< Integer > list ){
		double result = 1.0;
		for( int i = 0; i < list.size(); i++ ){
			result *= list.get( i );
		}
		result = Math.pow( result, 1.0 / list.size() );
		return result;
	}
	
	/**
	 * Calculator the factors in linear regression function.
	 * @param list
	 * @return
	 */
	public LRFPair getLRFPair( ArrayList< LRVPair > list ){
		// calculate average of x
		ArrayList< Double > xDoubleList = new ArrayList< Double >();
		for( int i = 0; i < list.size(); i++ ){
			xDoubleList.add( list.get( i ).getX() );
		}
		double xAverage = getDoubleArithmeticAverage( xDoubleList );
		// calculate average of y
		ArrayList< Double > yDoubleList = new ArrayList< Double >();
		for( int i = 0; i < list.size(); i++ ){
			yDoubleList.add( list.get( i ).getY() );
		}
		double yAverage = getDoubleArithmeticAverage( yDoubleList );
		
		// sigma (xy)
		double sumXY = 0.0;
		for( int i = 0; i < list.size(); i++ ){
			sumXY += list.get( i ).getX() * list.get( i ).getY();
		}
		// n(xy)
		double nXY = list.size() * xAverage * yAverage;
		// sigma (xx)
		double sumXX = 0.0;
		for( int i = 0; i < list.size(); i++ ){
			sumXX += list.get( i ).getX() * list.get( i ).getX();
		}
		// n(xx)
		double nXX = list.size() * xAverage * xAverage;
		
		// get b value
		double b = ( sumXY - nXY ) / ( sumXX - nXX );
		double a = yAverage - b * xAverage;
		
		return new LRFPair( a, b );
	}
	
	/**
	 * The pair of the ( x, y ) values in linear regression function.
	 * x stands for variable and y stands for measured value.
	 * @author wxy
	 *
	 */
	static class LRVPair{
		
		private double x;	// variable x
		private double y;	// measured value y
		
		LRVPair( double x, double y ){
			this.setX( x );
			this.setY( y );
		}

		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
			this.y = y;
		}
	}
	
	/**
	 * The pair of the ( a, b ) factors in linear regression function.
	 * a stands for additional value of bx and b stands for factor of x.
	 * Formula: y = bx + a
	 * @author wxy
	 *
	 */
	static class LRFPair{
		
		private double a;	// value 'a' in formula
		private double b;	// factor 'b' in formula
		
		LRFPair( double a, double b ){
			this.setA( a );
			this.setB( b );
		}
		
		public double getA() {
			return a;
		}
		public void setA(double a) {
			this.a = a;
		}
		public double getB() {
			return b;
		}
		public void setB(double b) {
			this.b = b;
		}
		
		@Override
		public boolean equals( Object o ){
			if( o instanceof LRFPair ){
				return Math.abs( ( ( ( LRFPair )o ).getA() - this.a ) ) < 0.001
						&& Math.abs( ( ( ( LRFPair )o ).getB() - this.b ) ) < 0.001;
			}
			return false;
		}
		
		@Override
		public String toString(){
			return "Pair: a = " + this.a + " b = " + this.b; 
		}
	}
}
