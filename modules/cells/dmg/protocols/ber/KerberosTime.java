package dmg.protocols.ber ;

import java.util.* ;
import java.text.* ;

public class KerberosTime extends BerObject {
   private SimpleDateFormat __form =
       new SimpleDateFormat("yyyyMMddhhmmss'Z'" ) ;
   private Date _date = null ;
   public KerberosTime( ){
                        
       super( BerObject.UNIVERSAL , true , 24 ) ;
       _date = new Date()  ;
   }
   public KerberosTime( Date date ){
                        
       super( BerObject.UNIVERSAL , true , 24 ) ;
       _date = date ;
   }
   public KerberosTime( byte [] data , int off , int size ){
                        
       super( BerObject.UNIVERSAL , true , 24 , data ,off,size) ;
       String str = new String( data , off , size ) ;
       ParsePosition pos = new ParsePosition(0);
       _date = __form.parse( str , pos ) ;
   }
   public String toString(){ return _date.toString() ; }
   public String getTypeString(){
      return super.getTypeString()+" KerberosTime" ;
   }
   public byte [] getEncodedData(){
      byte [] original = getData() ;
      if( original != null ){
         return getEncodedData( original ) ;
      }else{
         String str = __form.format( _date ) ;
         return getEncodedData( str.getBytes() ) ;
      }
   }
}
