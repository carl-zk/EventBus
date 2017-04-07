import org.junit.Test;

import java.util.LinkedList;

/**
 * Created by hero on 17-4-7.
 */
public class ListRemoveTest {

    @Test
    public void test() {
        User u1 = new User("a");
        User u2 = new User("a");
        LinkedList<User> users = new LinkedList<>();
        users.addLast(u1);
        users.addLast(u2);

        for (User u = users.getLast(); u != null; ) {
            if (u.equals(u1))
                users.remove(u);
            System.out.println(u);
            u = users.isEmpty() ? null : users.getLast();
        }
        System.out.println(users.size());
    }

    class User {
        public String name;

        public User(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof User)) return false;

            User user = (User) o;

            return name != null ? name.equals(user.name) : user.name == null;

        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}
