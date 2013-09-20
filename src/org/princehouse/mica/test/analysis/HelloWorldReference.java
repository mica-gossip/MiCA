package org.princehouse.mica.test.analysis;

public class HelloWorldReference {

    public HelloWorldReference() {
        super();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        System.out.println("howdy");

        boolean c = (args.length > 1);

        if (c) {
            System.out.println("foo");
        } else {
            System.out.println("bar");
        }
    }

}
