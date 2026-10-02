package com.hrbotproject.hr_bot.screening;

import com.hrbotproject.hr_bot.screening.CvTextExtractor.ExtractedCv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvTextExtractorTest {

    private final CvTextExtractor extractor = new CvTextExtractor();

    @Test
    @DisplayName("Reads a plain text CV")
    void readsTxt() {
        ExtractedCv cv = extractor.extract("cv.txt", "Java developer".getBytes(StandardCharsets.UTF_8));

        assertThat(cv.text()).isEqualTo("Java developer");
        assertThat(cv.documentName()).isEqualTo("cv.txt");
    }

    @Test
    @DisplayName("Reads the text of a Word .docx CV")
    void readsDocx() throws IOException {
        byte[] docx = docx("Ada Okafor", "Skills: Java &amp; Spring Boot");

        ExtractedCv cv = extractor.extract("Ada CV.DOCX", docx);

        assertThat(cv.text()).contains("Ada Okafor").contains("Java & Spring Boot");
    }

    @Test
    @DisplayName("Finds the CV inside a zip, skipping macOS junk files")
    void readsCvInsideZip() throws IOException {
        byte[] zip = zip(
                new Entry("__MACOSX/._cv.docx", "junk".getBytes(StandardCharsets.UTF_8)),
                new Entry("photo.jpg", new byte[]{1, 2, 3}),
                new Entry("my-cv/cv.docx", docx("MongoDB and Git")));

        ExtractedCv cv = extractor.extract("application.zip", zip);

        assertThat(cv.documentName()).isEqualTo("cv.docx");
        assertThat(cv.text()).contains("MongoDB and Git");
    }

    @Test
    @DisplayName("Rejects a zip with no CV inside")
    void zipWithoutCv_isRejected() throws IOException {
        byte[] zip = zip(new Entry("photo.jpg", new byte[]{1, 2, 3}));

        assertThatThrownBy(() -> extractor.extract("stuff.zip", zip))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no PDF, Word");
    }

    @Test
    @DisplayName("Rejects unsupported file types")
    void unsupportedType_isRejected() {
        assertThatThrownBy(() -> extractor.extract("cv.exe", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type .exe");
    }

    @Test
    @DisplayName("Rejects a damaged PDF with a friendly message")
    void damagedPdf_isRejected() {
        byte[] notReallyPdf = "this is not a pdf".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> extractor.extract("cv.pdf", notReallyPdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not read cv.pdf");
    }

    // ---- helpers: build small .docx and .zip files in memory ----

    private record Entry(String name, byte[] bytes) {}

    private static byte[] zip(Entry... entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Entry e : entries) {
                zip.putNextEntry(new ZipEntry(e.name()));
                zip.write(e.bytes());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    // Minimal Word file: just word/document.xml with one paragraph per line.
    private static byte[] docx(String... paragraphs) throws IOException {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?><w:document><w:body>");
        for (String p : paragraphs) {
            xml.append("<w:p><w:r><w:t>").append(p).append("</w:t></w:r></w:p>");
        }
        xml.append("</w:body></w:document>");
        return zip(new Entry("word/document.xml", xml.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
