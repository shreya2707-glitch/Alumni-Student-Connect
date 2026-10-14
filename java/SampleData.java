import java.util.*;

/** Demo accounts. alumni password: alumni123 | admin/admin123 | student1/student123 */
public class SampleData {
    public static void seed(UserRepository repo) {
        String ah = PasswordUtil.hash("alumni123");
        for (Alumni a : alumni()) repo.save(a, ah);
        repo.save(new Admin(0, "admin", "Site Admin", "admin@alumni.edu"), PasswordUtil.hash("admin123"));
        repo.save(new Student(0, "student1", "Demo Student", "student1@college.edu", "", "",
                "Technology", "Technical Expert"), PasswordUtil.hash("student123"));
    }

    private static Alumni a(String name, String company, String job, String industry, int year,
                            String skills, String interests, String goal, String qual, String journey) {
        String u = name.split(" ")[0].toLowerCase();
        return new Alumni(0, u, name, u + "@alumni.edu", company, job, industry, year,
                          skills, interests, goal, qual, journey);
    }

    static List<Alumni> alumni() {
        List<Alumni> l = new ArrayList<>();
        l.add(a("Aarav Mehta","Google","Software Engineer","Technology",2018,"Java,Python,Cloud,SQL","AI,Startups,Gaming","Technical Expert","B.Tech CS","Intern -> Junior Dev -> SDE at Google"));
        l.add(a("Priya Nair","Microsoft","Data Scientist","Technology",2019,"Python,Machine Learning,Data Analysis,SQL","AI,Research","Researcher","B.Tech CS, MS Data Science","Analyst -> MS abroad -> Data Scientist"));
        l.add(a("Rohan Kulkarni","Infosys","Engineering Manager","Technology",2012,"Java,Leadership,Communication,Cloud","Teaching,Consulting","Team Leader","B.Tech IT, MBA","Developer -> Tech Lead -> Engineering Manager"));
        l.add(a("Sneha Iyer","Goldman Sachs","Quantitative Analyst","Finance",2016,"Python,Finance,Data Analysis,SQL","Finance,Research","Technical Expert","B.Tech, MFE","Campus placement -> Analyst -> Quant"));
        l.add(a("Vikram Singh","Zerodha","Founder - FinTech Startup","Finance",2014,"Finance,Leadership,Marketing,Java","Startups,Finance","Entrepreneur","B.Com, MBA","Banking analyst -> Founded own startup"));
        l.add(a("Ananya Desai","Figma","Product Designer","Design",2020,"UI/UX,Communication,Marketing","Design,Startups","Technical Expert","B.Des","Freelance -> Design intern -> Product Designer"));
        l.add(a("Karan Joshi","Deloitte","Cybersecurity Consultant","Consulting",2017,"Cybersecurity,Cloud,Communication","Security,Consulting","Team Leader","B.Tech, CISSP","SOC analyst -> Security consultant"));
        l.add(a("Meera Rao","Apollo Hospitals","Health Data Analyst","Healthcare",2018,"Data Analysis,SQL,Python","Healthcare,Research","Researcher","B.Sc Stats, M.Sc","Research assistant -> Health analytics lead"));
        l.add(a("Rahul Verma","Amazon","Cloud Architect","Technology",2013,"Cloud,Java,Leadership,Cybersecurity","AI,Security","Team Leader","B.Tech CS","Sysadmin -> DevOps -> Cloud Architect"));
        l.add(a("Neha Kapoor","BYJU'S","Learning Experience Lead","Education",2015,"Communication,Leadership,UI/UX","Teaching,Design","Team Leader","B.Ed, M.A.","Teacher -> Content designer -> Lead"));
        l.add(a("Aditya Rao","McKinsey","Business Analyst","Consulting",2019,"Data Analysis,Communication,Finance","Consulting,Startups","Entrepreneur","B.Tech, MBA","Engineer -> MBA -> Consultant"));
        l.add(a("Isha Patil","Google","ML Engineer","Technology",2021,"Python,Machine Learning,Cloud,Java","AI,Research,Gaming","Technical Expert","B.Tech CS","Research intern -> ML Engineer"));
        return l;
    }
}
