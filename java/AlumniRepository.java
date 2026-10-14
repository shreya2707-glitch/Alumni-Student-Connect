import java.util.List;

/** CONTRACT: read access to alumni (used by discovery, matching, mentorship, referrals). */
public interface AlumniRepository {
    List<Alumni> findAllAlumni();
    List<Alumni> findAlumniByIndustry(String industry);
    Alumni findAlumniById(int userId);
}
