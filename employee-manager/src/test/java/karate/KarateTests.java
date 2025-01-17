package karate;

import com.intuit.karate.junit5.Karate;

class KarateTests {
    
    @Karate.Test
    Karate testEmployees() {
        return Karate.run("employees").relativeTo(getClass());
    }
}