package xserver.io;

import java.nio.ByteBuffer;

public class DataBuffer{
   private byte[] data;
   private int size;
   
   public byte[] data(){
      return data;
   }
   
   public int size(){
      return size;
   }
   
   public void setSize(int size){
      ensureCapacity(size);
      this.size=size;
   }
   
   public byte get(int off){
      checkRead(off, 1);
      return data[off];
   }
   
   public int get(int off, byte[] dst, int dstOff, int len){
      checkRead(off, len);
      len=Math.min(len,size-off);
      System.arraycopy(data,off,dst,dstOff,len);
      return len;
   }
   
   public void append(byte v){
      put(size, v);
   }
   
   public int append(byte[] src){
      return append(src, 0, src.length);
   }
   
   public int append(byte[] src, int off, int len){
      return put(size, src, off, len);
   }
   
   public int append(ByteBuffer src, int off, int len){
      return put(size, src, off, len);
   }
   
   private void put(int off, byte v){
      checkWrite(off, 1);
      data[off]=v;
   }
   
   public int put(int off, byte[] src){
      return put(off, src, 0, src.length);
   }
   
   public int put(int off, byte[] src, int srcOff, int len){
      checkWrite(off, len);
      System.arraycopy(src,srcOff,data,off,len);
      return len;
   }
   
   public int put(int off, ByteBuffer src, int srcOff, int len){
      checkWrite(off, len);
      src.position(srcOff);
      src.get(data,off, len);
      return len;
   }
   
   protected void checkRead(int off, int len){
      if(size<(off+len)) throw new ArrayIndexOutOfBoundsException(off+len);
   }
   
   protected void checkWrite(int off, int len){
      if(off>size) throw new ArrayIndexOutOfBoundsException(off+">"+size);
      ensureCapacity(off+len);
      size=Math.max(size, off+len);
   }
   
   private void ensureCapacity(int n){
      if(data==null){
         data=new byte[n*3/2+10];
         return;
      }
      if(data.length>=n) return;
      byte[] newdata=new byte[n*3/2+10];
      System.arraycopy(data,0,newdata,0,size);
      data=newdata;
      return;
   }
   
   public String toString(){
      StringBuilder sb=new StringBuilder("[");
      for(int i=0; i<size; i++){
         if(sb.length()>1) sb.append(", ");
         sb.append(data[i]&0xff);
      }
      return sb.append("]").toString();
   }
   
   public static void main(String[] args){
      DataBuffer b=new DataBuffer();
      b.append(new byte[]{1,2,3,4,5});
      b.append(new byte[]{6,7,8,9,10});
      System.out.println(b); //[1,2,3,4,5,6,7,8,9,10]
      b.setSize(0);
      System.out.println(b); //[]
      b.put(0, new byte[]{1,2,3,4,5});
      b.put(3, new byte[]{1,2,3,4,5});
      System.out.println(b); //[1,2,3,1,2,3,4,5]
   }
}
