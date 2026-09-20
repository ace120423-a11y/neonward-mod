package jp.neonward;
/** Five valve settings, with continuous angles across the 4 -> 0 wrap. */
final class HandwheelMotion {
 static final float STEP=(float)(Math.PI*2/5);
 static float advance(float current,int setting,long ticks){
  float turn=(float)(Math.PI*2),delta=setting*STEP-current;
  delta-=turn*(float)Math.floor((delta+Math.PI)/turn);
  if(Math.abs(delta)<.00001f)return current;
  float limit=.09f*Math.max(0,Math.min(4,ticks));
  return current+Math.max(-limit,Math.min(limit,delta));
 }
}
