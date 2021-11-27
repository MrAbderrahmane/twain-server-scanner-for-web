
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;

public class MyImageUtils {
    
    public static String toBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        byte[] data = baos.toByteArray();
        baos.close();
        return "data:image/jpg;base64," + Base64.encodeBase64String(data);
    }

}
