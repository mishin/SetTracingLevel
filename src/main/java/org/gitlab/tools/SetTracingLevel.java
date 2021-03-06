package org.gitlab.tools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Properties;

class SetTracingLevel {
    private static final String PROP_FILENAME = "config.properties";
    private final String archiveDirectory;
    private final String trackingLevel;
    private final String trackingEndpointTopic;

    private SetTracingLevel(Properties prop) {
        archiveDirectory = prop.getProperty("archive_directory");
        trackingLevel = prop.getProperty("tracking_level");
        trackingEndpointTopic = prop.getProperty("tracking_endpoint_topic");
    }

    /**
     * Правим уровень логирования в ESB процессе
     */
    public static void main(String[] args) throws IOException {
        SetTracingLevel setTracingLevel = new SetTracingLevel(readProperties());
        setTracingLevel.changeTracingLevelInDirectory();
    }

    private void changeTracingLevelInDirectory() {
        System.out.println(String.format("Set Trace level to all xar files in directory: %s", getArchiveDirectory()));
        DirectoryStream.Filter<Path> documentFilter = entry -> {
            String fileName = entry.getFileName().toString();
            return fileName != null && (fileName.endsWith("xar") || fileName.endsWith("zip"));
        };
        try (DirectoryStream<Path> pathList = Files.newDirectoryStream(Paths.get(getArchiveDirectory()),
                documentFilter)) {
            for (Path path : pathList) {
                switch (FilenameUtils.getExtension(path.toString())) {
                    case "xar":
                        changeTracingLevelInFile(path.toFile());
                        break;
                    case "zip":
                        changeTracingLevelInSdmFile(path.toFile());
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private static void regexpReplacer(String docPath, String regexp, String toInsert) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(docPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String xmlContent = "";
        assert fis != null;
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader bReader = new BufferedReader(isr);
        String buf;

        while ((buf = bReader.readLine()) != null) {
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


    private void changeTracingLevelInSdmFile(File archiveName) {
        try {
            Path tempDirectory = Files.createTempDirectory(null);
            ZipUtil.unpack(archiveName, tempDirectory.toFile());
            processXarInSdmDirectory(tempDirectory);
            ZipUtil.pack(tempDirectory.toFile(), archiveName);
            FileUtils.deleteDirectory(tempDirectory.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processXarInSdmDirectory(Path tempDirectory) {
        try {
            Files.walkFileTree(tempDirectory, new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    if (file.toString().endsWith(".xar")) {
                        System.out.println("Process xar files in SDM directory: " + file);
                        changeTracingLevelInFile(file.toFile());
                    }
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
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void changeTracingLevelInFile(File archiveName) {
        try {
            Path tempDirectory = Files.createTempDirectory(null);
            ZipUtil.unpack(archiveName, tempDirectory.toFile());
            changeLevelInAllFilesInDirectory(tempDirectory);
            ZipUtil.pack(tempDirectory.toFile(), archiveName);
            FileUtils.deleteDirectory(tempDirectory.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeLevelInAllFilesInDirectory(Path tempDirectory) throws IOException {
        Files.walkFileTree(tempDirectory, new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (file.toString().endsWith(".xml")) {
                    System.out.println("Replace trace in xml File: " + file);
                    replaceTraceLevel(file.toString());
                }
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
                return FileVisitResult.CONTINUE;
            }
        });
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

    private String getTrackingLevel() {
        return trackingLevel;
    }

    private String getTrackingEndpointTopic() {
        return trackingEndpointTopic;
    }

    private String getArchiveDirectory() {
        return archiveDirectory;
    }
}
