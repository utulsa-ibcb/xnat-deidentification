package org.ibcb.xnat.redaction.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;
import org.ibcb.xnat.redaction.client.DataBaseConnector;

public class MessagePasser {
    public static void main (String[] args) throws ClassNotFoundException, SQLException, FileNotFoundException, IOException, AddressException, MessagingException {
        Map<String,String> argmap = new HashMap<String,String>();
        for (int i = 0; i < args.length-1; i += 2) argmap.put(args[i], args[i+1]);
        for (String key : argmap.keySet()) System.out.println(key + " " + argmap.get(key));

        FileReader fr = new FileReader("data/mpassing.config");
        BufferedReader br = new BufferedReader(fr);
        String strLine;
        while ((strLine = br.readLine()) != null) {
            String[] assign = strLine.split("=");
            argmap.put(assign[0], assign[1]);
        }

        fr.close();

        String dburl = argmap.get("dburl");
        String database = argmap.get("database");
        String dbtable = argmap.get("dbtable");
        String username = argmap.get("username");
        String password = argmap.get("password");
        String host = argmap.get("-h");
        String from = argmap.get("-f");
        String to = argmap.get("-t");

        HtmlEmail he = new HtmlEmail();
        he.setHostName(host);

        DataBaseConnector dbc = new DataBaseConnector(database, username,
            password, dburl, DataBaseConnector.PLATFORM_POSTGRES);
        int quid = dbc.openQuery("SELECT subject, text, timestamp FROM " + dbtable +
            " WHERE flag = FALSE AND recipient = '" + to + "'");
        ResultSet rs = dbc.getResults(quid);

        while (rs.next()) {
            String subject = rs.getString(1);
            String text = rs.getString(2);
            String timestamp = rs.getString(3);

            dbc.executeUpdate("UPDATE " + dbtable + " SET flag = TRUE WHERE subject = '" +
                subject + "' AND text = '" + text + "' AND timestamp = '" + 
                timestamp + "' AND recipient = '" + to + "'");

            text = "The following was created at " + timestamp + ":\n" + text;

            he.setSubject(subject);
            he.setHtmlMsg("<html>" + text + "</html>");
            he.setTextMsg(StringUtils.replace(text, "<br />", "\n"));
            he.addTo(to);
            he.setFrom(from);
            he.send();
        }
        dbc.closeQuery(quid);
    }
}
