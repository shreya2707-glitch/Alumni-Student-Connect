# Member 1 Contract - what everyone else can rely on

Member 1 provides the foundation. **Do not change these files without telling Member 1.**
Share this folder (or a Git repo) and add your own files next to them.

## 1. Who is logged in?
```java
User u = Session.current();            // null if nobody logged in
Session.hasRole(Role.ALUMNI);
u.getId();                             // use this as the foreign key in your tables
if (u instanceof Student) { Student s = (Student) u; s.getSkills(); s.getTargetIndustry(); }
if (u instanceof Alumni)  { Alumni a  = (Alumni) u;  a.getCompany(); a.getJobRole(); }
```
Hierarchy: `User` (abstract) -> `Student`, `Alumni`, `Admin`. `Role` enum: STUDENT, ALUMNI, ADMIN.

## 2. Data access (never write your own user SQL)
| Need | Call |
|---|---|
| any user by id / username | `AppContext.users.findById(id)` / `findByUsername(name)` |
| all users (Member 5 admin/analytics) | `AppContext.users.findAllUsers()` |
| all alumni (Members 2, 3, 4) | `AppContext.alumni.findAllAlumni()` |
| alumni by industry / id | `findAlumniByIndustry(x)` / `findAlumniById(id)` |
| save profile changes | `AppContext.users.updateProfile(user)` |

For your own tables use `DBConnection.get()` (JDBC `Connection`) with `PreparedStatement`.

## 3. Database rules (see `schema.sql`)
* Table `users(id, username, user_role, full_name, email, bio)` is the single source of identity.
* Every table that refers to a person has a column like `student_id INT` / `alumni_id INT` / `user_id INT`
  with `FOREIGN KEY ... REFERENCES users(id)`.
* Add your tables at the bottom of `schema.sql` in your own section. Do not alter M1 tables;
  ask Member 1 for new columns.

## 4. Plugging in your screens
```java
public class JobBoardModule implements FeatureModule {
    public String getTitle() { return "Jobs"; }
    public boolean isVisibleFor(Role r) { return r != Role.ADMIN; }
    public JComponent createPanel(User current) { return new JobBoardPanel(current); }
}
```
Then add **one line** in `AppContext.init()`: `modules.add(new JobBoardModule());`
Your tab appears automatically after login.

## 5. Matching (Member 2)
Implement `MatchStrategy` and set `AppContext.matcher = new YourMatcher();`.
Discovery, Find My Career Mentor and the network map will use it automatically.

## 6. Demo accounts
student1 / student123 - aarav (any alumni first name) / alumni123 - admin / admin123
