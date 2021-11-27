import free.lucifer.jtwain.Twain;
import free.lucifer.jtwain.TwainScanner;
import free.lucifer.jtwain.exceptions.TwainException;

public class MySourceManager {

    private static MySourceManager instance;

    public static MySourceManager instance() {
        if (instance == null) {
            instance = new MySourceManager();
        }
        return instance;
    }

    public MySource getSource(String sourceName) throws TwainException {
        for (String s : TwainScanner.getScanner().getDeviceNames()) {
            if(s.equals(sourceName)){
                MySource source = new MySource();
                source.setName(s);
                return source;
            }
        }
        return null;
    }

    public static String[] getDeviceNames() throws TwainException{
        return TwainScanner.getScanner().getDeviceNames();
    }

    public void freeResources() {
        Twain.done();
    }

}

