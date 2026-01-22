package mb.fw.transformation.tool;

public class LittleEndianByteHandler{

/**
* short type을 byte로 변형
**/
public static final byte[] shortToByte(short shortVar){
byte littleShort[] = new byte[2];
littleShort[0] = (byte)((shortVar>>0) & 0xff);
littleShort[1] = (byte)((shortVar>>8) & 0xff);
return littleShort;
}

/**
* int type을 byte로 변형
**/
public static final byte[] intToByte(int intVar){
 byte littleInt[] = new byte[4];
littleInt[0] = (byte)((intVar>>0) & 0xff);
littleInt[1] = (byte)((intVar>>8) & 0xff);
littleInt[2] = (byte)((intVar>>16) & 0xff);
littleInt[3] = (byte)((intVar>>24) & 0xff);
return littleInt;
}

/**
* int type을 byte로 변형
**/
public static final byte[] intToByte2(int intVar){
byte littleInt[] = new byte[2];
littleInt[0] = (byte)((intVar>>0) & 0xff);
littleInt[1] = (byte)((intVar>>8) & 0xff);
return littleInt;
}

/**
* long type을 byte로 변형
**/ 
public static final byte[] longToByte(long longVar){
byte littleLong[] = new byte[8]; 
littleLong[0] = (byte)((longVar>>0) & 0xff);
littleLong[1] = (byte)((longVar>>8) & 0xff);
littleLong[2] = (byte)((longVar>>16) & 0xff);
littleLong[3] = (byte)((longVar>>24) & 0xff);
littleLong[4] = (byte)((longVar>>32) & 0xff);
littleLong[5] = (byte)((longVar>>40) & 0xff);
littleLong[6] = (byte)((longVar>>48) & 0xff);
littleLong[7] = (byte)((longVar>>56) & 0xff); 
return littleLong;
}

/**
* double type을 byte로 변형
**/ 
public static final byte[] doubleToByte(double doubleVar){
long temp; 
byte littleDouble[] = new byte[8];
temp = Double.doubleToLongBits(doubleVar);
littleDouble = longToByte(temp); 
return littleDouble;
}

/**
* byte 배열을 short 형으로 변환한다.
**/
public static final short byteToShort(byte[] buffer){
return byteToShort(buffer, 0);
}

public static final short byteToShort(byte[] buffer, int offset){ 
return (short) ( (buffer[offset+1]&0xff)<<8 | (buffer[offset]&0xff) );
}


public static final long byteToInt48( byte[] src) {
	return  ((src[5]&0xff) << 40) | 
			((src[4]&0xff) << 32) | 
			((src[3]&0xff) << 24) | 
			((src[2]&0xff) << 16) |	
			((src[1]&0xff) << 8) | 
			 (src[0]&0xff) ;
}

/**
* byte 배열을 int 형으로 변환한다.
**/
public static final int byte1ToInt(byte b){
return (int)(b&0xff);
} 
public static final int byte1ToInt(byte[] b, int offset){
return (int)(b[offset]&0xff);
}
public static final int byte2ToInt(byte[] buffer, int offset){
return (buffer[offset+1]&0xff)<<8 | (buffer[offset]&0xff);
}
public static final int byteToInt(byte[] buffer){
return byteToInt(buffer, 0);
}
public static final int byteToInt(byte[] buffer, int offset){ 
return (buffer[offset+3]&0xff)<<24 | (buffer[offset+2]&0xff)<<16 | (buffer[offset+1]&0xff)<<8 | (buffer[offset]&0xff);
}

/**
* byte 배열을 long 형으로 변환한다.
**/
public static final long byteToLong(byte[] buffer){ 
return byteToLong(buffer, 0);
}

public static final long byteToLong(byte[] buffer, int offset){
return ((long)byteToInt(buffer, offset+4) <<32) | ((long) byteToInt(buffer,offset) & 0xffffffffL );
}

/**
* byte 배열을 double 형으로 변환한다.
**/
public static final double byteToDouble(byte[] buffer, int offset){
long temp = byteToLong(buffer, offset);
return Double.longBitsToDouble(temp);
} 
public static final double byteToDouble(byte[] buffer){
long temp = byteToLong(buffer,0);
return Double.longBitsToDouble(temp);
}


public static void main(String[] args) {
	
}
}
