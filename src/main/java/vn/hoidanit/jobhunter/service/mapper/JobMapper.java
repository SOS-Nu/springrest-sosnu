package vn.hoidanit.jobhunter.service.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.response.job.ResJobDTO;
import vn.hoidanit.jobhunter.service.ExchangeRateService;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class JobMapper {

    @Autowired
    private ExchangeRateService exchangeRateService;

    // KHAI BÁO TƯỜNG MINH TẤT CẢ CÁC SOURCE TỪ THAM SỐ 'job'
    @Mapping(source = "job.id", target = "id")
    @Mapping(source = "job.name", target = "name")
    @Mapping(source = "job.location", target = "location")
    @Mapping(source = "job.address", target = "address")
    @Mapping(source = "job.quantity", target = "quantity")
    @Mapping(source = "job.level", target = "level")
    @Mapping(source = "job.startDate", target = "startDate")
    @Mapping(source = "job.endDate", target = "endDate")
    @Mapping(source = "job.active", target = "active")
    @Mapping(source = "job.createdAt", target = "createdAt")
    @Mapping(source = "job.updatedAt", target = "updatedAt")
    @Mapping(source = "job.createdBy", target = "createdBy")
    @Mapping(source = "job.updatedBy", target = "updatedBy")

    // Mapping cho các object lồng nhau
    @Mapping(source = "job.company.id", target = "company.id")
    @Mapping(source = "job.company.name", target = "company.name")
    @Mapping(source = "job.company.logo", target = "company.logo")
    @Mapping(source = "job.skills", target = "skills")

    // Bỏ qua salary để xử lý thủ công
    @Mapping(target = "salary", ignore = true)
    public abstract ResJobDTO toDto(Job job, String language);

    public List<ResJobDTO> toDto(List<Job> jobs, String language) {
        if (jobs == null) {
            return null;
        }
        return jobs.stream()
                .map(job -> toDto(job, language))
                .collect(Collectors.toList());
    }

    @AfterMapping
    protected void convertSalary(Job job, @MappingTarget ResJobDTO dto, String language) {
        ResJobDTO.SalaryInfo salaryInfo = new ResJobDTO.SalaryInfo();
        if (language != null && language.startsWith("en")) {
            salaryInfo.setValue(exchangeRateService.convert(job.getSalary(), "VND", "USD"));
            salaryInfo.setCurrency("USD");
        } else {
            salaryInfo.setValue(job.getSalary());
            salaryInfo.setCurrency("VND");
        }
        dto.setSalary(salaryInfo);
    }
}