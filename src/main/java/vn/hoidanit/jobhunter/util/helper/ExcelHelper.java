package vn.hoidanit.jobhunter.util.helper;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import vn.hoidanit.jobhunter.domain.UserImportDTO;

import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelHelper {
    public static List<UserImportDTO> parseExcel(InputStream is) throws Exception {
        List<UserImportDTO> users = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || row.getCell(0) == null)
                    continue;

                UserImportDTO user = new UserImportDTO();
                user.setName(getString(row.getCell(0)));
                user.setEmail(getString(row.getCell(1)));
                user.setPassword(getString(row.getCell(2)));
                user.setGender(getString(row.getCell(3)));
                user.setAddress(getString(row.getCell(4)));
                user.setAge((int) row.getCell(5).getNumericCellValue());
                user.setRoleId((long) row.getCell(6).getNumericCellValue());

                users.add(user);
            }
        }
        return users;
    }

    private static String getString(Cell cell) {
        if (cell == null)
            return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}
