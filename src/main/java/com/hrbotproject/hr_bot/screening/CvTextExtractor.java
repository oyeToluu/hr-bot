package com.hrbotproject.hr_bot.screening;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Turns an uploaded CV into plain text the screening bot can read.
 *
 * Supported: .pdf (Apache PDFBox), .docx (read straight from the Word XML,
 * no extra library), .txt, and .zip containing one of those (the first
 * PDF/DOCX/TXT inside is used, so candidates can "zip their CV").
 */
@Component
public class CvTextExtractor {

    // Guards against "zip bombs": a tiny zip that unpacks to gigabytes.
    static final int MAX_UNPACKED_BYTES = 10 * 1024 * 1024;

    public record ExtractedCv(String documentName, String text) {}

    public ExtractedCv extract(String fileName, byte[] bytes) {
        if (fileName == null || bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Please upload a CV file");
        }
        String ext = extension(fileName);
        try {
            return switch (ext) {
                case "zip" -> extractFromZip(bytes);
                case "pdf", "docx", "txt" -> new ExtractedCv(fileName, readDocument(ext, bytes));
                default -> throw new IllegalArgumentException(
                        "Unsupported file type ." + ext + ". Upload a PDF, Word (.docx), text or zip file");
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read " + fileName + ". Is the file damaged?");
        }
    }

    private ExtractedCv extractFromZip(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                // Skip folders and macOS metadata ("__MACOSX/._cv.pdf")
                if (entry.isDirectory() || name.contains("__MACOSX") || baseName(name).startsWith(".")) {
                    continue;
                }
                String ext = extension(name);
                if (ext.equals("pdf") || ext.equals("docx") || ext.equals("txt")) {
                    byte[] inner = readLimited(zip);
                    return new ExtractedCv(baseName(name), readDocument(ext, inner));
                }
            }
        }
        throw new IllegalArgumentException("The zip file has no PDF, Word (.docx) or text CV inside");
    }

    private String readDocument(String ext, byte[] bytes) throws IOException {
        return switch (ext) {
            case "pdf" -> readPdf(bytes);
            case "docx" -> readDocx(bytes);
            default -> new String(bytes, StandardCharsets.UTF_8);
        };
    }

    private String readPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    // A .docx is a zip; the text lives in word/document.xml. We turn
    // paragraph ends into new lines and drop every other XML tag.
    private String readDocx(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    String xml = new String(readLimited(zip), StandardCharsets.UTF_8);
                    String text = xml
                            .replaceAll("</w:p>", "\n")
                            .replaceAll("<w:tab/>", " ")
                            .replaceAll("<w:br/>", "\n")
                            .replaceAll("<[^>]+>", "");
                    return unescapeXml(text);
                }
            }
        }
        throw new IllegalArgumentException("This .docx file has no document text. Is it a real Word file?");
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > MAX_UNPACKED_BYTES) {
                throw new IllegalArgumentException("The file inside the zip is too large (over 10 MB unpacked)");
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static String unescapeXml(String s) {
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private static String baseName(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}
