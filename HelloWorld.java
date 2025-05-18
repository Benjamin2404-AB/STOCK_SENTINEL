class Test{
    public static void main(String[] args) {
        int age = 25;
        // System.out.println("Hello, World! {age}");
        System.out.println("Hello, World!  I am " + age + " years old");
        System.out.println(String.format("Hello, World! I am %d years old", age));
        System.out.printf("Hello, World! %d years old\n", age); 
    }
}
