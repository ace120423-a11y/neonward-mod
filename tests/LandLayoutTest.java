package jp.neonward;
public final class LandLayoutTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){for(int p=0;p<8;p++){int x=LandLayout.x(p),z=LandLayout.z(p);check(LandLayout.plot(x,z)==p);check(LandLayout.plot(x+31,z+31)==p);check(LandLayout.plot(x+32,z)!=p);check(LandLayout.editable(p,x,48,z));check(LandLayout.editable(p,x,127,z));check(!LandLayout.editable(p,x,47,z));check(!LandLayout.editable(p,x,128,z));for(int q=0;q<8;q++)if(q!=p)check(!LandLayout.editable(q,x,65,z));}check(LandLayout.plot(-120,154)==-1);check(!LandLayout.area(-32,154));check(LandLayout.area(-33,154));System.out.println("LAND_LAYOUT_PASS: 8 disjoint plots, negative coordinates, vertical and street boundaries");}
}
