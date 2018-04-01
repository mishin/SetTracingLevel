package org.gitlab.tools;

import org.apache.commons.io.IOUtils;
import org.gitlab.tools.Logic.ZipReplacer;
import org.zeroturnaround.zip.ZipUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class SetTracingLevel {


    private static final int BUFFER_SIZE = 1024;
    private static final String PROP_FILENAME = "config.properties";
    private String archiveName;
    private String tmpXmlFileName;
//    private String fileForChange;
    private String trackingLevel;
    private String trackingEndpointTopic;

    SetTracingLevel(Properties prop) {
        archiveName = prop.getProperty("archive_name");
//        fileForChange = prop.getProperty("file_for_change");
        trackingLevel = prop.getProperty("tracking_level");
        trackingEndpointTopic = prop.getProperty("tracking_endpoint_topic");
        tmpXmlFileName = getTmpFile();
    }

    public static void main(String[] args) throws IOException {
        /**
         * Правим уровень логирования в ESB процессе
         */
        SetTracingLevel setTracingLevel = new SetTracingLevel(readProperties());
        // create a buffer to improve copy performance later.
        setTracingLevel.copyZipToFile();
//        setTracingLevel.replaceTraceLevel();
/*
        TFile archive = new TFile("archive.zip");
        for (String member : archive.list())
            System.out.println(member);*/
        // append a file to archive under different name
//        TFile.cp(new File("existingFile.txt"), new TFile("archive.zip", "entry.txt"));
//        ZipReplacer.zipFileReplace(setTracingLevel.getArchiveName(),setTracingLevel.getFileForChange(), tmpXmlFileName);

//        ZipReplacer.zipFileReplace();
    }

    /**
     * Modifies, adds or deletes file(s) from a existing zip file.
     *
     * @param zipFile                           the original zip file
     * @param newZipFile                        the destination zip file
     * @param filesToAddOrOverwrite             the names of the files to add or modify from the original file
     * @param filesToAddOrOverwriteInputStreams the input streams containing the content of the files
     *                                          to add or modify from the original file
     * @param filesToDelete                     the names of the files to delete from the original file
     * @throws IOException if the new file could not be written
     */
    public static void modifyZipFile(File zipFile,
                                     File newZipFile,
                                     String[] filesToAddOrOverwrite,
                                     InputStream[] filesToAddOrOverwriteInputStreams,
                                     String[] filesToDelete) throws IOException {


        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(newZipFile))) {

            // add existing ZIP entry to output stream
            try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry = null;
                while ((entry = zin.getNextEntry()) != null) {
                    String name = entry.getName();

                    // check if the file should be deleted
                    if (filesToDelete != null) {
                        boolean ignoreFile = false;
                        for (String fileToDelete : filesToDelete) {
                            if (name.equalsIgnoreCase(fileToDelete)) {
                                ignoreFile = true;
                                break;
                            }
                        }
                        if (ignoreFile) {
                            continue;
                        }
                    }

                    // check if the file should be kept as it is
                    boolean keepFileUnchanged = true;
                    if (filesToAddOrOverwrite != null) {
                        for (String fileToAddOrOverwrite : filesToAddOrOverwrite) {
                            if (name.equalsIgnoreCase(fileToAddOrOverwrite)) {
                                keepFileUnchanged = false;
                            }
                        }
                    }

                    if (keepFileUnchanged) {
                        // copy the file as it is
                        out.putNextEntry(new ZipEntry(name));
                        IOUtils.copy(zin, out);
                    }
                }
            }

            // add the modified or added files to the zip file
            if (filesToAddOrOverwrite != null) {
                for (int i = 0; i < filesToAddOrOverwrite.length; i++) {
                    String fileToAddOrOverwrite = filesToAddOrOverwrite[i];
                    try (InputStream in = filesToAddOrOverwriteInputStreams[i]) {
                        out.putNextEntry(new ZipEntry(fileToAddOrOverwrite));
                        IOUtils.copy(in, out);
                        out.closeEntry();
                    }
                }
            }

        }

    }

    private void replaceFileInZip() {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        Path path = Paths.get("test.zip");
        URI uri = URI.create("jar:" + path.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            Path nf = fs.getPath("new.txt");
            try (Writer writer = Files.newBufferedWriter(nf, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                writer.write("hello");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addFilesToZip(File source, File[] files) {
        try {

            File tmpZip = File.createTempFile(source.getName(), null);
            tmpZip.delete();
            if (!source.renameTo(tmpZip)) {
                throw new Exception("Could not make temp file (" + source.getName() + ")");
            }
            byte[] buffer = new byte[1024];
            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(source));

            for (int i = 0; i < files.length; i++) {
                InputStream in = new FileInputStream(files[i]);
                out.putNextEntry(new ZipEntry(files[i].getName()));
                for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
                in.close();
            }

            for (ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()) {
                out.putNextEntry(ze);
                for (int read = zin.read(buffer); read > -1; read = zin.read(buffer)) {
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
            }

            out.close();
            tmpZip.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractSubDir(String archiveName, String targetDir)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        // Создаем каталог, куда будут распакованы файлы
        final String dstDirectory = targetDir;
        final File dstDir = new File(dstDirectory);
        if (!dstDir.exists()) {
            dstDir.mkdir();
        }

        try {
            // Получаем содержимое ZIP архива
            final ZipInputStream zis = new ZipInputStream(
                    new FileInputStream(archiveName));
            ZipEntry ze = zis.getNextEntry();
            String nextFileName;
            while (ze != null) {
                nextFileName = ze.getName();
                File nextFile = new File(dstDirectory + File.separator
                        + nextFileName);
                System.out.println("Распаковываем: "
                        + nextFile.getAbsolutePath());
                // Если мы имеем дело с каталогом - надо его создать. Если
                // этого не сделать, то не будут созданы пустые каталоги
                // архива
                if (ze.isDirectory()) {
                    nextFile.mkdir();
                } else {
                    // Создаем все родительские каталоги
                    new File(nextFile.getParent()).mkdirs();
                    // Записываем содержимое файла
                    try (FileOutputStream fos
                                 = new FileOutputStream(nextFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(org.gitlab.tools.SetTracingLevel.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(org.gitlab.tools.SetTracingLevel.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

    private void copyZipToFile() {
        try {
            Path tmp1 = Files.createTempDirectory(null);
            System.out.println("TMP1: " + tmp1.toString());
//            Path path = Paths.get(archiveName);
//            extractSubDir(archiveName, tmp1.toString());
            ZipUtil.unpack(new File(archiveName), new File(tmp1.toString()));
            String pathString = tmp1.toString();
            Files.walkFileTree(Paths.get(pathString), new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
//                    System.out.println("preVisitDirectory: " + dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    System.out.println("Replace trace in File: " + file);
                    replaceTraceLevel(file.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc)
                        throws IOException {
                    System.out.println("visitFileFailed: " + file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
//                    System.out.println("postVisitDirectory: " + dir);
                    return FileVisitResult.CONTINUE;
                }
            });//            replaceTraceLevel();
            ZipUtil.pack(new File(pathString), new File(archiveName));
        } catch (IOException e) {
            System.err.println(e);
        }

    }




    private static void write(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);
        out.close();
        in.close();
    }

    private void copyArchiveFileToTemporeryFile(ZipInputStream stream) throws IOException {

        byte[] buffer = new byte[2048];
        // Once we get the entry from the stream, the stream is
        // positioned read to read the raw data, and we keep
        // reading until read returns 0 or less.
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(getTmpXmlFileName());
            int len = 0;
            while ((len = stream.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
        } finally {
            // we must always close the output file
            if (output != null) output.close();
        }
    }

    private void showFilesInArchive() {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(getArchiveName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Enumeration zipEnum = zipfile.entries();
        while (zipEnum.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) zipEnum.nextElement();
            int size = (int) zipEntry.getSize();
            String s = String.format("Entry: %s len %d",
                    zipEntry.getName(), size);
            System.out.println(s);

        }
    }

    private String getTmpFile() {
        Random r = new Random(System.currentTimeMillis());
        int randomForName = r.nextInt();
        return "document.xml" + randomForName;
    }

    private StringBuilder getTxtFiles(InputStream in) {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        } catch (IOException e) {
            // do something, probably not a text file
            e.printStackTrace();
        }
        return out;
    }

    private byte[] getImage(InputStream in) {
        try {
            BufferedImage image = ImageIO.read(in); //just checking if the InputStream belongs in fact to an image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            // do something, it is not a image
            e.printStackTrace();
        }
        return null;
    }


   
    void streamCopy(Path src, Path dst) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(src)));
             BufferedWriter bw = new BufferedWriter(
                     new OutputStreamWriter(Files.newOutputStream(dst)))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("key1=value1", "key1=value2");
                bw.write(line);
                bw.newLine();
            }
        }
    }

    static private Properties readProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(PROP_FILENAME);
            // load a properties file
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }


    private void replaceTraceLevel(String ProcessFile) {
        try {

/*            <xq:trackingDetails trackingLevel="0">
            <xq:eventEndpoint endpoint_ref="Broker_Tracking.Entry" type="ENDPOINT"/>
            <xq:idGenerator class="com.sonicsw.xqimpl.service.accessor.ScriptEvaluator"/>
            </xq:trackingDetails>*/


            regexpReplacer(ProcessFile,
                    "<xq:eventEndpoint endpoint_ref=\"(.*?)\" type=\"ENDPOINT\"/>",
                    "<xq:eventEndpoint endpoint_ref=\"" + getTrackingEndpointTopic() + "\" type=\"ENDPOINT\"/>");
            regexpReplacer(ProcessFile,
                    "<xq:trackingDetails trackingLevel=\"(.*?)\">",
                    "<xq:trackingDetails trackingLevel=\"" + getTrackingLevel() + "\">");

        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }


    private static void regexpReplacer(String docPath, String regexp, String toInsert) throws IOException{
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(docPath);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String xmlContent = "";
        assert fis != null;
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader bReader = new BufferedReader(isr);
        String buf;

        while((buf = bReader.readLine()) != null){
            xmlContent += buf + "\n";
        }

        bReader.close();

        xmlContent = xmlContent.replaceAll(regexp, toInsert);

        FileOutputStream fos = new FileOutputStream(docPath);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bWriter = new BufferedWriter(osw);
        bWriter.write(xmlContent);
        bWriter.flush();
        bWriter.close();
    }


    public String getArchiveName() {
        return archiveName;
    }

//    public String getFileForChange() {
//        return fileForChange;
//    }

    public String getTrackingLevel() {
        return trackingLevel;
    }

    public String getTrackingEndpointTopic() {
        return trackingEndpointTopic;
    }

    public String getTmpXmlFileName() {
        return tmpXmlFileName;
    }
}
