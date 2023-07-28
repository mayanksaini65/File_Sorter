package com.fileSorter.main;

import static java.util.Map.entry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Application {

	static Map<String, String> fileFormatMap = Map.ofEntries(entry("exe", "Executables"), entry("msi", "Executables"),
			entry("img", "Images"), entry("jpg", "Images"), entry("jpeg", "Images"), entry("png", "Images"),
			entry("gif", "Images"), entry("svg", "Images"), entry("tiff", "Images"), entry("raw", "Images"),
			entry("bmp", "Images"), entry("mp3", "Music"), entry("avi", "Videos"), entry("mp4", "Videos"),
			entry("webm", "Videos"), entry("mkv", "Videos"), entry("doc", "Documents/Word Documents"),
			entry("ppt", "Documents/Presentations"), entry("pptx", "Documents/Presentations"),
			entry("docx", "Documents/Word Documents"), entry("csv", "Documents/Excel Documents"),
			entry("xls", "Documents/Excel Documents"), entry("xlsx", "Documents/Excel Documents"),
			entry("pdf", "Documents/Pdf Documents"), entry("txt", "Documents/Text Files"), entry("zip", "Zip Files"),
			entry("gz", "Zip Files"), entry("tgz", "Zip Files"));

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Enter a location to sort");

		Scanner scanner = new Scanner(System.in);
		String sourceLocation = scanner.next();

		Set<String> extensionsSet = new TreeSet<>();
		Set<String> directorySet = new HashSet<>();

		File actual = new File(sourceLocation);
		while (actual != null && !actual.isDirectory()) {
			System.out.println("The entered directory is not a directory.");
			System.out.println("Enter a directory location or type exit to stop.");

			sourceLocation = scanner.next();

			if (sourceLocation.equalsIgnoreCase("exit")) {
				scanner.close();
				System.out.println("Bye bye.");
				return;
			} else
				actual = new File(sourceLocation);
		}

		Map<String, String> fileNameLocationMap = new HashMap<String, String>();
		long totalFileSize = 0;
		for (File f : actual.listFiles()) {
			int index = f.getName().lastIndexOf(".");
			if (index < 0) {
				if (!f.isDirectory()) {
					extensionsSet.add(f.getName());
				} else
					directorySet.add(f.getName());
			} else {
				String extension = f.getName().substring(index);
				extensionsSet.add(extension);
				fileNameLocationMap.put(f.getName(), extension);
				totalFileSize += f.length();
			}
		}

		System.out.println("Total " + fileNameLocationMap.size() + " files and " + directorySet.size()
				+ " directories detected with a total file size of: " + formatSize(totalFileSize));
		System.out.println();

		System.out.println("Enter location for sorted files. (Default taking current location as the target)");
		String targetLocation = scanner.next();
		if (targetLocation.isBlank() || !Files.isDirectory(Path.of(targetLocation), LinkOption.NOFOLLOW_LINKS)) {
			System.out.println("Not a valid folder! Taking the default value.");
			targetLocation = sourceLocation;
		}

		System.out.println("Checking for permissions");
		Thread.sleep(2000);
		if (!Files.isWritable(Paths.get(targetLocation))) {
			System.out.println("Permission not found!");
			System.out.println("Make sure to run as admin.");
		} else {
			Map<String, List<String>> newLocations = getSortedLocations(fileNameLocationMap, targetLocation);

			System.out.println("Do you want a preview of how files will be sorted? Press Y/N");
			String preview = scanner.next();
			if (preview.equalsIgnoreCase("y")) {
				System.out.println("Working....");
				printPreview(newLocations, targetLocation);

			} else if (!preview.equalsIgnoreCase("n"))
				System.out.println("Invalid input detected. Ignoring preview!");

			System.out.println("\n\n\n");

			System.out.println("Proceed to sort files? (Y/N)");
			String sortingInput = scanner.next();

			if (sortingInput.equalsIgnoreCase("y")) {
				if (newLocations != null && !newLocations.isEmpty())
					sortFiles(newLocations, targetLocation, sourceLocation, fileNameLocationMap.size());
				else
					System.out.println("Something went wrong!\n Contact developer at: saini_mayank@outlook.com");
			} else if (!sortingInput.equalsIgnoreCase("n"))
				System.out.println("Invalid input detected. Ignoring!");
		}

		System.out.println("Bye!");

		scanner.close();
	}

	private static void sortFiles(Map<String, List<String>> newLocations, String targetLocation,
			String originalLocation, int totalFiles) {
		AtomicInteger count = new AtomicInteger(0);
		LocalTime startTime = LocalTime.now();
		newLocations.forEach((folderName, fileList) -> {
			fileList.forEach(fileName -> {
				try {
					Path originalPath = Paths.get(originalLocation, fileName);
					Path copiedFolderPath = Paths.get(targetLocation, folderName);
					Files.createDirectories(copiedFolderPath);
					Path copiedFilePath = Paths.get(copiedFolderPath.toString(), fileName);
					count.incrementAndGet();
					Files.copy(originalPath, copiedFilePath, StandardCopyOption.REPLACE_EXISTING);

					if (count.get() % 10 == 0) {
						StringBuilder sb = new StringBuilder();
						sb.append("Time elapsed: ");
						sb.append(startTime.until(LocalTime.now(), ChronoUnit.SECONDS));
						sb.append(" seconds.");
						sb.append(" Files copied: ");
						sb.append(count.get());
						sb.append(" Files remaining: ");
						sb.append(totalFiles - count.get());
						System.out.println(sb.toString());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		});

		System.out.println("Total files sorted :" + count.get());
	}

	private static Map<String, List<String>> getSortedLocations(Map<String, String> fileNameLocationMap,
			String targetLocation) {
		Map<String, List<String>> newLocations = new HashMap<String, List<String>>();
		fileNameLocationMap.forEach((fileName, fileExtension) -> {
			String folderName = "Sorted/"
					+ fileFormatMap.getOrDefault(fileExtension.replace(".", "").toLowerCase(), "Misc");
			List<String> files = newLocations.getOrDefault(folderName, new ArrayList<>());
			files.add(fileName);
			newLocations.put(folderName, files);
		});
		return newLocations;
	}

	private static void printPreview(Map<String, List<String>> newLocations, String targetLocation) {
		if (!newLocations.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append(targetLocation);
			sb.append("\n");
			newLocations.forEach((folderName, filesList) -> {
				sb.append(getIndentString(1));
				sb.append(folderName);
				sb.append("/");
				sb.append("\n");
				for (String file : filesList) {
					printFile(file, 2, sb);
				}
			});
			System.out.println(sb.toString());
		}
	}

	private static void printFile(String fileName, int indent, StringBuilder sb) {
		sb.append(getIndentString(indent));
		sb.append("+--");
		sb.append(fileName);
		sb.append("\n");
	}

	private static String getIndentString(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append("|  ");
		}
		return sb.toString();
	}

	private static String formatSize(long v) {
		if (v < 1024)
			return v + " B";
		int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
		return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
	}
}
