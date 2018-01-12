package org.gitlab.tools;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AddZip {

    public void AddZip() {
    }

    public void addToZipFile(ZipOutputStream zos, String nombreFileAnadir, String nombreDentroZip) {
        FileInputStream fis = null;
        try {
            if (!new File(nombreFileAnadir).exists()) {//NO EXISTE
                System.out.println(" No existe el archivo :  " + nombreFileAnadir);return;
            }
            File file = new File(nombreFileAnadir);
            System.out.println(" Generando el archivo '" + nombreFileAnadir + "' al ZIP ");
            fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(nombreDentroZip);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {zos.write(bytes, 0, length);}
            zos.closeEntry();
            fis.close();

        } catch (FileNotFoundException ex ) {
            Logger.getLogger(AddZip.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AddZip.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}