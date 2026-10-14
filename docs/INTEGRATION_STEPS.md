# Integration: only 2 small edits in the team's files (do NOT replace AppContext.java)

## 1. AppContext.init(): add one line after the other modules.add(...) lines
    modules.add(new AdminModule());      // Member 5

## 2. AuthService.java: replace with integration/AuthService.java
The ONLY change is these 2 lines in login(), before Session.start(u):
    if (BlockCheck.isBlocked(u.getId()))
        throw new AuthException("Your account has been blocked by an administrator.");
(BlockCheck.java is in src/ and is safe: if MySQL/M5 tables are missing it simply allows login.)
