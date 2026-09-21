package jp.neonward;
public final class TelevisionRangeTest {
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
 public static void main(String[] args){
  check(TelevisionRoomPolicy.portableRange(80,74,240,80.5,74.5,240.5),"at TV");
  check(TelevisionRoomPolicy.portableRange(80,74,240,90.5,74.5,240.5),"exactly ten");
  check(!TelevisionRoomPolicy.portableRange(80,74,240,90.501,74.5,240.5),"beyond ten");
  check(TelevisionRoomPolicy.portableRange(80,74,240,86.5,74.5,248.5),"diagonal ten");
  check(!TelevisionRoomPolicy.portableRange(80,74,240,88.5,74.5,248.5),"square corners excluded");
  check(!TelevisionRoomPolicy.portableRange(80,74,240,80.5,84.501,240.5),"vertical distance included");
  check(TelevisionRoomPolicy.apartment(80,75,240,80.5,75,240.5),"same floor");
  check(!TelevisionRoomPolicy.apartment(80,75,240,80.5,83,240.5),"other floor rejected even within ten");
  check(!TelevisionRoomPolicy.apartment(80,75,240,65,75,240),"outside building rejected");
  check(!TelevisionRoomPolicy.portableRange(0,0,0,Double.NaN,0,0),"invalid coordinates fail closed");
  System.out.println("TV_RANGE_QA_PASS: 10-block sphere, boundary, vertical distance, existing floor restriction");
 }
}
