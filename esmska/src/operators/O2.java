/*
 * O2.java
 *
 * Created on 7. červenec 2007, 18:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package operators;

import esmska.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ripper
 */
public class O2 implements Operator {
    private final int MAX_CHARS = 60;
    private final int SMS_LENGTH = 60;
    CookieManager manager;
    
    /**
     * Creates a new instance of O2
     */
    public O2() {
        manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }
    
    public URL getSecurityImage() {
        URL url = null;
        try {
            url = new URL("http://www2.cz.o2.com/sms/SMSGWChargingClient?action=edit");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            List<String> cookies = con.getHeaderFields().get("Set-Cookie");
            con.disconnect();
            
            CookieHandler.setDefault(manager);
            for (String c : cookies) {
                c = c.substring(0, c.indexOf(";"));
                String cookieName = c.substring(0, c.indexOf("="));
                String cookieValue = c.substring(c.indexOf("=") + 1, c.length());
                HttpCookie cookie = new HttpCookie(cookieName,cookieValue);
                cookie.setPath("/");
                cookie.setMaxAge(10000);
                manager.getCookieStore().add(new URI("http://www2.cz.o2.com/"),cookie);
            }
            
            url = new URL("http://www2.cz.o2.com/sms/SMSGWChargingClient?action=image");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return url;
    }
    
    public boolean send(SMS sms) {
        try {
            URL url = new URL("http://www2.cz.o2.com/sms/SMSGWChargingClient");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=windows-1250");
            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), "windows-1250");
            wr.write("action=" + URLEncoder.encode("confirm","windows-1250")
            + "&msgText=" + URLEncoder.encode(sms.getText()!=null?sms.getText():"","windows-1250")
            + "&sn=" + URLEncoder.encode(sms.getNumber()!=null?sms.getNumber():"","windows-1250")
            + "&code=" + URLEncoder.encode(sms.getImageCode()!=null?sms.getImageCode():"","windows-1250")
            + "&reply=" + URLEncoder.encode("0","windows-1250")
            + "&intruder=" + URLEncoder.encode("0","windows-1250")
            + "&lang=" + URLEncoder.encode("cs","windows-1250")
            );
            wr.flush();
            wr.close();
            
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "windows-1250"));
            String content = "";
            String s = "";
            while ((s = br.readLine()) != null) {
                content += s + "\n";
            }
            br.close();
            con.disconnect();
            
            Pattern p = Pattern.compile("<div class=\"err\">\n*</div>");
            Matcher m = p.matcher(content);
            boolean ok = m.find();
            
            if (!ok) {
                p = Pattern.compile("<div class=\"err\">(.*?)</div>",Pattern.DOTALL);
                m = p.matcher(content);
                boolean error = m.find();
                if (error)
                    sms.setErrMsg(m.group(1));
                
                CookieHandler.setDefault(null);
                return false;
            }
            
            // confirm page
            
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=windows-1250");
            wr = new OutputStreamWriter(con.getOutputStream(), "windows-1250");
            wr.write("action=" + URLEncoder.encode("send","windows-1250")
            + "&lang=" + URLEncoder.encode("cs","windows-1250")
            );
            wr.flush();
            wr.close();
            
            br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "windows-1250"));
            content = "";
            s = "";
            while ((s = br.readLine()) != null) {
                content += s + "\n";
            }
            br.close();
            con.disconnect();
            
            p = Pattern.compile("<div class=\"err\">\n*</div>");
            m = p.matcher(content);
            ok = m.find();
            
            if (!ok) {
                p = Pattern.compile("<div class=\"err\">(.*?)</div>",Pattern.DOTALL);
                m = p.matcher(content);
                boolean error = m.find();
                if (error)
                    sms.setErrMsg(m.group(1));
                
                CookieHandler.setDefault(null);
                return false;
            }
            
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        CookieHandler.setDefault(null);
        return false;
    }
    
    public String toString() {
        return "O2";
    }
    
    public int getMaxChars() {
        return MAX_CHARS;
    }
    
    public int getSMSLength() {
        return SMS_LENGTH;
    }
    
    public int getSMSCount(int chars) {
        return (int)Math.ceil((double)chars / SMS_LENGTH);
    }
    
    
}
