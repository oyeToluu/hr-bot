package com.hrbotproject.hr_bot.data;

import com.hrbotproject.hr_bot.model.Company;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.JobApplication;
import com.hrbotproject.hr_bot.model.JobPosting;
import com.hrbotproject.hr_bot.model.LeaveRequest;
import com.hrbotproject.hr_bot.model.OnboardingFaq;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.repository.CompanyRepository;
import com.hrbotproject.hr_bot.service.CompanyService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HR Bots used to serve a single company. This runs on every start-up (after
 * DemoDataSeeder) and puts any record that has no company yet into the demo
 * company "Zenith Foods Ltd". Once everything has a company it does nothing.
 */
@Component
@Order(2)
public class CompanyMigration implements CommandLineRunner {

    static final String DEMO_COMPANY = "Zenith Foods Ltd";
    static final String DEMO_INDUSTRY = "Food & Beverage";

    private static final List<Class<?>> COMPANY_OWNED = List.of(
            Employee.class, LeaveRequest.class, PolicyDocument.class,
            OnboardingFaq.class, JobPosting.class, JobApplication.class);

    private final MongoTemplate mongo;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;

    public CompanyMigration(MongoTemplate mongo, CompanyRepository companyRepository, CompanyService companyService) {
        this.mongo = mongo;
        this.companyRepository = companyRepository;
        this.companyService = companyService;
    }

    @Override
    public void run(String... args) {
        if (COMPANY_OWNED.stream().noneMatch(type -> mongo.exists(withoutCompany(), type))) {
            return; // nothing to move
        }

        Company demo = companyRepository.findBySlug(CompanyService.slugify(DEMO_COMPANY))
                .orElseGet(() -> companyService.create(DEMO_COMPANY, DEMO_INDUSTRY));

        long moved = 0;
        for (Class<?> type : COMPANY_OWNED) {
            moved += mongo.updateMulti(withoutCompany(), Update.update("companyId", demo.getId()), type)
                    .getModifiedCount();
        }
        // Jobs show the company name on the public job board
        mongo.updateMulti(Query.query(Criteria.where("companyId").is(demo.getId()).and("companyName").exists(false)),
                Update.update("companyName", demo.getName()), JobPosting.class);

        System.out.println("[migration] put " + moved + " existing record(s) into " + demo.getName());
    }

    private static Query withoutCompany() {
        return Query.query(new Criteria().orOperator(
                Criteria.where("companyId").exists(false),
                Criteria.where("companyId").is(null)));
    }
}