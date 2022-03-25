package com.github.sviperll.staticmustache.examples;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MainTest {

    int[][] array = new int[][] {new int[] {1, 2, 3, 4, 5}, new int[] {1, 2, 3, 4, 5},
            new int[] {1, 2, 3, 4, 5}, new int[] {1, 2, 3, 4, 5}, new int[] {1, 2, 3, 4, 5}};
    List<User1.Item<String>> list1 = new ArrayList<User1.Item<String>>();
    {
        list1.add(new User1.Item<String>("abc"));
        list1.add(new User1.Item<String>("def"));
    }

    @Test
    public void testMain() throws IOException {
        Main.main(new String[] {});
    }

    @Test
    public void testUser() throws Exception {

        PrintStream out = requireNonNull(System.out);
        if (out == null)
            throw new IllegalStateException();
        User1 user2 = new User1("Victor", 29, new String[] {"aaa", "bbb", "ccc"}, array, list1);
        
        RenderableHtmlUser1Adapter.of(user2).render(out);
    }

}