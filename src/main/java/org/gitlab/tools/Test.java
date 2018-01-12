package org.gitlab.tools;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;


public class Test {

    public static void main(String[] args) throws ZipException, IOException {
//c:\Users\Mishin737\Documents\work_line\07122017\..
        String root= "/Users/Mishin737/Documents/work_line/07122017/";
        File inputArchive = new File(root+"MailingWS_mapped.xar");
        File outputArchive = new File(root+"MailingWS_mapped2.xar");

        ArchiveModifier archive = new ArchiveModifier(inputArchive);
//        archive.remove("/ESB/Processes/run.get_mailing_status_vpk.ws.xml");//removeFilesWithName("ESB/Processes/run.get_mailing_status_vpk.ws.xml");
//        archive.add();
        archive.add("run.get_mailing_status_vpk.ws.xml", new File(root+"document.xml-1271425896"), true);

        archive.save(outputArchive);
    }

}
