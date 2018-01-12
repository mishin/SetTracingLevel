package org.gitlab.tools.Logic;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ZipReplacer {

	public static void zipFileReplace(String zipFileString, String fileInsideZipString, String myFileString){
		Path zipFilePath = Paths.get(zipFileString);
		
		Path myFilePath = Paths.get(myFileString);
		
	    try( FileSystem fs = FileSystems.newFileSystem(zipFilePath, null) ){
			fileInsideZipString = "/".concat(fileInsideZipString.replace("\\", "/"));
	        Path fileInsideZipPath = fs.getPath(fileInsideZipString);
	        Files.copy(myFilePath, fileInsideZipPath, REPLACE_EXISTING);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public static void getFileFromZip(String zipFileString, String fileInsideZipString, String myFileString){
		Path zipFilePath = Paths.get(zipFileString);
				
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(myFileString);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try( FileSystem fs = FileSystems.newFileSystem(zipFilePath, null) ){
	        Path fileInsideZipPath = fs.getPath(fileInsideZipString);
			assert fos != null;
			Files.copy(fileInsideZipPath, fos);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }finally{
	    	try {
				assert fos != null;
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
	
	public static void regexpReplacer(String docPath, String regexp, String toInsert) throws IOException{
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

	/**
	 * Modifies, adds or deletes file(s) from a existing zip file.
	 *
	 * @param zipFile the original zip file
	 * @param newZipFile the destination zip file
	 * @param filesToAddOrOverwrite the names of the files to add or modify from the original file
	 * @param filesToAddOrOverwriteInputStreams the input streams containing the content of the files
	 * to add or modify from the original file
	 * @param filesToDelete the names of the files to delete from the original file
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
}
