package FileManager.ImgManager;

import java.io.IOException;
import FileManager.FileHandler;

public abstract class IMGClass extends FileHandler {
    public IMGClass(String path) throws IOException {
        super(path);
    }

    public IMGClass() {
        super(0);
    }
}
