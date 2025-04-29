package vn.hoidanit.jobhunter.util.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

import vn.hoidanit.jobhunter.domain.UserImportDTO;

public class CsvHelper {
    public static List<UserImportDTO> parseCsv(InputStream is) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(is))) {
            return new CsvToBeanBuilder<UserImportDTO>(reader)
                    .withType(UserImportDTO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }
    }
}
