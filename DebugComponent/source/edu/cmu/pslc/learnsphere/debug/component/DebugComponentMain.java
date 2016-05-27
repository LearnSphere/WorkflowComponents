package edu.cmu.pslc.learnsphere.debug.component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.FastDateFormat;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class DebugComponentMain extends AbstractComponent {

    public static void main(String[] args) {

        DebugComponentMain tool = new DebugComponentMain();
        tool.startComponent(args);
    }

    public DebugComponentMain() {
        super();
    }


    public static String whoami() throws Exception {
        Process p = Runtime.getRuntime().exec("whoami");
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        String output = "";

        while ((line = in.readLine()) != null) {
            output += line;
        }
        in.close();
        p.destroy();
        return output;
    }


}
