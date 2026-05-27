package bitzero.server.util;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public final class FlashMasterSocketPolicyLoader {
     public String loadPolicy(String fileName) throws IOException {
          return FileUtils.readFileToString(new File(fileName));
     }
}
