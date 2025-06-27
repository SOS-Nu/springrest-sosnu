package vn.hoidanit.jobhunter.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Value("${hoidanit.upload-file.base-uri}")
    private String baseURI;

    public void createDirectory(String folder) throws URISyntaxException {
        URI uri = new URI(folder);
        Path path = Paths.get(uri);
        File tmpDir = new File(path.toString());
        if (!tmpDir.isDirectory()) {
            try {
                Files.createDirectory(tmpDir.toPath());
                System.out.println(">>> CREATE NEW DIRECTORY SUCCESSFUL, PATH = " + tmpDir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> SKIP MAKING DIRECTORY, ALREADY EXISTS");
        }

    }

    public String store(MultipartFile file, String folder) throws URISyntaxException, IOException {
        // create unique filename
        String finalName = System.currentTimeMillis() + "-" + file.getOriginalFilename();

        URI uri = new URI(baseURI + folder + "/" + finalName);
        Path path = Paths.get(uri);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, path,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return finalName;
    }

    public long getFileLength(String fileName, String folder) throws URISyntaxException {
        URI uri = new URI(baseURI + folder + "/" + fileName);
        Path path = Paths.get(uri);

        File tmpDir = new File(path.toString());

        // file không tồn tại, hoặc file là 1 director => return 0
        if (!tmpDir.exists() || tmpDir.isDirectory())
            return 0;
        return tmpDir.length();
    }

    public InputStreamResource getResource(String fileName, String folder)
            throws URISyntaxException, FileNotFoundException {
        URI uri = new URI(baseURI + folder + "/" + fileName);
        Path path = Paths.get(uri);

        File file = new File(path.toString());
        return new InputStreamResource(new FileInputStream(file));
    }

    /**
     * PHƯƠNG THỨC ĐÃ CẬP NHẬT: Trích xuất nội dung văn bản từ một file.
     * 
     * @param file File đầu vào (PDF, DOCX, TXT, etc.)
     * @return Nội dung văn bản của file
     * @throws IOException
     */
    public String extractTextFromFile(MultipartFile file) throws IOException {
        Tika tika = new Tika();
        try (InputStream stream = file.getInputStream()) {
            return tika.parseToString(stream);
        } catch (TikaException e) {
            // Bắt TikaException và ném ra một IOException để Controller xử lý
            // Điều này giúp giữ cho các lớp gọi không cần biết về TikaException cụ thể
            throw new IOException("Lỗi khi phân tích cú pháp file: " + e.getMessage(), e);
        }
    }

    /**
     * PHƯƠNG THỨC MỚI: Đọc một file từ storage và trả về dưới dạng byte array.
     * 
     * @param fileName Tên file
     * @param folder   Thư mục chứa file
     * @return a byte array of the file content
     * @throws IOException
     * @throws URISyntaxException
     */
    public byte[] readFileAsBytes(String fileName, String folder) throws IOException, URISyntaxException {
        if (fileName == null || folder == null)
            return null;

        URI uri = new URI(baseURI + folder + "/" + fileName);
        Path path = Paths.get(uri);

        if (Files.exists(path) && !Files.isDirectory(path)) {
            return Files.readAllBytes(path);
        }

        return null; // Trả về null nếu file không tồn tại
    }

    /**
     * PHƯƠNG THỨC MỚI: Trích xuất nội dung văn bản từ một file đã được lưu trữ.
     *
     * @param fileName Tên file cần trích xuất.
     * @param folder   Thư mục chứa file.
     * @return Nội dung văn bản của file.
     * @throws IOException
     * @throws URISyntaxException
     */
    public String extractTextFromStoredFile(String fileName, String folder) throws IOException, URISyntaxException {
        byte[] fileBytes = readFileAsBytes(fileName, folder);
        if (fileBytes == null || fileBytes.length == 0) {
            return null;
        }
        return extractTextFromBytes(fileBytes);
    }

    /**
     * PHƯƠNG THỨC MỚI: Trích xuất nội dung văn bản từ một mảng byte.
     *
     * @param fileBytes Mảng byte của file.
     * @return Nội dung văn bản.
     * @throws IOException
     */
    public String extractTextFromBytes(byte[] fileBytes) throws IOException {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }
        Tika tika = new Tika();
        try (InputStream stream = new ByteArrayInputStream(fileBytes)) {
            return tika.parseToString(stream);
        } catch (TikaException e) {
            throw new IOException("Lỗi khi phân tích cú pháp dữ liệu byte: " + e.getMessage(), e);
        }
    };

}
