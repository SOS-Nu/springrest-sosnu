package vn.hoidanit.jobhunter.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.entity.PaymentHistory;
import vn.hoidanit.jobhunter.repository.PaymentHistoryRepository;

@Service
public class PaymentExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,###");

    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentExportService(PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    // --- GIỮ NGUYÊN EXCEL (Theo yêu cầu) ---
    public ByteArrayInputStream exportToExcel(List<PaymentHistory> histories) throws IOException {
        String[] columns = { "ID", "User Email", "Số Tiền (VND)", "Mã Đơn Hàng", "Trạng Thái", "Ngày Tạo" };
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payment History");
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLUE.getIndex());
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (PaymentHistory history : histories) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(history.getId());
                row.createCell(1).setCellValue(history.getUser().getEmail());
                row.createCell(2).setCellValue(history.getAmount());
                row.createCell(3).setCellValue(history.getOrderId());
                row.createCell(4).setCellValue(history.getStatus().toString());
                if (history.getCreatedAt() != null) {
                    row.createCell(5).setCellValue(history.getCreatedAt().format(DATE_FORMATTER));
                }
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportReportByMonth(int month, int year, String creatorName) throws IOException {
        // 1. Lấy số liệu từ Database (Aggregated Data)
        Long totalRevenue = paymentHistoryRepository.getTotalRevenueByMonth(month, year);
        Long totalTxn = paymentHistoryRepository.countSuccessTransactions(month, year);

        // Xử lý null nếu không có giao dịch nào
        if (totalRevenue == null)
            totalRevenue = 0L;
        if (totalTxn == null)
            totalTxn = 0L;

        long avgRevenue = (totalTxn > 0) ? (totalRevenue / totalTxn) : 0;

        // 2. Chuẩn bị Map dữ liệu để thay thế
        Map<String, String> dataMapping = new HashMap<>();
        dataMapping.put("{{month}}", String.valueOf(month));
        dataMapping.put("{{year}}", String.valueOf(year));
        dataMapping.put("{{total_txn}}", String.valueOf(totalTxn));
        dataMapping.put("{{total_revenue}}", CURRENCY_FORMATTER.format(totalRevenue));
        dataMapping.put("{{avg_revenue}}", CURRENCY_FORMATTER.format(avgRevenue));
        dataMapping.put("{{creator_name}}", creatorName);

        // 3. Đọc file Template từ resources
        ClassPathResource resource = new ClassPathResource("templates/report_template.docx");

        try (InputStream is = resource.getInputStream();
                XWPFDocument document = new XWPFDocument(is);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 4. Duyệt qua các đoạn văn (Paragraphs) để replace text
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replaceTextInParagraph(paragraph, dataMapping);
            }

            // 5. Duyệt qua các bảng (Tables) nếu template có bảng
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replaceTextInParagraph(paragraph, dataMapping);
                        }
                    }
                }
            }

            document.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // Helper: Hàm tìm và thay thế text trong đoạn văn
    // Thay thế hàm replaceTextInParagraph cũ bằng hàm này
    private void replaceTextInParagraph(XWPFParagraph paragraph, Map<String, String> dataMapping) {
        String paragraphText = paragraph.getText();

        // Kiểm tra nhanh xem đoạn văn có chứa key nào không
        boolean hasKey = false;
        for (String key : dataMapping.keySet()) {
            if (paragraphText.contains(key)) {
                hasKey = true;
                break;
            }
        }

        if (!hasKey)
            return;

        // Nếu có key, ta thực hiện thay thế "Mạnh tay" (Brute-force) nhưng giữ lại
        // Style của đoạn văn
        // Cách này xử lý triệt để việc Word chia nhỏ chữ (Run splitting)

        String newText = paragraphText;
        for (Map.Entry<String, String> entry : dataMapping.entrySet()) {
            newText = newText.replace(entry.getKey(), entry.getValue());
        }

        // Xóa sạch các Run cũ trong đoạn văn (để tránh rác)
        // Lưu ý: Phải xóa từ cuối lên đầu để không lỗi index
        while (paragraph.getRuns().size() > 0) {
            paragraph.removeRun(0);
        }

        // Tạo một Run mới chứa toàn bộ text đã thay thế
        XWPFRun newRun = paragraph.createRun();
        newRun.setText(newText);

        // Thiết lập lại Font/Size mặc định để file báo cáo nhìn đẹp
        // (Bạn có thể chỉnh theo font chữ của file Word mẫu)
        newRun.setFontFamily("Times New Roman");
        newRun.setFontSize(13);

        // Mẹo: Nếu dòng này là Tiêu đề (dựa vào text), ta set Bold
        // Đây là logic phụ trợ để giữ định dạng
        if (newText.contains("BÁO CÁO") || newText.contains("CỘNG HÒA")) {
            newRun.setBold(true);
            newRun.setFontSize(14); // Nếu là tiêu đề thì cho to hơn
        }
    }

}