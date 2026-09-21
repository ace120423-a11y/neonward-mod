package jp.neonward;
/** Independent western parcel geometry. All ownership checks use this exact boundary. */
public final class LandLayout {
 public static final int COUNT=8,PRICE=20000,ANIMAL_LIMIT=12,MIN_Y=48,MAX_Y=127;
 public static int x(int id){if(id<0||id>=COUNT)throw new IllegalArgumentException();return -232+(id%4)*44;}
 public static int z(int id){if(id<0||id>=COUNT)throw new IllegalArgumentException();return id<4?104:168;}
 public static boolean area(int x,int z){return x>=-248&&x<=-33&&z>=88&&z<=216;}
 public static int plot(int x,int z){for(int i=0;i<COUNT;i++)if(x>=x(i)&&x<x(i)+32&&z>=z(i)&&z<z(i)+32)return i;return -1;}
 public static boolean editable(int plot,int x,int y,int z){return plot>=0&&plot<COUNT&&y>=MIN_Y&&y<=MAX_Y&&plot(x,z)==plot;}
 public static boolean samePlot(int x,int y,int z,int X,int Y,int Z){int p=plot(x,z);return editable(p,x,y,z)&&editable(p,X,Y,Z);}
}
