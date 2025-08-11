package vn.hoidanit.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.entity.Skill;
import vn.hoidanit.jobhunter.domain.entity.Subscriber;
import vn.hoidanit.jobhunter.domain.response.email.ResEmailJob;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.SkillRepository;
import vn.hoidanit.jobhunter.repository.SubscriberRepository;
import vn.hoidanit.jobhunter.kafka.MailCommand;
import vn.hoidanit.jobhunter.kafka.MailProducer;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final EmailService emailService;
    private final MailProducer mailProducer;

    public SubscriberService(
            SubscriberRepository subscriberRepository,
            SkillRepository skillRepository,
            JobRepository jobRepository,
            EmailService emailService, MailProducer mailProducer) {
        this.subscriberRepository = subscriberRepository;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.emailService = emailService;
        this.mailProducer = mailProducer;
    }

    // @Scheduled(cron = "*/10 * * * * *")
    // public void testCron() {
    // System.out.println(">>> TEST CRON");
    // }

    public boolean isExistsByEmail(String email) {
        return this.subscriberRepository.existsByEmail(email);
    }

    public Subscriber create(Subscriber subs) {
        // check skills
        if (subs.getSkills() != null) {
            List<Long> reqSkills = subs.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subs.setSkills(dbSkills);
        }

        return this.subscriberRepository.save(subs);
    }

    public Subscriber update(Subscriber subsDB, Subscriber subsRequest) {
        // check skills
        if (subsRequest.getSkills() != null) {
            List<Long> reqSkills = subsRequest.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subsDB.setSkills(dbSkills);
        }
        return this.subscriberRepository.save(subsDB);
    }

    public Subscriber findById(long id) {
        Optional<Subscriber> subsOptional = this.subscriberRepository.findById(id);
        if (subsOptional.isPresent())
            return subsOptional.get();
        return null;
    }

    public ResEmailJob convertJobToSendEmail(Job job) {
        ResEmailJob res = new ResEmailJob();
        res.setName(job.getName());
        res.setSalary(job.getSalary());
        res.setCompany(new ResEmailJob.CompanyEmail(job.getCompany().getName()));
        List<Skill> skills = job.getSkills();
        List<ResEmailJob.SkillEmail> s = skills.stream().map(skill -> new ResEmailJob.SkillEmail(skill.getName()))
                .collect(Collectors.toList());
        res.setSkills(s);
        return res;
    }

    public void sendSubscribersEmailJobs() {
        List<Subscriber> listSubs = this.subscriberRepository.findAll();
        if (listSubs == null || listSubs.isEmpty())
            return;

        for (Subscriber sub : listSubs) {
            List<Skill> listSkills = sub.getSkills();
            if (listSkills == null || listSkills.isEmpty())
                continue;

            List<Job> listJobs = this.jobRepository.findBySkillsIn(listSkills);
            if (listJobs == null || listJobs.isEmpty())
                continue;

            // Lấy jobId thay vì ResEmailJob chi tiết
            List<Long> jobIds = listJobs.stream().map(Job::getId).toList();

            // (khuyến nghị) cắt bớt nếu quá nhiều, ví dụ tối đa 50 job/lần gửi
            int limit = Math.min(jobIds.size(), 50);
            List<Long> jobIdsLimited = jobIds.subList(0, limit);

            MailCommand cmd = new MailCommand(
                    sub.getEmail(),
                    "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                    "job",
                    sub.getName(),
                    jobIdsLimited);

            // enqueue qua producer
            mailProducer.queue(cmd);
        }
    }

    public Subscriber findByEmail(String email) {
        return this.subscriberRepository.findByEmail(email);
    }
}
