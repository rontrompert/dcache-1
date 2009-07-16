package dmg.cells.nucleus ;

import dmg.util.* ;
import java.util.* ;
import java.io.* ;



public class ClassLoaderProvider {

    private class _TreeNode {
        private Hashtable    _hash  = null ;
        private ClassDataProvider _entry = null ;
        private _TreeNode( ClassDataProvider entry ){
            _entry = entry ;
        }
        private _TreeNode(){}
        private void put( String key , _TreeNode value ){
            if( _hash == null )_hash = new Hashtable() ;
            _hash.put( key , value ) ;
        }
        private _TreeNode get( String key ){
            return _hash==null?null:(_TreeNode)_hash.get( key ) ;
        }
        private void setDefault( ClassDataProvider entry ){
            _entry = entry ;
        }
        private ClassDataProvider getDefault(){ return _entry ; }
        private Enumeration keys(){ return _hash==null?null:_hash.keys() ; }


    }
    //
    // set the default ( root ) to the system loader
    //
    private _TreeNode    _root  = new _TreeNode( new CDPDummy() ) ;


    public ClassLoaderProvider(){}
    public String [][] getProviders(){
        Vector v = new Vector() ;
        getProviders( v , "*" , _root ) ;
        String [][] rt = new String[v.size()][] ;
        v.copyInto( rt ) ;
        return rt ;
    }
    public void getProviders( Vector v , String name , _TreeNode cursor ){
        ClassDataProvider le = cursor.getDefault() ;
        String [] out = new String[2] ;
        out[0]  = name ;
        out[1]  = le==null?"none":le.toString() ;
        v.addElement( out ) ;
        Enumeration e = cursor.keys() ;
        if( e == null )return ;
        for( ; e.hasMoreElements() ; ){
            String    nodeName = (String)e.nextElement() ;
            _TreeNode node = cursor.get( nodeName ) ;
            getProviders( v , name+"."+nodeName , node ) ;
        }
    }
    public void display( ){
        display( "*" , _root ) ;
    }
    void display( String    name , _TreeNode cursor ){

        ClassDataProvider le = cursor.getDefault() ;
        System.out.println( " "+name+"    "+
                            (le==null?"Inherited":le.toString()) );
        Enumeration e = cursor.keys() ;
        if( e == null )return ;
        for( ; e.hasMoreElements() ; ){
            String    nodeName = (String)e.nextElement() ;
            _TreeNode node = cursor.get( nodeName ) ;
            display( name+"."+nodeName , node ) ;
        }

    }
    public void setDefault( ClassDataProvider defEntry ){
        _root.setDefault( defEntry ) ;
    }
    public void addEntry( String selection , ClassDataProvider provider ){

        StringTokenizer st = new StringTokenizer( selection , ".") ;
        _TreeNode cursor = _root ;
        for( ; st.hasMoreTokens() ; ){
            String token = st.nextToken() ;
            if( token.equals("*") )break ;
            _TreeNode rt = cursor.get( token ) ;
            if( rt == null ){
                rt = new _TreeNode() ;
                cursor.put( token , rt ) ;
            }
            cursor = rt ;
        }
        cursor.setDefault( provider ) ;
    }
    public void removeSystemProvider( String selection ){
        addEntry( selection , null ) ;
    }
    public void addSystemProvider( String selection ){
        addEntry( selection , new CDPDummy() ) ;
    }
    public void addFileProvider( String selection , File file ){
        addEntry( selection , new ClassDataProvider0( file ) ) ;
    }
    public void addCellProvider( String selection ,
                                 CellNucleus nucleus ,
                                 CellPath    cellPath           ){

        addEntry( selection , new ClassDataProvider0( nucleus , cellPath ) ) ;

    }
    ClassDataProvider getEntry( String className ){
        StringTokenizer    st = new StringTokenizer( className , ".") ;
        _TreeNode      cursor = _root ;
        ClassDataProvider def = _root.getDefault() ;
        for( ; st.hasMoreTokens() ; ){
            String token = st.nextToken() ;
            _TreeNode rt = cursor.get( token ) ;
            if( rt == null )return def ;
            ClassDataProvider tmp = rt.getDefault() ;
            def = tmp == null ? def : tmp ;
            cursor = rt ;
        }
        return def ;
    }
    public Class loadClass( String className ) throws ClassNotFoundException {
        ClassLoader loader = new ClassLoaderC( this ) ;
        return loader.loadClass( className ) ;
    }
    public static void main( String [] args ) throws Exception {
        if( args.length < 1 ){
            System.out.println( "USAGE : ... <selection> ... " ) ;
            System.exit(43);
        }
        ClassLoaderProvider clp = new ClassLoaderProvider() ;
        int i ;
        for(  i = 0 ; i < args.length ; i++ ){
            if( args[i].equals(".") )break ;
            System.out.println( "Loading .... "+args[i] ) ;
            StringTokenizer st = new StringTokenizer( args[i] ,":") ;
            String selection = st.nextToken() ;
            String provider  = st.nextToken() ;
            ClassDataProvider loader = null ;
            if( ! provider.equals( "system" ) ){
                loader =  new ClassDataProvider0( new File( provider) );
            }
            clp.addEntry( selection , loader ) ;
        }
        String [] [] out = clp.getProviders() ;
        for( int j = 0 ; j < out.length ; j++ ){
            System.out.println( " "+out[j][0]+" -> "+out[j][1] ) ;
            //      System.out.println( "Displaying .........................." ) ;
            //      clp.display() ;
            //      for( i++ ; i < args.length ; i++ ){
            //         System.out.println( "Class Loading .... "+args[i] ) ;
            //         Class entry = clp.loadClass( args[args.length-1]  ) ;
            //         System.out.println( "Found ............ "+entry ) ;
        }
    }
}
class CDPDummy implements ClassDataProvider {

    public byte [] getClassData( String className )
        throws IOException {
        return null ;
    }
    public String toString(){
        return "CDP=System" ;
    }
}
class ClassDataProvider0 implements ClassDataProvider {

    private CellNucleus _nucleus  = null ;
    private CellPath    _cellPath = null ;
    private File        _dir      = null ;
    private boolean     _useSystem = false ;

    ClassDataProvider0( CellNucleus nucleus , CellPath cellPath ){
        _nucleus  = nucleus ;
        _cellPath = cellPath ;
    }
    ClassDataProvider0( File dir ){
        _dir = dir ;
    }
    ClassDataProvider0(){
        _useSystem = true ;
    }
    public boolean useSystem(){ return _useSystem ; }
    public String toString(){
        if( _dir != null )
            return "CDP0;Directory="+_dir ;
        if( ( _nucleus != null ) && ( _cellPath != null ) )
            return "CDP0;Cell="+_nucleus.getCellName()+
                "CellPath="+_cellPath ;
        return "CDP0;PANIC" ;
    }
    public byte [] getClassData( String className )
        throws IOException {

        if( _dir != null ){
            className = className.replace( '.' , '/' )+".class" ;
            return loadClassDataFile( _dir , className ) ;
        }
        if( ( _nucleus != null ) && ( _cellPath != null ) ){

            return loadClassDataCell( className ) ;
        }
        throw new IOException( "PANIC in ClassDataProvider0" ) ;
    }
    private byte [] loadClassDataCell( String className )
        throws IOException {

        _nucleus.say( "getClassData("+className+") send to classProvider" ) ;
        CellMessage answer = null ;
        try{
            answer = _nucleus.sendAndWait(
                                          new CellMessage( _cellPath ,
                                                           "get class "+className
                                                           ) ,
                                          4000
                                           ) ;
        }catch( Exception e ){
            _nucleus.say( "getClassData Exception : "+e ) ;
            throw new IOException( e.toString() ) ;
        }
        if( answer == null ){
            _nucleus.say( "getClassData sendAndWait timed out" ) ;
            throw new IOException( "getClassData sendAndWait timed out" ) ;
        }
        Object answerObject = answer.getMessageObject() ;
        if( answerObject == null )
            throw new IOException( "PANIC Message didn't contain data" ) ;

        if( ! ( answerObject instanceof byte [] ) ){
            _nucleus.say( "getClassData sendAndWait got : "+
                          answerObject.toString() ) ;
            throw new IOException( "Unknown data arrived" ) ;
        }

        return (byte [] )answerObject ;

    }
    private byte [] loadClassDataFile( File dir , String name)
        throws IOException {

        System.out.println( "loadClassData : File="+name ) ;
        File file = new File( dir , name ) ;
        DataInputStream in = null ;
        try{
            long length = file.length() ;
            System.out.println( "loadClassData : length="+length ) ;
            if( length == 0 )
                throw new IOException( "Datafile has zero size" ) ;
            byte [] data = new byte[(int)length] ;
            in = new DataInputStream( new FileInputStream( file ) ) ;
            in.read( data ) ;
            in.close() ;

            return data ;

        }catch( IOException eee ){
            throw eee ;
        }finally{
            try{ in.close(); }catch(Exception sese){} ;
        }

    }

}
class ClassLoaderC extends ClassLoader {

    private ClassLoaderProvider _provider = null ;
    private static int __version = 0 ;

    ClassLoaderC( ClassLoaderProvider classLoaderProvider ){
        _provider = classLoaderProvider ;
        synchronized( getClass() ){
            __version++ ;
        }
    }
    public String toString(){
        return "CLC-"+__version ;
    }
    public synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException   {

        byte [] data = null ;
        try{

            ClassDataProvider dp = _provider.getEntry( name ) ;

            //           System.out.print( "Loading class V-"+__version+" "+name ) ;
            if( dp != null ){
                if( dp instanceof CDPDummy ){
                    //                  System.out.println( " throu <System>" ) ;
                    return Class.forName( name ) ;
                }else{
                    //                  System.out.println( " throu "+dp ) ;
                    data = dp.getClassData( name ) ;
                }
            }else{
                //               System.out.println( " Failed" ) ;
                throw new ClassNotFoundException( "No class provider specified" ) ;
            }
        }catch( IOException ioe ){
            throw new ClassNotFoundException( ioe.toString() ) ;
        }

        if( data == null )
            throw new
                ClassNotFoundException( "PANIC : class provider returned null" ) ;

        Class entry = defineClass( name , data, 0, data.length);

        if( resolve )resolveClass(entry );

        return entry ;
    }


}
