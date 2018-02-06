package heptio.database;

public class EasyReaderWriter {

    private static EasyReaderWriter ourInstance = new EasyReaderWriter();

    public static EasyReaderWriter getInstance() {
        return ourInstance;
    }

    private EasyReaderWriter() {



    }



}
